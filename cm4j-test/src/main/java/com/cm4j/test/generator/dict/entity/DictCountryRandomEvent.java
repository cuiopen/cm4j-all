package com.cm4j.test.generator.dict.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * 国战随机事件表
 *
 * @desc     代码生成器生成.
 * @date     2014-03-25
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "dict_country_random_event")
public class DictCountryRandomEvent {

    // 事件ID
    private byte id;
    // 事件类型
    private byte type;
    // 名称
    private int name;
    // ICON
    private String icon;
    // 持续时间
    private int seconds;
    // 完成条件
    private String condition;
    // 完成奖励
    private String reward;
    // 描述
    private String desc;

    @Id
    @Column(name = "n_id")
    public byte getId(){
        return this.id;
    }

    public void setId(byte id){
        this.id = id;
    }

    @Column(name = "n_type")
    public byte getType(){
        return this.type;
    }

    public void setType(byte type){
        this.type = type;
    }

    @Column(name = "s_name")
    public int getName(){
        return this.name;
    }

    public void setName(int name){
        this.name = name;
    }

    @Column(name = "s_icon")
    public String getIcon(){
        return this.icon;
    }

    public void setIcon(String icon){
        this.icon = icon;
    }

    @Column(name = "n_seconds")
    public int getSeconds(){
        return this.seconds;
    }

    public void setSeconds(int seconds){
        this.seconds = seconds;
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

    @Column(name = "s_desc")
    public String getDesc(){
        return this.desc;
    }

    public void setDesc(String desc){
        this.desc = desc;
    }

}