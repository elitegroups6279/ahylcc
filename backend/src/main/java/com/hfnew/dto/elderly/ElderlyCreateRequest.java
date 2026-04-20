package com.hfnew.dto.elderly;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class ElderlyCreateRequest {
    private String name;
    private String idCard;
    private Integer gender;
    private LocalDate birthDate;
    private Integer age;
    private LocalDate admissionDate;
    private Long bedId;
    private String customBedNumber;
    private String category;
    private Integer enableLongCare;
    private Integer enableCoupon;
    private BigDecimal contractMonthlyFee;
    private BigDecimal deposit;
    private LocalDate contractStartDate;
    private Integer contractMonths;
    private String paymentMethod;
    private String bankAccount;
    private String careLevel;
    private String disabilityLevel;
    private List<ElderlyContactDTO> contacts;
    private List<Long> staffIds;
}
