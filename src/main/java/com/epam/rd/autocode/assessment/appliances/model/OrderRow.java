package com.epam.rd.autocode.assessment.appliances.model;

import java.math.BigDecimal;

public class OrderRow {

    private Long id;

    private Appliance appliance;

    private Long number;

    private BigDecimal amount;

    public OrderRow() {
    }

    public OrderRow(Long id, Appliance appliance, Long number, BigDecimal amount) {
        this.id = id;
        this.appliance = appliance;
        this.number = number;
        this.amount = amount;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Appliance getAppliance() {
        return appliance;
    }

    public void setAppliance(Appliance appliance) {
        this.appliance = appliance;
    }

    public Long getNumber() {
        return number;
    }

    public void setNumber(Long number) {
        this.number = number;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}
