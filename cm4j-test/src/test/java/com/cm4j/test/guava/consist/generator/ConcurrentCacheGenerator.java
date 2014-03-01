package com.cm4j.test.guava.consist.generator;

import com.cm4j.test.guava.consist.entity.TmpListMultikey;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.Template;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.FileWriter;
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
public class ConcurrentCacheGenerator {

    private static String source = "E:/workspace/cm4j-all/cm4j-test/src/test/java/com/cm4j/test/guava/consist/generator/ftl";
    // 生成目录
    private static String generatePath = "E:/workspace/cm4j-all/cm4j-test/src/main/java/";
    // 生成包路径
    private static String packagePath = "com/cm4j/test/guava/consist/cc";

    public static Configuration configure() throws Exception {
        Configuration cfg = new Configuration();
        cfg.setDirectoryForTemplateLoading(new File(source));
        cfg.setObjectWrapper(new DefaultObjectWrapper());
        return cfg;
    }

    public static void generateMap(Class pojo, String ftlName) throws Exception {
        String fileName = pojo.getSimpleName() + "MapCache";

        Template template = configure().getTemplate(ftlName);
//        Writer out = new OutputStreamWriter(System.out);
        Writer out = new FileWriter(generatePath + packagePath + "/" + fileName + "2.java");

        Map data = new HashMap();
        data.put("package", StringUtils.replaceChars(packagePath, "/", "."));
        data.put("author", "yanghao");
        data.put("data", new Date());

        data.put("file_name", fileName);

        data.put("pojo", pojo.getSimpleName());
        data.put("map_key", "Integer");

        template.process(data, out);
        out.flush();
    }

    public static void main(String[] args) throws Exception {
        generateMap(TmpListMultikey.class, "map.ftl");
    }
}
