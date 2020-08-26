package com.yale.rpc.encoder;

import com.yale.rpc.serializer.MySerializer;
import com.yale.rpc.serializer.RpcSerializer;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public class RpcEncoder extends MessageToByteEncoder {

    private Class<?> clazz;

    private MySerializer serializer;

    public RpcEncoder(Class<?> clazz, RpcSerializer serializer){
        this.clazz=clazz;
        this.serializer=serializer;
    }

    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, Object o, ByteBuf byteBuf) throws Exception {
        if(clazz != null && clazz.isInstance(o)){
            byte[] bytes = serializer.serialize(o);
            //将序列化对象写入通道
            byteBuf.writeInt(bytes.length);
            byteBuf.writeBytes(bytes);
        }
    }
}
