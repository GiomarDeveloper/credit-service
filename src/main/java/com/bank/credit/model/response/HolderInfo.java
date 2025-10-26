package com.bank.credit.model.response;

import lombok.Data;

@Data
public class HolderInfo {
    private String customerId;
    private String name;
    private String relationship;
}