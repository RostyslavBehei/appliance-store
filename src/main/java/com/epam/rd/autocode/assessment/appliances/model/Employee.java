package com.epam.rd.autocode.assessment.appliances.model;

import com.epam.rd.autocode.assessment.appliances.model.enums.Role;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@DiscriminatorValue("EMPLOYEE")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Employee extends User {

    @Column(name = "department")
    private String department;

    @OneToMany(mappedBy = "employee", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Order> processedOrders = new ArrayList<>();

    public Employee(String firstName, String lastName, String middleName, String email, String password, LocalDate birthday, String department) {
        this.setFirstName(firstName);
        this.setLastName(lastName);
        this.setMiddleName(middleName);
        this.setEmail(email);
        this.setPassword(password);
        this.setBirthday(birthday);
        this.setDepartment(department);
        this.setRole(Role.ROLE_EMPLOYEE);
        this.setEnabled(true);
    }
}
