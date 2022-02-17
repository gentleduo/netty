package org.duo.reflect.step02.entity;

import java.lang.reflect.Field;
import java.sql.ResultSet;

public class EntityHelper {

    /**
     * 通过在函数的参数中引入泛型，提高泛化能力
     * 调用时只需要传入具体entity类的class对象即可，该方法不需要再修改
     */
    public <T> T create(Class<T> entityClazz, ResultSet rs) throws Exception {

        if (rs == null) {
            return null;
        }

        Object entity = entityClazz.newInstance();
        Field[] fields = entity.getClass().getFields();

        for (Field field : fields) {
            // 获取字段上的注解
            Column annotation = field.getAnnotation(Column.class);
            if (annotation == null) {
                continue;
            }
            // 获取数据库字段名(注解中的name属性即为数据库中的字段名
            String colName = annotation.name();
            // 从数据库中获取列值
            Object colVal = rs.getObject(colName);

            if (colVal == null) {
                continue;
            }
            field.set(entity, colVal);
        }

        return (T) entity;
    }
}
