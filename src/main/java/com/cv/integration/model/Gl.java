package com.cv.integration.model;

import lombok.Data;

import java.util.Date;

@Data
public class Gl implements java.io.Serializable {

    private String glCode;
    private Date glDate;
    private String description;
    private String srcAccCode;
    private String accCode;
    private String curCode;
    private Double drAmt;
    private Double crAmt;
    private String reference;
    private String deptCode;
    private String vouNo;
    private String traderCode;
    private String compCode;
    private Date createdDate;
    private String createdBy;
    private String tranSource;
    private String remark;
    private Integer macId;
    private String refNo;
    private boolean deleted;
    private boolean cash = false;

    public Gl() {
    }
}
