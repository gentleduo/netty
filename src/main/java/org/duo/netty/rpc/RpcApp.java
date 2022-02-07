package org.duo.netty.rpc;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.junit.Test;

import java.io.*;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

public class RpcApp {

    @Test
    public void startServer() {

        BwmCar bwm = new BwmCar();
        J20Fly j20 = new J20Fly();

        Dispatcher dispatcher = new Dispatcher();
        dispatcher.register(Car.class.getName(), bwm);
        dispatcher.register(Fly.class.getName(), j20);

        NioEventLoopGroup boss = new NioEventLoopGroup(50);
        NioEventLoopGroup worker = boss;

        ServerBootstrap serverBootstrap = new ServerBootstrap();
        ChannelFuture bind = serverBootstrap.group(boss, worker).channel(NioServerSocketChannel.class).childHandler(new ChannelInitializer<NioSocketChannel>() {
            @Override
            protected void initChannel(NioSocketChannel ch) throws Exception {
                System.out.println("server accept client port : " + ch.remoteAddress().getPort());
                ChannelPipeline p = ch.pipeline();
                p.addLast(new ServerDecode());
                p.addLast(new ServerRequestHandler(dispatcher));
            }
        }).bind(new InetSocketAddress("localhost", 9090));

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
    public void get() throws IOException {

        new Thread(() -> {
            startServer();
        }).start();

        System.out.println("server started......");

//        Car car = proxyGet(Car.class); //动态代理实现
//        car.drive("hello");

        AtomicInteger num = new AtomicInteger(0);
        for (int i = 0; i < 20; i++) {

            new Thread(() -> {
                Car car = proxyGet(Car.class); //动态代理实现
                String arg = "hello" + num.incrementAndGet();
                String res = car.drive(arg);
                System.out.println("client over msg: " + res + " src arg " + arg);
            }).start();
        }

        System.in.read();
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
                Content body = new Content();
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
                Header header = CreateHeader(msgBody);
                byteArrayOutputStream.reset();
                objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
                objectOutputStream.writeObject(header);
                byte[] msgHeader = byteArrayOutputStream.toByteArray();
                //3.连接池：取得连接
                ClientFactory factory = ClientFactory.getFactory();
                NioSocketChannel clientChannel = factory.getClient(new InetSocketAddress("localhost", 9090));
                //4.发送 ==> IO
                long id = header.getRequestId();
                CompletableFuture<String> res = new CompletableFuture<>();
                ResponseHandler.addCallBack(id, res);
                ByteBuf byteBuf = PooledByteBufAllocator.DEFAULT.directBuffer(msgHeader.length + msgBody.length);
                byteBuf.writeBytes(msgHeader);
                byteBuf.writeBytes(msgBody);
                ChannelFuture channelFuture = clientChannel.writeAndFlush(byteBuf);
                channelFuture.sync();

                return res.get();
            }
        });
    }

    public static Header CreateHeader(byte[] msg) {

        Header header = new Header();
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

class ServerDecode extends ByteToMessageDecoder {

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf buf, List<Object> out) throws Exception {
        while (buf.readableBytes() >= 97) {
            byte[] bytes = new byte[97];
            //buf.readBytes(bytes);
            buf.getBytes(buf.readerIndex(), bytes);//从哪里读，读多少，但是read index不变(指针不变)
            ByteArrayInputStream in = new ByteArrayInputStream(bytes);
            ObjectInputStream oin = new ObjectInputStream(in);
            Header header = (Header) oin.readObject();
//            System.out.println("server response @ id: " + header.getRequestId());
            // 通信协议
            if (buf.readableBytes() >= 97 + header.getDataLength()) {
                // 处理指针
                buf.readBytes(97);//移动指针到body开始的位置，
                byte[] data = new byte[(int) header.getDataLength()];
                buf.readBytes(data);
                ByteArrayInputStream din = new ByteArrayInputStream(data);
                ObjectInputStream doin = new ObjectInputStream(din);

                if (header.getFlag() == 0x14141414) {
                    Content content = (Content) doin.readObject();
//                System.out.println(content.getInterfaceName());
                    out.add(new Packmsg(header, content));
                } else if (header.getFlag() == 0x14141424) {
                    Content content = (Content) doin.readObject();
//                System.out.println(content.getInterfaceName());
                    out.add(new Packmsg(header, content));
                }
            } else {
                break;
            }
        }
    }
}

