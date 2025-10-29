package com.bank.credit.mapper;

import com.bank.credit.model.AssociatedAccount;
import com.bank.credit.model.Credit;
import com.bank.credit.model.CreditBalanceResponse;
import com.bank.credit.model.CreditRequest;
import com.bank.credit.model.CreditRequestAssociatedAccountsInner;
import com.bank.credit.model.CreditResponse;
import com.bank.credit.model.CreditTypeEnum;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

/**
 * Mapper para conversión entre entidades de crédito y DTOs.
 * <p>
 * Esta interfaz utiliza MapStruct para mapear automáticamente entre
 * las entidades de dominio y los objetos de transferencia de datos (DTOs)
 * utilizados en la API y eventos.
 * </p>
 * <p>
 * Incluye métodos personalizados para cálculos específicos del negocio
 * como el pago mínimo y fechas de vencimiento.
 * </p>
 *
 */
@Mapper(componentModel = "spring")
public interface CreditMapper {
  CreditMapper INSTANCE = Mappers.getMapper(CreditMapper.class);

  /**
   * Convierte un CreditRequest en una entidad Credit.
   *
   * @param creditRequest DTO de solicitud de crédito
   * @return entidad Credit mapeada
   */
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "outstandingBalance", expression = "java(creditRequest.getAmount())")
  @Mapping(target = "availableCredit", expression = "java(creditRequest.getCreditLimit() != null ? creditRequest.getCreditLimit() : 0.0)")
  @Mapping(target = "remainingPayments", expression = "java(creditRequest.getTermMonths() != null ? creditRequest.getTermMonths() : 0)")
  @Mapping(target = "status", constant = "ACTIVO")
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  @Mapping(target = "associatedAccounts", source = "associatedAccounts")
  Credit toEntity(CreditRequest creditRequest);

  /**
   * Convierte una entidad Credit en un CreditResponse.
   *
   * @param credit entidad de crédito
   * @return DTO de respuesta de crédito
   */
  @Mapping(target = "associatedAccounts", source = "associatedAccounts")
  CreditResponse toResponse(Credit credit);

  /**
   * Convierte una entidad Credit en un CreditBalanceResponse.
   *
   * @param credit entidad de crédito
   * @return DTO de respuesta de balance de crédito
   */
  @Mapping(target = "creditId", source = "id")
  @Mapping(target = "minimumPayment", expression = "java(calculateMinimumPayment(credit))")
  @Mapping(target = "dueDate", expression = "java(calculateDueDate())")
  @Mapping(target = "currency", constant = "PEN")
  CreditBalanceResponse toBalanceResponse(Credit credit);

  List<AssociatedAccount> mapAssociatedAccounts(
    List<CreditRequestAssociatedAccountsInner> associatedAccounts);

  @Mapping(target = "associatedAt", expression = "java(java.time.LocalDate.now())")
  @Mapping(target = "status", constant = "ACTIVA")
  AssociatedAccount mapAssociatedAccount(CreditRequestAssociatedAccountsInner inner);

  List<com.bank.credit.model.CreditResponseAssociatedAccountsInner> mapToResponseAssociatedAccounts(
    List<AssociatedAccount> associatedAccounts);

  @Mapping(target = "associatedAt", source = "associatedAt")
  com.bank.credit.model.CreditResponseAssociatedAccountsInner mapToResponseAssociatedAccount(
    AssociatedAccount associatedAccount);

  default OffsetDateTime mapLocalDateToOffsetDateTime(LocalDate localDate) {
    return localDate != null ? localDate.atStartOfDay().atOffset(ZoneOffset.UTC) : null;
  }

  default LocalDate mapOffsetDateTimeToLocalDate(OffsetDateTime offsetDateTime) {
    return offsetDateTime != null ? offsetDateTime.toLocalDate() : null;
  }

  /**
   * Calcula el pago mínimo basado en el tipo de crédito.
   * <p>
   * Para tarjetas de crédito: 5% del saldo pendiente.
   * Para otros tipos de crédito: pago mensual establecido.
   * </p>
   *
   * @param credit entidad de crédito
   * @return monto del pago mínimo calculado
   */
  default Double calculateMinimumPayment(Credit credit) {
    if (CreditTypeEnum.TARJETA_CREDITO.getValue().equals(credit.getCreditType())) {
      return credit.getOutstandingBalance() * 0.05;
    } else {
      return credit.getMonthlyPayment();
    }
  }

  /**
   * Calcula la fecha de vencimiento del pago.
   * <p>
   * Establece la fecha de vencimiento a 15 días desde la fecha actual.
   * </p>
   *
   * @return fecha de vencimiento calculada
   */
  default LocalDate calculateDueDate() {
    return LocalDate.now().plusDays(15);
  }

  default OffsetDateTime map(Instant instant) {
    return instant != null ? instant.atOffset(ZoneOffset.UTC) : null;
  }
}