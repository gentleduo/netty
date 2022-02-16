package org.duo.herostory.cmdhandler;

import com.google.protobuf.GeneratedMessageV3;
import org.duo.herostory.GameMsgRecognizer;
import org.duo.herostory.msg.GameMsgProtocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public final class CmdHandlerFactory {

    static private final Logger LOGGER = LoggerFactory.getLogger(CmdHandlerFactory.class);

    static private Map<Class<?>, ICmdHandler<? extends GeneratedMessageV3>> _handlerMap = new HashMap<>();

    private CmdHandlerFactory() {
    }

    static public void init() {

        _handlerMap.put(GameMsgProtocol.UserEntryCmd.class, new UserEntryCmdHandler());
        _handlerMap.put(GameMsgProtocol.WhoElseIsHereCmd.class, new WhoElseIsHereCmdHandler());
        _handlerMap.put(GameMsgProtocol.UserMoveToCmd.class, new UserMoveToCmdHandler());
    }

    static public ICmdHandler<? extends GeneratedMessageV3> create(Class<?> msgClazz) {

        if (msgClazz == null) {
            return null;
        }
        return _handlerMap.get(msgClazz);
    }
}
