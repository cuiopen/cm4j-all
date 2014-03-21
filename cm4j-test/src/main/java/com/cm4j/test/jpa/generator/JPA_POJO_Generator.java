package com.cm4j.test.jpa.generator;


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
public class JPA_POJO_Generator {

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
        cfg.setDirectoryForTemplateLoading(new File("src/main/java/com/cm4j/test/jpa/generator/tpl"));
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