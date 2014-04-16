package com.cm4j.test.generator.jpa;


import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.List;

/**
 * 生成代码入口
 *
 * @author yanghao
 * @date 2013-6-6
 */
public class POJO_Generator {

    /**
     * 数据库常量
     *
     * @author yanghao
     * @date 2013-6-6
     */
    public static interface Consts {

        String DB_NAME = "king_high_db";            // 数据库名称
        String DB_HOST = "192.168.0.14";            // 数据库HOST
        int DB_PORT = 3300;                         // 数据库端口
        String DB_USER = "root";                    // 用户名
        String DB_PASS = "123";                     // 密码

        // 包路径
        String PACK_DIR = "net.bojoy.king.dao.webgame.entity";

        // 生成代码存放目录
        // king_high
        //    String TARGET_DIR = "E:/entity/";
        String TARGET_DIR = "E:\\workspace\\bojoy\\king\\Kinghigh_CODE_Server\\Kinghigh_cn_cn_Server\\branches\\20131024\\src\\java\\net\\bojoy\\king\\dao\\webgame\\entity\\";

        // 表前缀，当DB_TABLE为空时才启用
        String DB_TABLE_PREFIX = "";
        // 表名
        String DB_TABLE = "dict_country_daily_task";

    }

    public static void main(String[] args) throws IOException, TemplateException {

        List<TableMeta> tableList;

        String targetDir = Consts.TARGET_DIR;

        tableList = AnalysisDB.readDB();
        AnalysisDB.readTables(tableList);
        // 输出到文件
        File dir = new File(targetDir);
        if (!dir.isDirectory()) {
            dir.mkdirs();
        }

        Configuration cfg = new Configuration();
        cfg.setDirectoryForTemplateLoading(new File("src/main/java/com/cm4j/test/generator/jpa/tpl"));
        cfg.setObjectWrapper(new DefaultObjectWrapper());

        Writer idOut = null;
        Writer clsOut = null;
        if (tableList != null) {
            Template idTpl = cfg.getTemplate("id.ftl");
            Template clsTpl = cfg.getTemplate("cls.ftl");
            for (TableMeta tm : tableList) {
                if (StringUtils.isBlank(tm.getClassName())) {
                    continue;
                }
                // 复合主键Id类生成
                if (tm.getPrimaryKeySize() > 1) {
                    idOut = new FileWriter(new File(targetDir + tm.getClassName() + "Id.java"));
                    idTpl.process(tm, idOut);
                    System.out.println("===文件 " + tm.getClassName() + "Id.java" + " 生成成功===");
                }

                clsOut = new FileWriter(new File(targetDir + tm.getClassName() + ".java"));
                clsTpl.process(tm, clsOut);
                System.out.println("===文件 " + tm.getClassName() + ".java" + " 生成成功===");
            }
        }

        if (idOut != null) {
            idOut.flush();
            idOut.close();
        }

        if (clsOut != null) {
            clsOut.flush();
            clsOut.close();
        }

    }
}