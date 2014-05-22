package com.cm4j.test.guava.consist.generator;

import com.cm4j.test.guava.consist.entity.TmpListMultikey;
import com.google.common.collect.Maps;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.Template;

import java.io.File;
import java.io.OutputStreamWriter;
import java.io.Writer;
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
    public static String packagePath = "com/cm4j/test/guava/consist/caches";

    public static Configuration configure() throws Exception {
        Configuration cfg = new Configuration();
        cfg.setDirectoryForTemplateLoading(new File(source));
        cfg.setObjectWrapper(new DefaultObjectWrapper());
        return cfg;
    }

    public static void generateMap(AbsCCDataWrapper wrapper) throws Exception {
        Template template = configure().getTemplate(wrapper.getFtlName());

        // 输出途径
        Writer out = new OutputStreamWriter(System.out);
//        Writer out = new FileWriter(generatePath + packagePath + "/" + fileName + "2.java");

        template.process(wrapper.dataModel(), out);
        out.flush();
    }

    public static void main(String[] args) throws Exception {
        // 缓存的构造函数的参数
        Map params = Maps.newHashMap();
        params.put("int", "playerId");

        String hql = "from TmpListMultikey where id.NPlayerId = ?";
//        String query = "hibernate.findAll((\"" + hql + "\", NumberUtils.toInt(params[0]))";
        // single
        String query = "hibernate.findById(NumberUtils.toInt(params[0])";

        SingleCCDataWrapper wrapper = new SingleCCDataWrapper(TmpListMultikey.class, params, query);

        // map特有的属性
        wrapper.dataModel().put("entry_key_type", "Integer");
        wrapper.dataModel().put("entry_key_content", "entry.getId().getNType()");

        generateMap(wrapper);
    }
}
