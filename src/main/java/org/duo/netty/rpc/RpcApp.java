package org.duo.netty.rpc;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.junit.Test;

import java.io.*;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.InetSocketAddress;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

public class RpcApp {

    @Test
    public void startServer() {

        NioEventLoopGroup boss = new NioEventLoopGroup(1);
        NioEventLoopGroup worker = boss;

        ServerBootstrap serverBootstrap = new ServerBootstrap();
        ChannelFuture bind = serverBootstrap.group(boss, worker).channel(NioServerSocketChannel.class).childHandler(new ChannelInitializer<NioSocketChannel>() {
            @Override
            protected void initChannel(NioSocketChannel ch) throws Exception {

                ChannelPipeline p = ch.pipeline();
                p.addLast(new ServerRequestHandler());
            }
        }).bind(new InetSocketAddress("localhost",9090));

        try {
            bind.sync().channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * 模拟consumer端的实现
     */
    @Test
    public void get() {

        new Thread(()->{
            startServer();
        }).start();

        System.out.println("server started......");

        Car car = proxyGet(Car.class); //动态代理实现
        car.drive("hello");
    }

    public static <T> T proxyGet(Class<T> interfaceInfo) {

        ClassLoader classLoader = interfaceInfo.getClassLoader();
        Class<?>[] interfaces = {interfaceInfo};

        return (T) Proxy.newProxyInstance(classLoader, interfaces, new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                //consumer对于provider的调用过程
                //1.调用的服务、方法、参数 ==> 封装成message
                String interfaceName = interfaceInfo.getName(); //接口名称，即：服务
                String methodName = method.getName(); // 方法名称
                Class<?>[] parameterTypes = method.getParameterTypes();
                RequestContent body = new RequestContent();
                body.setInterfaceName(interfaceName);
                body.setMethodName(methodName);
                body.setParameterTypes(parameterTypes);
                body.setArgs(args);
                // 序列化
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
                objectOutputStream.writeObject(body);
                byte[] msgBody = byteArrayOutputStream.toByteArray();

                //2.requestId + message 本地缓存
                // 协议：[header : requestId bodyLength] [body]
                RequestHeader header = CreateHeader(msgBody);
                byteArrayOutputStream.reset();
                objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
                objectOutputStream.writeObject(header);
                byte[] msgHeader = byteArrayOutputStream.toByteArray();
                System.out.println(msgHeader.length);

                //3.连接池：取得连接
                ClientFactory factory = ClientFactory.getFactory();
                NioSocketChannel clientChannel = factory.getClient(new InetSocketAddress("localhost", 9090));
                //4.发送 ==> IO
                CountDownLatch countDownLatch = new CountDownLatch(1);
                long id = header.getRequestId();
                ResponseHandler.addCallBack(id, new Runnable() {
                    @Override
                    public void run() {
                        countDownLatch.countDown();
                    }
                });
                ByteBuf byteBuf = PooledByteBufAllocator.DEFAULT.directBuffer(msgHeader.length + msgBody.length);
                byteBuf.writeBytes(msgHeader);
                byteBuf.writeBytes(msgBody);
                ChannelFuture channelFuture = clientChannel.writeAndFlush(byteBuf);
                channelFuture.sync();

                //5.发送完成后，未来从客户端返回回来了，怎么再将代码从发送数据后接着执行(睡眠/回调，如何让线程停下来，而且还能让它继续)
                countDownLatch.await();

                return null;
            }
        });
    }

    public static RequestHeader CreateHeader(byte[] msg) {

        RequestHeader header = new RequestHeader();
        int size = msg.length;
        int flag = 0x14141414;
        long requestId = Math.abs(UUID.randomUUID().getLeastSignificantBits());
        // 0x14 0001 0100
        header.setFlag(flag);
        header.setDataLength(size);
        header.setRequestId(requestId);
        return header;
    }
}

class ServerRequestHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf buf = (ByteBuf) msg;
        ByteBuf sendBuf = buf.copy();
        if (buf.readableBytes() >= 104) {
            byte[] bytes = new byte[104];
            buf.readBytes(bytes);
            ByteArrayInputStream in = new ByteArrayInputStream(bytes);
            ObjectInputStream oin = new ObjectInputStream(in);
            RequestHeader header = (RequestHeader) oin.readObject();
            System.out.println("server response @ id: " + header.getRequestId());
            if (buf.readableBytes() >= header.getDataLength()) {
                byte[] data = new byte[(int) header.getDataLength()];
                buf.readBytes(data);
                ByteArrayInputStream din = new ByteArrayInputStream(data);
                ObjectInputStream doin = new ObjectInputStream(din);
                RequestContent content = (RequestContent) doin.readObject();
                System.out.println(content.getInterfaceName());
            }
        }
        ChannelFuture channelFuture = ctx.writeAndFlush(sendBuf);
        channelFuture.sync();
    }
}