class ServerRequestHandler extends ChannelInboundHandlerAdapter {

    Dispatcher dispatcher;

    public ServerRequestHandler(Dispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

        Packmsg buf = (Packmsg) msg;
//        System.out.println("server handler : " + buf.content.getArgs()[0]);

        String ioThreadName = Thread.currentThread().getName();
        // 1.在当前IO线程中处理业务及返回
        // 2.使用netty自己的eventloop来处理业务及返回
        ctx.executor().execute(new Runnable() {
            //        ctx.executor().parent().next().execute(new Runnable() {
            @Override
            public void run() {

                String interfaceNm = buf.content.getInterfaceName();
                String methodNm = buf.content.getMethodName();
                Object c = dispatcher.get(interfaceNm);
                Class<?> clazz = c.getClass();
                Object res = null;
                try {
                    Method m = clazz.getMethod(methodNm, buf.content.parameterTypes);
                    res = m.invoke(c, buf.content.getArgs());
                } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                    e.printStackTrace();
                }
                String exeThreadName = Thread.currentThread().getName();
                Header header = new Header();
                Content content = new Content();
//                System.out.println("io thread : " + ioThreadName + " exec thread : " + exeThreadName + " from args : " + buf.content.getArgs()[0]);
//                content.setRes("io thread : " + ioThreadName + " exec thread : " + exeThreadName + " from args : " + buf.content.getArgs()[0]);
//                System.out.println((String) res);
                content.setRes((String) res);
                byte[] contentByte = SerDerUtil.ser(content);

                header.setRequestId(buf.header.getRequestId());
                header.setFlag(0x14141424);
                header.setDataLength(contentByte.length);
                byte[] headerByte = SerDerUtil.ser(header);

                ByteBuf bytebuf = PooledByteBufAllocator.DEFAULT.directBuffer(headerByte.length + contentByte.length);

                bytebuf.writeBytes(headerByte);
                bytebuf.writeBytes(contentByte);

                ctx.writeAndFlush(bytebuf);
            }
        });

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
                p.addLast(new ServerDecode());
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

    static ConcurrentHashMap<Long, CompletableFuture> mapping = new ConcurrentHashMap<Long, CompletableFuture>();

    public static void addCallBack(long requestID, CompletableFuture cb) {

        mapping.putIfAbsent(requestID, cb);
    }

    public static void runCallBack(Packmsg packmsg) {

        CompletableFuture runnable = mapping.get(packmsg.header.getRequestId());
        runnable.complete(packmsg.getContent().getRes());
        removeCB(packmsg.header.getRequestId());
    }

    private static void removeCB(long requestID) {
        mapping.remove(requestID);
    }

}

class ClientResponse extends ChannelInboundHandlerAdapter {

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

        Packmsg buf = (Packmsg) msg;
        ResponseHandler.runCallBack(buf);
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

class Header implements Serializable {

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

class Content implements Serializable {

    String interfaceName;
    String methodName;
    Class<?>[] parameterTypes;
    Object[] args;
    String res;

    public String getRes() {
        return res;
    }

    public void setRes(String res) {
        this.res = res;
    }

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
    public String drive(String msg);
}

interface Fly {
    public String fly(String msg);

}

class BwmCar implements Car {
    public String drive(String msg) {
        System.out.println("Server,get client arg:" + msg);
        return "server res " + msg;
    }
}

class J20Fly implements Fly {
    public String fly(String msg) {
        System.out.println("Server,get client arg:" + msg);
        return "server res " + msg;
    }
}

class Dispatcher {

    public static ConcurrentHashMap<String, Object> invokeMapping = new ConcurrentHashMap<>();

    public void register(String k, Object obj) {
        invokeMapping.put(k, obj);
    }

    public Object get(String k) {
        return invokeMapping.get(k);
    }
}
