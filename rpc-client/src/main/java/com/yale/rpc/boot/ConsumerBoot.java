package com.yale.rpc.boot;

import com.yale.rpc.client.RPCConsumer;
import com.yale.rpc.entity.Server;
import com.yale.rpc.handler.UserClientHandler;
import com.yale.rpc.service.IUserService;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.EventLoopGroup;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

public class ConsumerBoot {

    public static volatile List<Bootstrap> bootstrapList;

    public static volatile Map<Server, EventLoopGroup> serverEventLoopGroupHashMap =new ConcurrentHashMap<>();

    public static volatile Map<Server, Bootstrap> serverBootstrapMap=new ConcurrentHashMap<>();

    public static volatile Map<Bootstrap, UserClientHandler> handlerMap=new ConcurrentHashMap<>();



    public static void main(String[] args) throws Exception {

        bootstrapList = RPCConsumer.initClient(handlerMap, serverEventLoopGroupHashMap,serverBootstrapMap);

        //1.创建代理对象
//        IUserService service = (IUserService) RPCConsumer.createProxy(IUserService.class);
        sendRequest();


    }

    public static void sendRequest() throws Exception {
        //2.循环给服务器写数据
        int i=0;
        while (i<10){
            List<Bootstrap> bootstraps=RPCConsumer.getLoadBalanceServer(bootstrapList,serverBootstrapMap);
            Iterator<Bootstrap> iterator = bootstraps.iterator();
            while(iterator.hasNext()){
                String result = RPCConsumer.sendRequestLoadBalance(IUserService.class,iterator.next()).toString();
                System.out.println(result);
                Thread.sleep(2000);
            }
            i++;
        }
    }
}
