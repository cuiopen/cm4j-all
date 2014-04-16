package com.cm4j.test.generator.dict;

import com.cm4j.test.generator.dict.entity.DictCountryDailyTask;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.Template;

import java.io.File;
import java.io.FileWriter;
import java.io.Writer;

/**
 * 基础数据Cache生成
 * <p/>
 * Created by yanghao on 14-3-21.
 */
public class Dict_Generator {

    private static String TARGET_DIR = "E:\\workspace\\bojoy\\king\\Kinghigh_CODE_Server\\Kinghigh_cn_cn_Server\\branches\\20131024\\src\\java\\net\\bojoy\\king\\game\\dict\\";

    // 待生成的entity
    private static final Class entity = DictCountryDailyTask.class;
    private static final String commnet = "国战每日任务缓存";

    public static void main(String[] args) throws Exception {
        File dir = new File(TARGET_DIR);
        if (!dir.isDirectory()) {
            dir.mkdirs();
        }

        Configuration cfg = new Configuration();
        cfg.setDirectoryForTemplateLoading(new File("src/main/java/com/cm4j/test/generator/dict/tpl/"));
        cfg.setObjectWrapper(new DefaultObjectWrapper());

        Template idTpl = cfg.getTemplate("dict.ftl");

        AnalysisEntity analysis = AnalysisEntity.analysis(entity);
        analysis.setComment(commnet);

        Writer idOut = new FileWriter(new File(TARGET_DIR + analysis.getClsName() + ".java"));
        idTpl.process(analysis, idOut);
        System.out.println("===文件 " + analysis.getClsName() + ".java" + " 生成成功===");

        idOut.flush();
        idOut.close();
    }
}
