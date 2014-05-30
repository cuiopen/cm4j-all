package com.cm4j.test.yimy;

/**
 * 快递
 *
 * Created by yanghao on 14-5-29.
 */
public enum Express {
    /**
     * 顺丰
     */
    SHUNFEN("顺丰", 6, 3),
    ;

    private String name;
    /**
     * 首重，单位kg
     */
    private int fkgPrice;
    /**
     * 续重
     */
    private int ykgPrice;

    Express(String name, int fkgPrice, int ykgPrice) {
        this.name = name;
        this.fkgPrice = fkgPrice;
        this.ykgPrice = ykgPrice;
    }

    /**
     * 计算运费
     *
     * @param g 单位克
     * @return
     */
    public double calcExpressPrice(int g) {
        if (g <= 0) {
            return 0;
        }
        double ceil = Math.ceil(((double) g) / 1000);
        if (ceil <= 1) {
            return fkgPrice;
        }
        return fkgPrice + (ceil - 1) * ykgPrice;
    }

    public int getFkgPrice() {
        return fkgPrice;
    }

    public int getYkgPrice() {
        return ykgPrice;
    }

    public String getName() {
        return name;
    }
}
