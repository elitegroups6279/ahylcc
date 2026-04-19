package com.hfnew.dto.staff;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class StaffUpdateRequest {
    private String name;
    private String phone;
    private Integer gender;
    private LocalDate birthDate;
    private Integer age;
    private LocalDate hireDate;
    private LocalDate resignDate;
    private String resignReason;
    private String jobType;
    private BigDecimal baseSalary;
    private String status;
    private String probationStatus;
    private Integer probationMonths;
    private Integer hasCaregiverCert;
    private Integer hasHealthCert;
    private String positionType;
}
