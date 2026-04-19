package com.hfnew.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CalendarEventItemDTO {

    /** 事件类型：ADMISSION / DISCHARGE */
    private String type;

    /** 老人类别：SOCIAL / LOW_BAO / WU_BAO，退住事件时可能为null */
    private String category;

    /** 老人姓名 */
    private String elderlyName;

    /** 老人ID */
    private Long elderlyId;
}
