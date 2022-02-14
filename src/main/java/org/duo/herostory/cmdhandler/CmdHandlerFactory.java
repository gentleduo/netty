package org.duo.herostory.cmdhandler;

import com.google.protobuf.GeneratedMessageV3;
import org.duo.herostory.msg.GameMsgProtocol;

public final class CmdHandlerFactory {

    private CmdHandlerFactory() {
    }

    static public ICmdHandler<? extends GeneratedMessageV3> create(Object msg) {

        if (msg instanceof GameMsgProtocol.UserEntryCmd) {
            // 用户入场
            return new UserEntryCmdHandler();
        } else if (msg instanceof GameMsgProtocol.WhoElseIsHereCmd) {
            // 还有谁在场
            return new WhoElseIsHereCmdHandler();
        } else if (msg instanceof GameMsgProtocol.UserMoveToCmd) {
            // 用户移动
            return new UserMoveToCmdHandler();
        } else {
            return null;
        }
    }
}
