package com.hfnew.dto.elderly;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;

@Data
public class ElderlyLeaveRequest {
    @NotNull(message = "请假开始日期不能为空")
    private LocalDate startDate;
    private LocalDate endDate;
    private String reason;
}
