package com.cm4j.test.yimy;

/**
 * Created by yanghao on 14-5-29.
 */
public class YimyMain {


    public static void main(String[] args) {
        Product product = Product.YICANMI_10B;
        Express express = Express.SHUNFEN;

        ComputedResult result = new ComputedResult(product, express);

        System.out.println("产品 -> " + product);
        System.out.println("快递 -> " + express.getName());

        System.out.println("总售价 -> " + result.getSalePrice() + "元");
        System.out.println("");

        System.out.println("成本如下：");
        System.out.println("米价 -> " + result.getRicePrice() + "元");
        System.out.println("物流 -> " + result.getExpress() + "元");
        System.out.println("其他 -> " + result.getOtherCost() + "元");
        System.out.println("总计 -> " + result.getTotalCost() + "元");
        System.out.println("");

        System.out.println("利润 -> " + String.format("%.2f", result.getProfit()) + "元");
        System.out.println("利润率 -> " + String.format("%.2f", result.getProfitRate()) + "%");
    }
}
