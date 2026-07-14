package com.hfnew.dto.medication;

import lombok.Data;

import java.util.List;

@Data
public class TodayBoardVO {

    private Long elderlyId;
    private String elderlyName;
    private String bedNumber;
    private String supplyCategory;
    private List<TimeSlotGroup> timeSlots;

    @Data
    public static class TimeSlotGroup {
        private String slot;
        private String slotLabel;
        private String overallStatus;
        private List<MedicationRecordVO> records;
    }
}
