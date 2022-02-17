package org.duo.reflect.step03.entity;

import javassist.*;

import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

public class EntityHelperFactory {

    private static final Map<Class<?>, AbstractEntityHelper> _entityHelperMap = new HashMap<>();

    private EntityHelperFactory() {
    }

    public static AbstractEntityHelper getEntityHelper(Class<?> entityClass) throws Exception {
        if (null == entityClass) {
            return null;
        }

        AbstractEntityHelper helperObj = _entityHelperMap.get(entityClass);
        if (helperObj != null) {
            return helperObj;
        }

        ClassPool pool = ClassPool.getDefault();
        pool.appendSystemPath();

        pool.importPackage(ResultSet.class.getName());
        pool.importPackage(entityClass.getName());

        CtClass abstractEntityHelperClazz = pool.getCtClass(AbstractEntityHelper.class.getName());
        final String helperImplClazzName = entityClass.getName() + "_Helper";

        CtClass helperClazz = pool.makeClass(helperImplClazzName, abstractEntityHelperClazz);
        CtConstructor constructor = new CtConstructor(new CtClass[0], helperClazz);

        constructor.setBody("{}");
        helperClazz.addConstructor(constructor);

        final StringBuilder sb = new StringBuilder();
        sb.append("public Object create(java.sql.ResultSet rs) throws Exception {\n");
        sb.append(entityClass.getName())
                .append(" obj = new ")
                .append(entityClass.getName())
                .append("();\n");

        Field[] fArr = entityClass.getFields();

        for (Field f : fArr) {
            Column annoColumn = f.getAnnotation(Column.class);
            if (annoColumn == null) {
                continue;
            }
            String colName = annoColumn.name();
            if (f.getType() == Integer.TYPE) {
                sb.append("obj.")
                        .append(f.getName())
                        .append(" = rs.getInt(\"")
                        .append(colName)
                        .append("\");\n");
            } else if (f.getType().equals(String.class)) {
                sb.append("obj.")
                        .append(f.getName())
                        .append(" = rs.getString(\"")
                        .append(colName)
                        .append("\");\n");
            } else {

                // 可以继续扩充内容
            }
        }

        sb.append("return obj;\n");
        sb.append("}");

        CtMethod cm = CtNewMethod.make(sb.toString(), helperClazz);
        helperClazz.addMethod(cm);
        Class<?> javaClazz = helperClazz.toClass();
        helperObj = (AbstractEntityHelper) javaClazz.newInstance();

        // 将构造好的类写入文件
        helperClazz.writeFile("D:\\intellij-workspace\\netty\\logs");
        _entityHelperMap.put(entityClass, helperObj);
        return helperObj;
    }
}