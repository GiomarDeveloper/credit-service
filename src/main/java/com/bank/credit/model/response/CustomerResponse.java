package com.bank.credit.model.response;

import lombok.Data;

import java.time.Instant;

@Data
public class CustomerResponse {
    private String id;
    private String documentType;
    private String documentNumber;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String customerType;
    private Instant createdAt;
}