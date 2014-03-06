package com.cm4j.test.jpa.generator;

/**
 * 数据库常量
 *
 * @author yanghao
 * @date 2013-6-6
 */
public interface Consts {

    String DB_NAME = "king_high_db";            // 数据库名称
    String DB_HOST = "192.168.0.14";            // 数据库HOST
    int DB_PORT = 3300;                         // 数据库端口
    String DB_USER = "root";                    // 用户名
    String DB_PASS = "123";                     // 密码

    // 包路径
    String PACK_DIR = "net.bojoy.king.dao.webgame.entity";

    // 生成代码存放目录
    // king_high
    String TARGET_DIR = "E:/entity/";
    //    String TARGET_DIR = "E:\\workspace\\bojoy\\king\\Kinghigh_CODE_Server\\Kinghigh_cn_cn_Server\\branches\\20131024\\src\\java\\net\\bojoy\\king\\dao\\webgame\\entity\\";

    // 表前缀  或 直接指定表名
    String DB_TABLE_PREFIX = "dict_country_city";

}