package org.duo.herostory;

import com.google.protobuf.GeneratedMessageV3;
import com.google.protobuf.Message;
import org.duo.herostory.msg.GameMsgProtocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class GameMsgRecognizer {

    static private final Logger LOGGER = LoggerFactory.getLogger(GameMsgRecognizer.class);

    static private final Map<Integer, GeneratedMessageV3> _msgCodeAndMsgObjMap = new HashMap<>();

    static private final Map<Class<?>, Integer> _clazzAndMsgCodeMap = new HashMap<>();

    private GameMsgRecognizer() {
    }

    static public void init() {

        // 获取GameMsgProtocol的内部类
        Class<?>[] declaredClasses = GameMsgProtocol.class.getDeclaredClasses();

        for (Class<?> clazz : declaredClasses) {
            if (clazz == null || !GeneratedMessageV3.class.isAssignableFrom(clazz)) {
                continue;
            }
            String clazzName = clazz.getSimpleName();
            clazzName = clazzName.toLowerCase();
            for (GameMsgProtocol.MsgCode msgCode : GameMsgProtocol.MsgCode.values()) {
                if (null == msgCode) {
                    continue;
                }
                // 消息编码
                String strMsgCode = msgCode.name();
                strMsgCode = strMsgCode.replaceAll("_", "");
                strMsgCode = strMsgCode.toLowerCase();

                if (!strMsgCode.startsWith(clazzName)) {
                    continue;
                }

                Object getDefaultInstance = null;
                try {
                    getDefaultInstance = clazz.getDeclaredMethod("getDefaultInstance").invoke(clazz);
                } catch (IllegalAccessException e) {
                    LOGGER.error(e.getMessage(), e);
                } catch (InvocationTargetException e) {
                    LOGGER.error(e.getMessage(), e);
                } catch (NoSuchMethodException e) {
                    LOGGER.error(e.getMessage(), e);
                }
                _msgCodeAndMsgObjMap.put(msgCode.getNumber(), (GeneratedMessageV3) getDefaultInstance);
                _clazzAndMsgCodeMap.put(clazz, msgCode.getNumber());
            }
        }
//        _msgCodeAndMsgObjMap.put(GameMsgProtocol.MsgCode.USER_ENTRY_CMD_VALUE, GameMsgProtocol.UserEntryCmd.getDefaultInstance());
//        _msgCodeAndMsgObjMap.put(GameMsgProtocol.MsgCode.WHO_ELSE_IS_HERE_CMD_VALUE, GameMsgProtocol.WhoElseIsHereCmd.getDefaultInstance());
//        _msgCodeAndMsgObjMap.put(GameMsgProtocol.MsgCode.USER_MOVE_TO_CMD_VALUE, GameMsgProtocol.UserMoveToCmd.getDefaultInstance());
//
//        _clazzAndMsgCodeMap.put(GameMsgProtocol.UserEntryResult.class, GameMsgProtocol.MsgCode.USER_ENTRY_RESULT_VALUE);
//        _clazzAndMsgCodeMap.put(GameMsgProtocol.WhoElseIsHereResult.class, GameMsgProtocol.MsgCode.WHO_ELSE_IS_HERE_RESULT_VALUE);
//        _clazzAndMsgCodeMap.put(GameMsgProtocol.UserMoveToResult.class, GameMsgProtocol.MsgCode.USER_MOVE_TO_RESULT_VALUE);
//        _clazzAndMsgCodeMap.put(GameMsgProtocol.UserQuitResult.class, GameMsgProtocol.MsgCode.USER_QUIT_RESULT_VALUE);
    }

    static public Message.Builder getBuilderByMsgCode(int msgCode) {

        if (msgCode < 0) {
            return null;
        }

        GeneratedMessageV3 defaultMsg = _msgCodeAndMsgObjMap.get(msgCode);

        if (defaultMsg == null) {
            return null;
        }
        return defaultMsg.newBuilderForType();
    }

    static public int getMsgCodeByClazz(Class<?> clazz) {

        if (clazz == null) {
            return -1;
        }

        Integer msgCode = _clazzAndMsgCodeMap.get(clazz);

        if (msgCode == null) {
            return -1;
        }

        return msgCode.intValue();
    }
}
