package com.yale.rpc.serializer;

/**
 * @author yale
 */
public interface MySerializer {

    byte[] serialize(Object object);

    <T> T deserialize(Class<T> clazz,byte[] bytes);
}
