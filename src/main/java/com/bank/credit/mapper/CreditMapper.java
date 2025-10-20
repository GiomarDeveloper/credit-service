package com.bank.credit.mapper;

import com.bank.credit.model.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Mapper(componentModel = "spring")
public interface CreditMapper {
    CreditMapper INSTANCE = Mappers.getMapper(CreditMapper.class);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "outstandingBalance", expression = "java(creditRequest.getAmount())")
    @Mapping(target = "availableCredit", expression = "java(creditRequest.getCreditLimit() != null ? creditRequest.getCreditLimit() : 0.0)")
    @Mapping(target = "remainingPayments", expression = "java(creditRequest.getTermMonths() != null ? creditRequest.getTermMonths() : 0)")
    @Mapping(target = "status", constant = "ACTIVO")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Credit toEntity(CreditRequest creditRequest);

    CreditResponse toResponse(Credit credit);

    @Mapping(target = "creditId", source = "id")
    @Mapping(target = "minimumPayment", expression = "java(calculateMinimumPayment(credit))")
    @Mapping(target = "dueDate", expression = "java(calculateDueDate())")
    @Mapping(target = "currency", constant = "PEN")
    CreditBalanceResponse toBalanceResponse(Credit credit);

    default Double calculateMinimumPayment(Credit credit) {
        if (CreditTypeEnum.TARJETA_CREDITO.getValue().equals(credit.getCreditType())) {
            return credit.getOutstandingBalance() * 0.05;
        } else {
            return credit.getMonthlyPayment();
        }
    }

    default LocalDate calculateDueDate() {
        return LocalDate.now().plusDays(15);
    }

    default OffsetDateTime map(Instant instant) {
        return instant != null ? instant.atOffset(ZoneOffset.UTC) : null;
    }
}