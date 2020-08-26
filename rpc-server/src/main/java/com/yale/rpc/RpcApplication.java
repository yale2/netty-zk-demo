package com.yale.rpc;

import com.yale.rpc.encoder.RpcDecoder;
import com.yale.rpc.encoder.RpcEncoder;
import com.yale.rpc.handler.UserServiceHandler;
import com.yale.rpc.request.RpcRequest;
import com.yale.rpc.serializer.RpcSerializer;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import org.apache.curator.CuratorZookeeperClient;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author yale
 */
@SpringBootApplication
public class RpcApplication {

    private static final int PORT=9999;

    private static final String IP="127.0.0.1";

    public static void main(String[] args) throws InterruptedException {
        SpringApplication.run(RpcApplication.class,args);
        startaServer(IP,PORT);
        //注册服务到zk
        registryServer(IP,PORT);
    }


    private static void startaServer(String ip, int port) throws InterruptedException {
        //创建boss线程池和work线程池
        NioEventLoopGroup bossgroup=new NioEventLoopGroup();
        NioEventLoopGroup workgroup=new NioEventLoopGroup();

        //创建启动类
        ServerBootstrap serverBootstrap=new ServerBootstrap();

        //配置启动引导类
        serverBootstrap.group(bossgroup,workgroup)
                //设置通道为nio
                .channel(NioServerSocketChannel.class)
                //创建监听channel
                .childHandler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel nioSocketChannel) throws Exception {
                        //获取管道对象
                        ChannelPipeline pipeline = nioSocketChannel.pipeline();
                        //给管道对象pipeLine 设置编码
                        pipeline.addLast(new StringEncoder());
                        pipeline.addLast( new RpcDecoder(RpcRequest.class,new RpcSerializer()));
                        //把我们自定义一个ChannelHander添加到通道中
                        pipeline.addLast(new UserServiceHandler());
                    }
                });
        //4.绑定端口
        serverBootstrap.bind(port).sync();
    }

    private static void registryServer(String ip, int port) {
        RetryPolicy retryPolicy=new ExponentialBackoffRetry(1000,3);
        CuratorFramework client= CuratorFrameworkFactory.newClient("139.155.234.189:2181",retryPolicy);
        client.start();
        try {
            client.create().creatingParentsIfNeeded().
                    withMode(CreateMode.EPHEMERAL).
                    forPath("/rpc-test/" + ip + ":" + port);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
