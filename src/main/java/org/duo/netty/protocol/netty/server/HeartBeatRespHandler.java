package org.duo.netty.protocol.netty.server;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.duo.netty.protocol.netty.MessageType;
import org.duo.netty.protocol.netty.struct.Header;
import org.duo.netty.protocol.netty.struct.NettyMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HeartBeatRespHandler extends ChannelInboundHandlerAdapter {

    static private final Logger LOG = LoggerFactory.getLogger(HeartBeatRespHandler.class);

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg)
            throws Exception {
        NettyMessage message = (NettyMessage) msg;
        // 返回心跳应答消息
        if (message.getHeader() != null
                && message.getHeader().getType() == MessageType.HEARTBEAT_REQ
                .value()) {
            LOG.info("Receive client heart beat message : ---> "
                    + message);
            NettyMessage heartBeat = buildHeatBeat();
            LOG.info("Send heart beat response message to client : ---> "
                    + heartBeat);
            ctx.writeAndFlush(heartBeat);
        } else
            ctx.fireChannelRead(msg);
    }

    private NettyMessage buildHeatBeat() {
        NettyMessage message = new NettyMessage();
        Header header = new Header();
        header.setType(MessageType.HEARTBEAT_RESP.value());
        message.setHeader(header);
        return message;
    }

}
