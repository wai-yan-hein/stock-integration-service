/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cv.integration.entity;

import lombok.*;
import org.hibernate.Hibernate;

import javax.persistence.*;
import java.util.Date;
import java.util.Objects;

/**
 * @author winswe
 */
@Getter
@Setter
@ToString
@Entity
@Table(name = "pur_his")
public class PurHis implements java.io.Serializable {

    @Id
    @Column(name = "vou_no", unique = true, nullable = false, length = 15)
    private String vouNo;
    @Column(name = "trader_code")
    private String traderCode;
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "vou_date")
    private Date vouDate;
    @Temporal(TemporalType.DATE)
    @Column(name = "due_date")
    private Date dueDate;
    @Column(name = "deleted")
    private Boolean deleted;
    @Column(name = "vou_total")
    private Float vouTotal;
    @Column(name = "paid")
    private Float paid;
    @Column(name = "discount")
    private Float discount;
    @Column(name = "balance")
    private Float balance;
    @Column(name = "remark")
    private String remark;
    @Column(name = "cur_code")
    private String curCode;
    @Column(name = "tax_amt")
    private Float taxAmt;
    @Column(name = "intg_upd_status")
    private String intgUpdStatus;

    public PurHis() {
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        PurHis purHis = (PurHis) o;
        return Objects.equals(vouNo, purHis.vouNo);
    }

    @Override
    public int hashCode() {
        return 0;
    }
}
