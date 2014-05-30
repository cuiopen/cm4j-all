package com.cm4j.test.yimy;

/**
 * 计算结果封装
 *
 * Created by yanghao on 14-5-29.
 */
public class ComputedResult {

    /**
     * 总售价
     */
    private final double salePrice;

    /*
     * ================成本==================
     */

    /**
     * 总米价
     */
    private final double ricePrice;

    /**
     * 物流费
     */
    private final double express;

    /**
     * 其他成本，如包装什么的
     */
    private final double otherCost = YimyConsts.OTHER_COST;

    /**
     * 总成本
     */
    private final double totalCost;

    /**
     * 总利润
     */
    private final double profit;

    /**
     * 利润率，单位为%
     */
    private final double profitRate;

    /**
     * 计算数值
     *
     * @param product
     * @param express
     */
    public ComputedResult(Product product, Express express) {
        this.salePrice = (product.getPrice() * product.getWeight() * product.getSaleNum() / 500);
        this.ricePrice = (YimyConsts.RICE_COST * product.getWeight() * product.getSaleNum() / 500);
        this.express = (express.calcExpressPrice(product.getWeight() * product.getSaleNum()));
        this.totalCost = this.ricePrice + this.express + this.otherCost;
        this.profit = (this.salePrice - this.totalCost);
        this.profitRate = this.profit / this.salePrice * 100;
    }

    public double getSalePrice() {
        return salePrice;
    }

    public double getRicePrice() {
        return ricePrice;
    }

    public double getExpress() {
        return express;
    }

    public double getOtherCost() {
        return otherCost;
    }

    public double getTotalCost() {
        return totalCost;
    }

    public double getProfit() {
        return profit;
    }

    public double getProfitRate() {
        return profitRate;
    }
}
