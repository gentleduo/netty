package org.duo.netty.codec.msgpack;


import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class EchoClientHandler extends ChannelInboundHandlerAdapter {
    private int count;

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
       /* User user = getUser();
        ctx.writeAndFlush(user);*/
        User[] users = getUsers();
        for (User u : users) {
            System.out.println(u);
            ctx.writeAndFlush(u);
        }

    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        System.out.println("这是客户端接收的消息【  " + ++count + "  】时间:【" + msg + "】");
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.close();
    }

    private User[] getUsers() {
        User[] users = new User[5];
        for (int i = 0; i < 5; i++) {
            User user = new User();
            user.setId(String.valueOf(i));
            user.setAge(18 + i);
            user.setName("迪迦" + i);
            user.setSex("男" + String.valueOf(i * 2));
            users[i] = user;
        }
        return users;
    }

    private User getUser() {
        User user = new User();
        user.setId("11");
        user.setAge(18);
        user.setName("张元");
        user.setSex("男");
        return user;
    }
}