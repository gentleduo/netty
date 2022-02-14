package org.duo.herostory;

import io.netty.channel.Channel;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;

/**
 * 广播员
 */
public class Broadcaster {

    /**
     * 信道组，注意这里一定要用static
     * 否则服无法实现群发
     */
    static private final ChannelGroup _channelGroup = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    /**
     * 私有化类默认构造器
     */
    private Broadcaster() {
    }

    /**
     * 添加信道
     *
     * @param ch
     */
    static public void addChannel(Channel ch) {

        if (ch != null) {
            _channelGroup.add(ch);
        }
    }

    /**
     * 删除信道
     *
     * @param ch
     */
    static public void removeChannel(Channel ch) {
        if (ch != null) {
            _channelGroup.remove(ch);
        }
    }

    /**
     * 广播消息
     *
     * @param msg
     */
    static public void broadcast(Object msg) {
        if (msg != null) {
            _channelGroup.writeAndFlush(msg);
        }
    }
}
