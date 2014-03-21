package com.cm4j.test.generator.dict.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * 国战_城池迷雾表
 *
 * @desc 代码生成器生成.
 * @date 2014-03-21
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "dict_country_fog")
public class DictCountryFog {

    // 迷雾ID
    private int id;
    // NPC信息
    private int npc;

    @Id
    @Column(name = "n_id")
    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Column(name = "n_npc")
    public int getNpc() {
        return this.npc;
    }

    public void setNpc(int npc) {
        this.npc = npc;
    }

}