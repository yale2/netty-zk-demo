package com.yale.rpc.encoder;

import com.alibaba.fastjson.JSONObject;
import com.yale.rpc.serializer.MySerializer;
import com.yale.rpc.serializer.RpcSerializer;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

/**
 * @author yale
 */
public class RpcDecoder extends ByteToMessageDecoder {

    private Class<?> clazz;

    private MySerializer serializer;

    public RpcDecoder(Class<?> clazz, RpcSerializer serializer){
        this.clazz=clazz;
        this.serializer=serializer;
    }

    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf, List<Object> list) throws Exception {
        if(clazz != null){
            int i = byteBuf.readInt();
            byte[] bytes=new byte[i];
            byteBuf.readBytes(bytes);
            Object object = JSONObject.parseObject(bytes, clazz);
            list.add(object);
        }
    }
}
