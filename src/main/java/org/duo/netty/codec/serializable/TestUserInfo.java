/*
 * Copyright 2013-2018 Lilinfeng.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.duo.netty.codec.serializable;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

public class TestUserInfo {

    /**
     * 基于java提供的对象输入/输出流ObjectInputStream和ObjectOutputStream，
     * 可以直接把Java对象作为可存储的字节数组写入文件，也可以传输到网络上。
     * Java序列化不需要添加额外的类库，只需要实现java.io.Serializable并生成序列ID即可，
     * 但是在在远程服务调用(RPC)时，很少直接使用Java序列化进行消息的编解码和传输，这是因为Java序列化有以下缺点：
     * 1.无法跨语言
     * 2.序列化后的码流太大
     * 通过下面的测试程序可是看到：采用JDK序列化机制编码后的二进制数组大小竟然是基于ByteBuffer的通用二级制编码的5.29倍，
     * 再由PerformTestUserInfo的测试程序可知：采用JDK序列化的性能只有基于二进制数组编码的6.17%。
     * 因此无论是序列化后的码流大小，还是序列化的性能，JDK默认的序列化机制都表现得很差，所以通常不会选择Java序列化作为远程节点调用的编解码框架。
     * <p>
     * 目前业界流行的编解码框架：
     * 1.Google的Protobuf
     * 2.Facebook的Thrift
     * 3.JBoss的Marshalling
     *
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        UserInfo info = new UserInfo();
        info.buildUserID(100).buildUserName("Welcome to Netty");
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream os = new ObjectOutputStream(bos);
        os.writeObject(info);
        os.flush();
        os.close();
        byte[] b = bos.toByteArray();
        System.out.println("The jdk serializable length is : " + b.length);
        bos.close();
        System.out.println("-------------------------------------");
        System.out.println("The byte array serializable length is : "
                + info.codeC().length);

    }

}
