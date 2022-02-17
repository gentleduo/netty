package org.duo.reflect.step03;

import org.duo.reflect.step03.entity.AbstractEntityHelper;
import org.duo.reflect.step03.entity.EntityHelperFactory;
import org.duo.reflect.step03.entity.UserEntity;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class App {

    public static void main(String[] args) throws Exception {

        Connection connection = null;
        Statement statement = null;
        ResultSet resultSet = null;
        try {
            Class.forName("com.mysql.cj.jdbc.Driver").newInstance();
            String dbConnStr = "jdbc:mysql://192.168.56.110:3306/test_db?useUnicode=true&characterEncoding=utf-8&useServerPrepStmts=true&serverTimezone=Asia/Shanghai";
            connection = DriverManager.getConnection(dbConnStr, "root", "123456");
            statement = connection.createStatement();
            String sql = "select * from t_user limit 50000";
            resultSet = statement.executeQuery(sql);
            AbstractEntityHelper helper = EntityHelperFactory.getEntityHelper(UserEntity.class);
            long start = System.currentTimeMillis();
            while (resultSet.next()) {
                UserEntity userEntity = (UserEntity) helper.create(resultSet);
            }

            long end = System.currentTimeMillis();
            // 耗时在100ms左右
            System.out.println("实例化花费时间 = " + (end - start) + "ms");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (resultSet != null) {
                resultSet.close();
            }
            if (statement != null) {
                statement.close();
            }
            if (connection != null) {
                connection.close();
            }
        }
    }
}
