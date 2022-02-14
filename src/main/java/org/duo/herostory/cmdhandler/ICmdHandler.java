package org.duo.herostory.cmdhandler;

import com.google.protobuf.GeneratedMessageV3;
import io.netty.channel.ChannelHandlerContext;
import org.duo.herostory.msg.GameMsgProtocol;

public interface ICmdHandler<T extends GeneratedMessageV3> {

    void handle(ChannelHandlerContext ctx, T t);
}
