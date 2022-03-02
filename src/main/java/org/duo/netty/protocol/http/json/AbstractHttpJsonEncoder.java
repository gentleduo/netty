package org.duo.netty.protocol.http.json;

import com.alibaba.fastjson.JSONObject;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;

import java.nio.charset.Charset;

/**
 * 使用了FastJson框架进行编码
 *
 * @param <T>
 */
public abstract class AbstractHttpJsonEncoder<T> extends MessageToMessageEncoder<T> {

    final static Charset UTF_8 = Charset.forName("utf-8");

    protected ByteBuf encode0(ChannelHandlerContext ctx, Object body) {
        String jsonStr = JSONObject.toJSONString(body);
        ByteBuf encodeBuf = Unpooled.copiedBuffer(jsonStr, UTF_8);
        return encodeBuf;
    }
}
