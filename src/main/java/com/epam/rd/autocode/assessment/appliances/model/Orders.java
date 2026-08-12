package com.epam.rd.autocode.assessment.appliances.model;

import java.util.HashSet;
import java.util.Set;

public class Orders {

    private Long id;

    private Employee employee;

    private Client client;

    private Set<OrderRow> orderRowSet = new HashSet<>();

    private Boolean approved = false;

    public Orders() {
    }

    public Orders(Long id, Employee employee, Client client, Set<OrderRow> orderRowSet, Boolean approved) {
        this.id = id;
        this.employee = employee;
        this.client = client;
        this.orderRowSet = orderRowSet;
        this.approved = approved;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Employee getEmployee() {
        return employee;
    }

    public void setEmployee(Employee employee) {
        this.employee = employee;
    }

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public Set<OrderRow> getOrderRowSet() {
        return orderRowSet;
    }

    public void setOrderRowSet(Set<OrderRow> orderRowSet) {
        this.orderRowSet = orderRowSet;
    }

    public Boolean getApproved() {
        return approved;
    }

    public void setApproved(Boolean approved) {
        this.approved = approved;
    }
}
