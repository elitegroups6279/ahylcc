package com.hfnew.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CalendarDayEventsDTO {

    /** 日期，格式 yyyy-MM-dd */
    private String date;

    /** 当日事件列表 */
    private List<CalendarEventItemDTO> events;
}
