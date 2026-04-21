package com.hfnew.dto.elderly;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ElderlyVO {
    private Long id;
    private String uniqueNo;
    private String name;
    private String idCardMasked;
    private String idCard;
    private Integer gender;
    private LocalDate birthDate;
    private Integer age;
    private LocalDate admissionDate;
    private Long bedId;
    private String bedNumber;
    private String building;
    private String floor;
    private String roomNumber;
    private String category;
    private String careLevel;
    private String disabilityLevel;
    private Integer enableLongCare;
    private Integer enableCoupon;
    private BigDecimal contractMonthlyFee;
    private BigDecimal deposit;
    private Integer contractMonths;
    private String paymentMethod;
    private String bankAccount;
    private String status;
    private LocalDate dischargeDate;
    private String dischargeReason;
    private LocalDateTime createTime;
    private List<ElderlyContactDTO> contacts;
    private List<Long> staffIds;
    private BigDecimal feeBalance;
}
