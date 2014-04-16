package com.cm4j.test.generator.jpa;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * 创建数据库连接
 *
 * @author yanghao
 * @date 2013-6-6
 */
public class DBHandler {

    private static Connection conn;

    public static final Connection createConnection() {
        try {
            Class.forName("com.mysql.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        try {
            conn = DriverManager.getConnection("jdbc:mysql://" + POJO_Generator.Consts.DB_HOST + ":" + POJO_Generator.Consts.DB_PORT + "/" + POJO_Generator.Consts.DB_NAME, POJO_Generator.Consts.DB_USER, POJO_Generator.Consts.DB_PASS);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return conn;
    }

    public static final void close() {
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

}