class ClientFactory {

    int poolsize = 1;
    NioEventLoopGroup clientWorker;

    Random rand = new Random();

    private ClientFactory() {

    }

    private static final ClientFactory factory;

    static {
        factory = new ClientFactory();
    }

    public static ClientFactory getFactory() {
        return factory;
    }

    ConcurrentHashMap<InetSocketAddress, ClientPool> outboxs = new ConcurrentHashMap<>();

    public synchronized NioSocketChannel getClient(InetSocketAddress address) {

        ClientPool clientPool = outboxs.get(address);
        if (clientPool == null) {
            outboxs.putIfAbsent(address, new ClientPool(poolsize));
            clientPool = outboxs.get(address);
        }

        int i = rand.nextInt(poolsize);
        if (clientPool.clients[i] != null && clientPool.clients[i].isActive()) {
            return clientPool.clients[i];
        }

        synchronized (clientPool.locks[i]) {
            return clientPool.clients[i] = create(address);
        }
    }

    private NioSocketChannel create(InetSocketAddress address) {

        clientWorker = new NioEventLoopGroup(1);
        Bootstrap bootstrap = new Bootstrap();
        ChannelFuture connect = bootstrap.group(clientWorker).channel(NioSocketChannel.class).handler(new ChannelInitializer<NioSocketChannel>() {
            @Override
            protected void initChannel(NioSocketChannel ch) throws Exception {
                ChannelPipeline p = ch.pipeline();
                p.addLast(new ClientResponse());
            }
        }).connect(address);

        NioSocketChannel client = null;
        try {
            client = (NioSocketChannel) connect.sync().channel();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return client;
    }
}

class ResponseHandler {

    static ConcurrentHashMap<Long, Runnable> mapping = new ConcurrentHashMap<Long, Runnable>();

    public static void addCallBack(long requestID, Runnable cb) {

        mapping.putIfAbsent(requestID, cb);
    }

    public static void runCallBack(long requestID) {

        Runnable runnable = mapping.get(requestID);
        runnable.run();
        removeCB(requestID);
    }

    private static void removeCB(long requestID) {
        mapping.remove(requestID);
    }

}

class ClientResponse extends ChannelInboundHandlerAdapter {

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf buf = (ByteBuf) msg;
        if (buf.readableBytes() >= 104) {
            byte[] bytes = new byte[104];
            buf.readBytes(bytes);
            ByteArrayInputStream in = new ByteArrayInputStream(bytes);
            ObjectInputStream oin = new ObjectInputStream(in);
            RequestHeader header = (RequestHeader) oin.readObject();
            System.out.println("client response @ id: " + header.getRequestId());
            ResponseHandler.runCallBack(header.getRequestId());
//            if (buf.readableBytes() >= header.getDataLength()) {
//                byte[] data = new byte[(int) header.getDataLength()];
//                buf.readBytes(data);
//                ByteArrayInputStream din = new ByteArrayInputStream(data);
//                ObjectInputStream doin = new ObjectInputStream(din);
//                RequestContent content = (RequestContent) doin.readObject();
//                System.out.println(content.getInterfaceName());
//            }
        }
    }
}

class ClientPool {

    NioSocketChannel[] clients;
    Object[] locks;

    ClientPool(int size) {
        clients = new NioSocketChannel[size];
        locks = new Object[10];
        for (int i = 0; i < size; i++) {
            locks[i] = new Object();
        }
    }
}

class RequestHeader implements Serializable {

    /**
     * 1 flag:协议类型
     * 2 UUID
     * 3 DATA_LEN
     */
    int flag;

    public int getFlag() {
        return flag;
    }

    public void setFlag(int flag) {
        this.flag = flag;
    }

    public long getRequestId() {
        return requestId;
    }

    public void setRequestId(long requestId) {
        this.requestId = requestId;
    }

    public long getDataLength() {
        return dataLength;
    }

    public void setDataLength(long dataLength) {
        this.dataLength = dataLength;
    }

    long requestId;
    long dataLength;

}

class RequestContent implements Serializable {

    String interfaceName;
    String methodName;
    Class<?>[] parameterTypes;
    Object[] args;

    public String getInterfaceName() {
        return interfaceName;
    }

    public void setInterfaceName(String interfaceName) {
        this.interfaceName = interfaceName;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public Class<?>[] getParameterTypes() {
        return parameterTypes;
    }

    public void setParameterTypes(Class<?>[] parameterTypes) {
        this.parameterTypes = parameterTypes;
    }

    public Object[] getArgs() {
        return args;
    }

    public void setArgs(Object[] args) {
        this.args = args;
    }
}

interface Car {
    public void drive(String msg);
}

interface Fly {
    public void fly(String msg);

}

