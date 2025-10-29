package com.bank.credit.model.response;

import java.time.Instant;
import lombok.Data;

/**
 * DTO de respuesta para información de cliente.
 * <p>
 * Representa la estructura de datos de un cliente tal como es expuesta
 * por el microservicio de clientes.
 * </p>
 *
 */
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