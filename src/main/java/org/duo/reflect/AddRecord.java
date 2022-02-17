package org.duo.reflect;

import java.sql.*;

public class AddRecord {

    public static void main(String[] args) throws Exception {

        Connection connection = null;
        PreparedStatement stmt = null;

        try {
            Class.forName("com.mysql.cj.jdbc.Driver").newInstance();
            String dbConnStr = "jdbc:mysql://192.168.56.110:3306/test_db?useUnicode=true&characterEncoding=utf-8&useServerPrepStmts=true&serverTimezone=Asia/Shanghai";
            connection = DriverManager.getConnection(dbConnStr, "root", "123456");
            String sql = "insert into t_user(user_id,user_name,password) values(?,?,?)";
            stmt = connection.prepareStatement(sql);
            for (int i = 0; i < 220000; i++) {
                stmt.setInt(1, i);
                stmt.setString(2, "user_name" + i);
                stmt.setString(3, "password" + i);
                stmt.executeUpdate();
                System.out.println("插入成功" + i);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            if (stmt != null) {
                stmt.close();
            }
            if (connection != null) {
                connection.close();
            }
        }
    }
}
