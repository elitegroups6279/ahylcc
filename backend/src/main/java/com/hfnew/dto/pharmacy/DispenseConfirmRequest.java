package com.hfnew.dto.pharmacy;

import lombok.Data;

import java.util.List;

@Data
public class DispenseConfirmRequest {
    private List<DispenseItemRequest> items;
}
