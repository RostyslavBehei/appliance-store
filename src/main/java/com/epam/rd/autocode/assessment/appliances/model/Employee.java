package com.epam.rd.autocode.assessment.appliances.model;

public class Employee extends User {

    private String department;

    public Employee() {}

    public Employee(Long id ,String name, String email, String password, String department) {
        super(id, name, email, password);
        this.department = department;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }
}
