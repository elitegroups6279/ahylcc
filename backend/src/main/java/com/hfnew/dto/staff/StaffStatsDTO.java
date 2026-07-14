package com.hfnew.dto.staff;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 护工统计概览 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffStatsDTO {

    /** 护工总数 */
    private long total;

    /** 在职护工数 */
    private long activeCount;

    /** 实习护工数 */
    private long internCount;

    /** 有护工证数 */
    private long certCount;
}
