/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cv.integration.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.Hibernate;

import javax.persistence.*;
import java.util.Date;
import java.util.Objects;

/**
 * @author Wai Yan
 */
@Entity
@Getter
@Setter
@ToString
@RequiredArgsConstructor
@Table(name = "sale_his")
public class SaleHis implements java.io.Serializable {

    @Id
    @Column(name = "vou_no", unique = true, nullable = false, length = 20)
    private String vouNo;
    @Column(name = "trader_code")
    private String traderCode;
    @Column(name = "saleman_code")
    private String saleManCode;
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "vou_date")
    private Date vouDate;
    @Column(name = "cur_code")
    private String curCode;
    @Column(name = "remark")
    private String remark;
    @Column(name = "vou_total")
    private Float vouTotal;
    @Column(name = "discount")
    private Float discount;
    @Column(name = "tax_amt")
    private Float taxAmt;
    @Column(name = "deleted")
    private Boolean deleted;
    @Column(name = "paid")
    private Float paid;
    @Column(name = "vou_balance")
    private Float balance;
    @Column(name = "tax_p")
    private Float taxPercent;
    @Column(name = "intg_upd_status")
    private String intgUpdStatus;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        SaleHis saleHis = (SaleHis) o;
        return Objects.equals(vouNo, saleHis.vouNo);
    }

    @Override
    public int hashCode() {
        return 0;
    }
}
