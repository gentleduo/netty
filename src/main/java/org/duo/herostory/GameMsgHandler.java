package org.duo.herostory;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GameMsgHandler extends SimpleChannelInboundHandler<Object> {

    static private Logger LOGGER = LoggerFactory.getLogger(GameMsgHandler.class);

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {

        if (ctx == null || msg == null) {

            return;
        }
        LOGGER.info("收到客户端消息，msg = {}", msg);
    }
}
