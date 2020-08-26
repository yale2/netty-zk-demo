package com.yale.rpc.client;

import com.google.common.base.Strings;
import com.yale.rpc.boot.ConsumerBoot;
import com.yale.rpc.encoder.RpcEncoder;
import com.yale.rpc.entity.Server;
import com.yale.rpc.handler.UserClientHandler;
import com.yale.rpc.request.RpcRequest;
import com.yale.rpc.serializer.RpcSerializer;
import com.yale.rpc.service.IUserService;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.Watcher;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 消费者
 */
public class RPCConsumer {

    //1.创建一个线程池对象  -- 它要处理我们自定义事件
    private static ExecutorService executorService =
            Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    //curator客户端
    private static CuratorFramework curatorFramework;

    private static Map<String,Server> serverMap=new HashMap<>();

    private static volatile Map<Bootstrap,Server> bootstrapServerMap=new HashMap<>();


    //2.声明一个自定义事件处理器  UserClientHandler
//    private static UserClientHandler userClientHandler;


    //3.编写方法,初始化客户端  ( 创建连接池  bootStrap  设置bootstrap  连接服务器)
    public static List<Bootstrap> initClient(Map<Bootstrap, UserClientHandler> handlerMap,
                                             Map<Server,EventLoopGroup> serverEventLoopGroupHashMap,
                                             Map<Server, Bootstrap> serverBootstrapMap) throws Exception {
        //从zk获取服务列表
        List<Server> serverList = getServerList();
        if(curatorFramework!=null){
            PathChildrenCache pathChildrenCache=new PathChildrenCache(curatorFramework,"/rpc-test",true);
            pathChildrenCache.getListenable().addListener(new PathChildrenCacheListener() {
                @Override
                public void childEvent(CuratorFramework curatorFramework, PathChildrenCacheEvent pathChildrenCacheEvent) throws Exception {
                    ChildData data = pathChildrenCacheEvent.getData();
                    String[] split = data.getPath().split("\\/");
                    Server server = getServer(split[split.length-1]);
                    switch (pathChildrenCacheEvent.getType()){
                        case CHILD_ADDED:
                            connectToServer(server,handlerMap,serverEventLoopGroupHashMap);
                            ConsumerBoot.sendRequest();
                            break;
                        case CHILD_REMOVED:
                            disConnectServer(server,serverEventLoopGroupHashMap,serverBootstrapMap);
                            break;
                    }
                }
            });
            pathChildrenCache.start(PathChildrenCache.StartMode.BUILD_INITIAL_CACHE);
        }
        List<Bootstrap> bootstrapList=new ArrayList<>(serverList.size());
        //连接所有服务端
        for (Server server : serverList) {
            Bootstrap bootstrap = connectToServer(server,handlerMap,serverEventLoopGroupHashMap);
            serverBootstrapMap.put(server,bootstrap);
            bootstrapServerMap.put(bootstrap,server);
            bootstrapList.add(bootstrap);
        }

        return bootstrapList;
    }

