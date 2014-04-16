package com.cm4j.test.generator.dict.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * 基础_国战每日任务
 *
 * @desc     代码生成器生成.
 * @date     2014-04-16
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "dict_country_daily_task")
public class DictCountryDailyTask {

    // 任务id
    private short id;
    // 任务名称
    private String name;
    // 任务描述
    private String desc;
    // 任务品质
    private byte quality;
    // 完成条件
    private String condition;
    // 完成奖励
    private String reward;
    // 秒CD元宝
    private short cash;
    // 任务出现概率
    private byte rate;
    // 扩展字段
    private String ext;

    @Id
    @Column(name = "n_id")
    public short getId(){
        return this.id;
    }

    public void setId(short id){
        this.id = id;
    }

    @Column(name = "s_name")
    public String getName(){
        return this.name;
    }

    public void setName(String name){
        this.name = name;
    }

    @Column(name = "s_desc")
    public String getDesc(){
        return this.desc;
    }

    public void setDesc(String desc){
        this.desc = desc;
    }

    @Column(name = "n_quality")
    public byte getQuality(){
        return this.quality;
    }

    public void setQuality(byte quality){
        this.quality = quality;
    }

    @Column(name = "s_condition")
    public String getCondition(){
        return this.condition;
    }

    public void setCondition(String condition){
        this.condition = condition;
    }

    @Column(name = "s_reward")
    public String getReward(){
        return this.reward;
    }

    public void setReward(String reward){
        this.reward = reward;
    }

    @Column(name = "n_cash")
    public short getCash(){
        return this.cash;
    }

    public void setCash(short cash){
        this.cash = cash;
    }

    @Column(name = "n_rate")
    public byte getRate(){
        return this.rate;
    }

    public void setRate(byte rate){
        this.rate = rate;
    }

    @Column(name = "s_ext")
    public String getExt(){
        return this.ext;
    }

    public void setExt(String ext){
        this.ext = ext;
    }

}