package com.cm4j.test.yimy;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;

/**
 * Created by yanghao on 14-5-29.
 */
public enum Product {

    YICANMI("一餐米[尝鲜装]", 300, 9.8, 3),;

    private String name;
    /**
     * 重量，单位g
     */
    private int weight;
    /**
     * 单价，单位 元/500g
     */
    private double price;
    /**
     * 起售数量
     */
    private int saleNum;

    Product(String name, int weight, double price, int saleNum) {
        this.name = name;
        this.weight = weight;
        this.price = price;
        this.saleNum = saleNum;
    }

    @Override
    public String toString() {
        return Joiner.on(",").withKeyValueSeparator(":").join(ImmutableMap.of("品种", this.name, "重量(g)", this.weight,
                "单价(元/500g)", this.price, "起售数量", this.saleNum));
    }

    public String getName() {
        return name;
    }

    public int getWeight() {
        return weight;
    }

    public double getPrice() {
        return price;
    }

    public int getSaleNum() {
        return saleNum;
    }
}
