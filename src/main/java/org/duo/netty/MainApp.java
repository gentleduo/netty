package org.duo.netty;

import io.netty.buffer.*;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.CharsetUtil;
import org.junit.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;

public class MainApp {

    public static void main(String[] args) {

//        ByteBuf buf = ByteBufAllocator.DEFAULT.buffer(8, 20);
        ByteBuf buf = PooledByteBufAllocator.DEFAULT.heapBuffer(8, 20);
//        ByteBuf buf = UnpooledByteBufAllocator.DEFAULT.buffer(8,20);

        MainApp.printBufferInfo(buf);
        buf.writeBytes(new byte[]{1, 2, 3, 4});
        MainApp.printBufferInfo(buf);
        buf.writeBytes(new byte[]{1, 2, 3, 4});
        MainApp.printBufferInfo(buf);
        buf.writeBytes(new byte[]{1, 2, 3, 4});
        MainApp.printBufferInfo(buf);
        buf.writeBytes(new byte[]{1, 2, 3, 4});
        MainApp.printBufferInfo(buf);
        buf.writeBytes(new byte[]{1, 2, 3, 4});
        MainApp.printBufferInfo(buf);
    }

    public static void printBufferInfo(ByteBuf buf) {
        System.out.println("buf.isReadable() " + buf.isReadable()); //是否可读
        System.out.println("buf.readerIndex() " + buf.readerIndex());//读位置
        System.out.println("buf.readableBytes() " + buf.readableBytes());//能读多少

        System.out.println("buf.isWritable() " + buf.isWritable());//是否可写
        System.out.println("buf.writerIndex() " + buf.writerIndex());//写位置
        System.out.println("buf.writableBytes() " + buf.writableBytes());//能写多少

        System.out.println("buf.capacity() " + buf.capacity()); //已经分配的空间
        System.out.println("buf.maxCapacity() " + buf.maxCapacity()); //可分配的空间(上限)

        System.out.println("buf.isDirect() " + buf.isDirect()); //true:堆外；false:堆内
        System.out.println("-------------------------------------");
    }

    /**
     * 客户端
     */
    @Test
    public void loopExecutor() throws IOException {

        // group线程池
        NioEventLoopGroup selector = new NioEventLoopGroup(1);
        selector.execute(() -> {
            try {
                for (; ; ) {
                    Thread.sleep(1000);
                    System.out.println("hello world");
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        System.in.read();
    }

    @Test
    public void clientMode() throws IOException, InterruptedException {

        // 相当于多路复用器
        NioEventLoopGroup thread = new NioEventLoopGroup(1);
        // 相当于socket channel
        NioSocketChannel client = new NioSocketChannel();
        //将客户端注册到eventLoop中，也就是将socket channel注册到selector中
        thread.register(client);

        ChannelPipeline pipeline = client.pipeline();
        pipeline.addLast(new InputHandler());

        ChannelFuture connect = client.connect(new InetSocketAddress("192.168.56.112", 9090));
        ChannelFuture sync = connect.sync();
        ByteBuf buf = Unpooled.copiedBuffer("hello world".getBytes());
        ChannelFuture send = client.writeAndFlush(buf);
        send.sync();

        sync.channel().closeFuture().sync();
        System.out.println("client over......");
    }
}

class InputHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        System.out.println("client registered......");
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("client active......");
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf buf = (ByteBuf) msg;
        //从buffer中读取完数据后会移动标志位，此时再通过writeAndFlush往channel里面写的话，由于buffer中的readerIndex已经到了数据的末尾，所以数据写入会失败
//        CharSequence charSequence = buf.readCharSequence(buf.readableBytes(), CharsetUtil.UTF_8);
        // 采用getCharSequence的方式读取的话不会移动index的位置，但是要自己控制读取数据的起始位置
        CharSequence charSequence = buf.getCharSequence(0, buf.readableBytes(), CharsetUtil.UTF_8);
        System.out.println(charSequence);
        ctx.writeAndFlush(buf);
    }
}
