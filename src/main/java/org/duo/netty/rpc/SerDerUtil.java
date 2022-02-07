package org.duo.netty.rpc;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

public class SerDerUtil {

    static ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

    public synchronized static byte[] ser(Object msg) {
        byteArrayOutputStream.reset();
        ObjectOutputStream objectOutputStream = null;
        byte[] msgbody = null;
        try {
            objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
            objectOutputStream.writeObject(msg);
            msgbody = byteArrayOutputStream.toByteArray();
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
        return msgbody;
    }
}
