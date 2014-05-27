package com.cm4j.test.jdk.scanner;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

/**
 * 利用Scanner解析文件内容
 */
public class ScannerUsage {

    private static void readfile(String filename) {
        try {
            Scanner scanner = new Scanner(new File(filename));
            scanner.useDelimiter(System.getProperty("line.separator"));
            while (scanner.hasNext()) {
                parseline(scanner.next());
            }
            scanner.close();
        } catch (FileNotFoundException e) {
            System.out.println(e);
        }
    }

    private static void parseline(String line) {
        Scanner linescanner = new Scanner(line);
        linescanner.useDelimiter(",");

        //可以修改usedelimiter参数以读取不同分隔符分隔的内容
        String name=linescanner.next();
        int age=linescanner.nextInt();
        String idate=linescanner.next();
        boolean iscertified = linescanner.nextBoolean();

        System.out.println("姓名：" + name + "，年龄：" + age + "，入司时间：" + idate + "，验证标记：" + iscertified);
    }

    public static void main(String[] args) {
        readfile("E:\\workspace\\cm4j-all\\cm4j-test\\src\\main\\java\\com\\cm4j\\test\\jdk\\scanner\\hrinfo.txt");
    }
}