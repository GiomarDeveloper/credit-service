package com.bank.credit.repository;

import com.bank.credit.model.Credit;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;

/**
 * Repositorio reactivo para la gestión de entidades Credit en MongoDB.
 * <p>
 * Esta interfaz proporciona métodos para realizar operaciones CRUD reactivas
 * sobre la colección de créditos, incluyendo consultas personalizadas por
 * diversos criterios de búsqueda.
 * </p>
 *
 */
public interface CreditRepository extends ReactiveMongoRepository<Credit, String> {

  /**
   * Encuentra todos los créditos de un cliente específico.
   *
   * @param customerId identificador único del cliente
   * @return Flux de créditos del cliente, vacío si no se encuentran
   */
  Flux<Credit> findByCustomerId(String customerId);

  /**
   * Encuentra todos los créditos de un tipo específico.
   *
   * @param creditType tipo de crédito a filtrar
   * @return Flux de créditos del tipo especificado
   */
  Flux<Credit> findByCreditType(String creditType);

  /**
   * Encuentra créditos de un cliente filtrados por tipo.
   *
   * @param customerId identificador único del cliente
   * @param creditType tipo de crédito a filtrar
   * @return Flux de créditos que coinciden con ambos criterios
   */
  Flux<Credit> findByCustomerIdAndCreditType(String customerId, String creditType);

  /**
   * Encuentra todos los créditos con un estado específico.
   *
   * @param status estado del crédito a filtrar
   * @return Flux de créditos con el estado especificado
   */
  Flux<Credit> findByStatus(String status);

  /**
   * Encuentra créditos de un cliente filtrados por estado.
   *
   * @param customerId identificador único del cliente
   * @param status estado del crédito a filtrar
   * @return Flux de créditos que coinciden con ambos criterios
   */
  Flux<Credit> findByCustomerIdAndStatus(String customerId, String status);
}