    private static Bootstrap connectToServer(Server server, Map<Bootstrap,UserClientHandler> handlerMap,Map<Server,EventLoopGroup> serverEventLoopGroupMap) throws InterruptedException, ExecutionException {

        //1) 初始化UserClientHandler
        UserClientHandler userClientHandler = new UserClientHandler();
        //2)创建连接池对象
        EventLoopGroup group = new NioEventLoopGroup();
        //3)创建客户端的引导对象
        Bootstrap bootstrap = new Bootstrap();
        //4)配置启动引导对象
        bootstrap.group(group)
                //设置通道为NIO
                .channel(NioSocketChannel.class)
                //设置请求协议为TCP
                .option(ChannelOption.TCP_NODELAY, true)
                //监听channel 并初始化
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel socketChannel) throws Exception {
                        //获取ChannelPipeline
                        ChannelPipeline pipeline = socketChannel.pipeline();
                        //设置编码
                        pipeline.addLast(new StringDecoder());
                        pipeline.addLast(new RpcEncoder(RpcRequest.class, new RpcSerializer()));
                        //添加自定义事件处理器
                        pipeline.addLast(userClientHandler);
                    }
                });
        //5)连接服务端
        bootstrap.connect(server.getIp(), server.getPort()).sync();
        if(ConsumerBoot.bootstrapList!= null && !ConsumerBoot.bootstrapList.contains(bootstrap)){
            ConsumerBoot.bootstrapList.add(bootstrap);
        }
        handlerMap.put(bootstrap,userClientHandler);
        serverEventLoopGroupMap.put(server,group);
        if(!ConsumerBoot.serverBootstrapMap.containsKey(server)){
            ConsumerBoot.serverBootstrapMap.put(server,bootstrap);
        }
        if(!bootstrapServerMap.containsKey(server)){
            bootstrapServerMap.put(bootstrap,server);
        }

        return bootstrap;
    }

    public static void disConnectServer(Server server, Map<Server,EventLoopGroup> serverEventLoopGroupHashMap,
                                        Map<Server, Bootstrap> serverBootstrapMap){
        Bootstrap bootstrap = serverBootstrapMap.get(server);
        EventLoopGroup group = serverEventLoopGroupHashMap.get(server);
        group.shutdownGracefully();
        ConsumerBoot.bootstrapList.remove(bootstrap);
        serverBootstrapMap.remove(server);
        bootstrapServerMap.remove(bootstrap);
        ConsumerBoot.handlerMap.remove(bootstrap);
    }

    /**
     * 从zk获取服务列表
     * @return 所有服务端信息
     */
    private static List<Server> getServerList() throws Exception {
        List<Server> serverList = new ArrayList<>();
        RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000, 3);
        curatorFramework= CuratorFrameworkFactory.newClient("139.155.234.189:2181", retryPolicy);
        curatorFramework.start();
        try {
            List<String> strings = curatorFramework.getChildren().forPath("/rpc-test");
            for (String string : strings) {
                Server server = getServer(string);
                serverList.add(server);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return serverList;
    }

    private static Server getServer(String string) {
        if(serverMap!=null && serverMap.containsKey(string)){
            return serverMap.get(string);
        }
        String[] split = string.split(":");
        Server server = new Server();
        server.setIp(split[0]);
        server.setPort(Integer.valueOf(split[1]));
        serverMap.put(string,server);
        return server;
    }

    //4.编写一个方法,使用JDK的动态代理创建对象
    // serviceClass 接口类型,根据哪个接口生成子类代理对象;   providerParam :  "UserService#sayHello#"
//    public static Object createProxy(Class<?> serviceClass) {
//        return Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),
//                new Class[]{serviceClass}, (o, method, objects) -> {
//
//
//                    //1)初始化客户端cliet
//                    if (userClientHandler == null) {
//                        initClient();
//                    }
//
//
//                });
//    }

    public static Object sendRequestLoadBalance(Class<?> clazz, Bootstrap bootstrap) throws Exception {
        UserClientHandler userClientHandler = ConsumerBoot.handlerMap.get(bootstrap);
        //2)给UserClientHandler 设置rpcRequest参数
        RpcRequest rpcRequest = new RpcRequest();
        rpcRequest.setClassName(clazz.getName());
        rpcRequest.setMethodName("sayHello");
        rpcRequest.setParamTypes(new Class[]{String.class});
        rpcRequest.setParams(new String[]{"this is client"});
        userClientHandler.setRpcRequest(rpcRequest);

        //3).使用线程池,开启一个线程处理处理call() 写操作,并返回结果
        long startTime=System.currentTimeMillis();
        Object result = executorService.submit(userClientHandler).get();
        long endTime=System.currentTimeMillis();
        String value = String.valueOf(endTime) + ":" + String.valueOf(endTime - startTime);
        Server server = bootstrapServerMap.get(bootstrap);
        curatorFramework.setData().forPath("/rpc-test/"+server.getIp()+":"+server.getPort(),value.getBytes());

        //4)return 结果
        return result;
    }

    public static List<Bootstrap> getLoadBalanceServer(List<Bootstrap> bootstrapList, Map<Server, Bootstrap> serverBootstrapMap) throws Exception {
        List<Bootstrap> bootstraps=new ArrayList<>();
        List<String> strings = curatorFramework.getChildren().forPath("/rpc-test");
        int[] responseTimes=new int[strings.size()];
        for (int i=0;i<strings.size();i++) {
            String value = new String(curatorFramework.getData().forPath("/rpc-test/"+strings.get(i)));
            if(Strings.isNullOrEmpty(value) || !value.contains(":")){
                bootstraps.add(serverBootstrapMap.get(getServer(strings.get(i))));
                continue;
            }
            String[] split = value.split(":");
            long now=System.currentTimeMillis();
            if(now-Long.valueOf(split[0]) > 5000){
                //如果上一次请求到现在的时间大于5秒  加入本次请求列表中
                bootstraps.add(serverBootstrapMap.get(getServer(strings.get(i))));
            }
            //否则将响应时间放到数组中，方便查找最小值
            responseTimes[i]=Integer.valueOf(split[1]);
        }
        int min = Arrays.stream(responseTimes).min().getAsInt();
        for (int i = 0; i < responseTimes.length; i++) {
            if(responseTimes[i]==min){
                Bootstrap bootstrap = serverBootstrapMap.get(getServer(strings.get(i)));
                if(!bootstraps.contains(bootstrap)){
                    bootstraps.add(bootstrap);
                }
            }
        }

        return bootstraps;
    }
}
