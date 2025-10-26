package com.bank.credit.mapper;

import com.bank.credit.model.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

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
    @Mapping(target = "associatedAccounts", source = "associatedAccounts")
    Credit toEntity(CreditRequest creditRequest);

    @Mapping(target = "associatedAccounts", source = "associatedAccounts")
    CreditResponse toResponse(Credit credit);

    @Mapping(target = "creditId", source = "id")
    @Mapping(target = "minimumPayment", expression = "java(calculateMinimumPayment(credit))")
    @Mapping(target = "dueDate", expression = "java(calculateDueDate())")
    @Mapping(target = "currency", constant = "PEN")
    CreditBalanceResponse toBalanceResponse(Credit credit);

    List<AssociatedAccount> mapAssociatedAccounts(List<CreditRequestAssociatedAccountsInner> associatedAccounts);

    @Mapping(target = "associatedAt", expression = "java(java.time.LocalDate.now())")
    @Mapping(target = "status", constant = "ACTIVA")
    AssociatedAccount mapAssociatedAccount(CreditRequestAssociatedAccountsInner inner);

    List<com.bank.credit.model.CreditResponseAssociatedAccountsInner> mapToResponseAssociatedAccounts(List<AssociatedAccount> associatedAccounts);

    @Mapping(target = "associatedAt", source = "associatedAt")
    com.bank.credit.model.CreditResponseAssociatedAccountsInner mapToResponseAssociatedAccount(AssociatedAccount associatedAccount);

    default OffsetDateTime mapLocalDateToOffsetDateTime(LocalDate localDate) {
        return localDate != null ? localDate.atStartOfDay().atOffset(ZoneOffset.UTC) : null;
    }

    default LocalDate mapOffsetDateTimeToLocalDate(OffsetDateTime offsetDateTime) {
        return offsetDateTime != null ? offsetDateTime.toLocalDate() : null;
    }

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