package org.duo.herostory.cmdhandler;

import com.google.protobuf.GeneratedMessageV3;
import org.duo.herostory.GameMsgRecognizer;
import org.duo.herostory.msg.GameMsgProtocol;
import org.duo.herostory.util.PackageUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class CmdHandlerFactory {

    static private final Logger LOGGER = LoggerFactory.getLogger(CmdHandlerFactory.class);

    static private Map<Class<?>, ICmdHandler<? extends GeneratedMessageV3>> _handlerMap = new HashMap<>();

    private CmdHandlerFactory() {
    }

    static public void init() {

        final String packageName = CmdHandlerFactory.class.getPackage().getName();
        Set<Class<?>> classSet = PackageUtil.listSubClazz(packageName, true, ICmdHandler.class);
        for (Class<?> handlerClazz : classSet) {
            if (handlerClazz == null || 0 != (handlerClazz.getModifiers() & Modifier.ABSTRACT)) {
                continue;
            }
            Method[] declaredMethods = handlerClazz.getDeclaredMethods();
            Class<?> msgClazz = null;
            for (Method currMethod : declaredMethods) {
                if (currMethod == null || !currMethod.getName().equals("handle")) {
                    continue;
                }
                // 获取函数参数类型数组
                Class<?>[] parameterTypes = currMethod.getParameterTypes();
                if (parameterTypes.length < 2
                        || parameterTypes[1] == GeneratedMessageV3.class
                        || !GeneratedMessageV3.class.isAssignableFrom(parameterTypes[1])) {
                    continue;
                }
                msgClazz = parameterTypes[1];
                break;
            }
            if (msgClazz == null) {
                continue;
            }

            try {
                ICmdHandler<?> newHandler = (ICmdHandler) handlerClazz.newInstance();
                _handlerMap.put(msgClazz, newHandler);
                LOGGER.info("{} <===> {}", msgClazz.getName(), handlerClazz.getName());
            } catch (InstantiationException e) {
                LOGGER.error(e.getMessage(), e);
            } catch (IllegalAccessException e) {
                LOGGER.error(e.getMessage(), e);
            }
        }
    }

    static public ICmdHandler<? extends GeneratedMessageV3> create(Class<?> msgClazz) {

        if (msgClazz == null) {
            return null;
        }
        return _handlerMap.get(msgClazz);
    }
}
