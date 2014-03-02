package com.cm4j.test.guava.consist.generator;

import com.cm4j.test.guava.consist.entity.TmpListMultikey;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.Template;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 代码生成器 CC
 * <p/>
 * 使用freemarker
 * <p/>
 * Created by yanghao on 14-2-26.
 */
public class CCGenerator {

    public static String source = "E:/workspace/cm4j-all/cm4j-test/src/test/java/com/cm4j/test/guava/consist/generator/ftl";
    // 生成目录
    public static String generatePath = "E:/workspace/cm4j-all/cm4j-test/src/main/java/";
    // 生成包路径
    public static String packagePath = "com/cm4j/test/guava/consist/cc";

    public static Configuration configure() throws Exception {
        Configuration cfg = new Configuration();
        cfg.setDirectoryForTemplateLoading(new File(source));
        cfg.setObjectWrapper(new DefaultObjectWrapper());
        return cfg;
    }

    public static void generateMap(AbsCCDataWarpper wrpper) throws Exception {
        Template template = configure().getTemplate(wrpper.getFtlName());
        Writer out = new OutputStreamWriter(System.out);
//        Writer out = new FileWriter(generatePath + packagePath + "/" + fileName + "2.java");

        template.process(wrpper.dataModel(), out);
        out.flush();
    }

    public static void main(String[] args) throws Exception {
        generateMap(new ListCCDataWarpper(TmpListMultikey.class));
    }
}
