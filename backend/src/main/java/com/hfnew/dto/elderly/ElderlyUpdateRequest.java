package com.hfnew.dto.elderly;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class ElderlyUpdateRequest {
    private String name;
    private Integer gender;
    private LocalDate birthDate;
    private Integer age;
    private String category;
    private Integer enableLongCare;
    private Integer enableCoupon;
    private BigDecimal contractMonthlyFee;
    private BigDecimal deposit;
    private Integer contractMonths;
    private String paymentMethod;
    private String bankAccount;
    private String careLevel;
    private String disabilityLevel;
    private List<ElderlyContactDTO> contacts;
    private List<Long> staffIds;
}
