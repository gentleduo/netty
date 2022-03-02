package org.duo.netty.protocol.http.json;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.util.ArrayList;

public class HttpJsonClientHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {

        System.out.println("连接上服务器...");

        Order order = new Order();
        Customer customer = new Customer();
        ArrayList<String> middleNames = new ArrayList<>();
        middleNames.add("丞相");
        customer.setFirstName("诸葛");
        customer.setLastName("亮");
        customer.setMiddleNames(middleNames);
        order.setCustomer(customer);
        Address billTo = new Address();
        billTo.setCity("上海");
        billTo.setCountry("中国");
        billTo.setState("浦东");
        billTo.setPostCode("021");
        Address shipTo = new Address();
        shipTo.setCity("北京");
        shipTo.setCountry("中国");
        shipTo.setState("海淀");
        shipTo.setPostCode("010");
        order.setBillTo(billTo);
        order.setShipTo(shipTo);

        HttpJsonRequest request = new HttpJsonRequest(null, order);
        ctx.writeAndFlush(request);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        System.out.println(msg.getClass().getName());
        System.out.println("接收到了数据..." + msg);
    }

    /*@Override
    protected void channelRead0(ChannelHandlerContext ctx, HttpJsonResponse msg) throws Exception {
        System.out.println("The client receive response of http header is : "
                + msg.getHttpResponse().headers().names());
        System.out.println("The client receive response of http body is : "
                + msg.getResult());
    }*/

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}