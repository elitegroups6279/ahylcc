package com.hfnew.dto.elderly;

import lombok.Data;
import java.time.LocalDate;

@Data
public class ElderlyReturnRequest {
    /** 实际返院日期，不传则默认为今天 */
    private LocalDate returnDate;
}
