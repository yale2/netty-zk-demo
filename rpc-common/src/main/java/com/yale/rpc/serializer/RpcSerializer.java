package com.yale.rpc.serializer;

import com.alibaba.fastjson.JSON;

/**
 * @author yale
 */
public class RpcSerializer implements MySerializer {

    private Object object;

    @Override
    public byte[] serialize(Object object) {
        this.object = object;
        return JSON.toJSONBytes(object);
    }

    @Override
    public <T> T deserialize(Class<T> clazz, byte[] bytes) {
        return JSON.parseObject(bytes,clazz);
    }
}
