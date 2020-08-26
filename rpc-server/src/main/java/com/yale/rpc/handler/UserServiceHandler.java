package com.yale.rpc.handler;

import com.yale.rpc.context.MyApplicationContext;
import com.yale.rpc.request.RpcRequest;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.Map;

/**
 * 自定义的业务处理器
 */
public class UserServiceHandler extends ChannelInboundHandlerAdapter {

    //当客户端读取数据时,该方法会被调用
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

        //注意:  客户端将来发送请求的时候会传递一个参数:  UserService#sayHello#are you ok
         //1.判断当前的请求是否符合规则
//        if(msg.toString().startsWith("UserService")){
//            //2.如果符合规则,调用实现类货到一个result
//            UserServiceImpl service = new UserServiceImpl();
//            String result = service.sayHello(msg.toString().substring(msg.toString().lastIndexOf("#")+1));
//            //3.把调用实现类的方法获得的结果写到客户端
//            ctx.writeAndFlush(result);
//        }
        RpcRequest rpcRequest = (RpcRequest) msg;
        Map<String,?> beanMap = MyApplicationContext.getBeanOfType(Class.forName(rpcRequest.getClassName()));
        for (Map.Entry<String, ?> stringEntry : beanMap.entrySet()) {
            Object bean = stringEntry.getValue();
            Method method = bean.getClass().getDeclaredMethod(rpcRequest.getMethodName(), rpcRequest.getParamTypes());
            Object result = method.invoke(bean, rpcRequest.getParams());
            ctx.writeAndFlush(result);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        super.exceptionCaught(ctx, cause);
    }
}
