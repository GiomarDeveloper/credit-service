package com.bank.credit.exception;

/**
 * Excepción lanzada cuando un recurso solicitado no puede ser encontrado.
 * <p>
 * Esta excepción se utiliza para indicar que un recurso específico (como un crédito,
 * cliente o cuenta) no existe en el sistema. Generalmente resulta en una respuesta
 * HTTP 404 Not Found.
 * </p>
 *
 */
public class ResourceNotFoundException extends RuntimeException {

  /**
   * Crea una nueva instancia de ResourceNotFoundException con el mensaje especificado.
   *
   * @param message el mensaje descriptivo del error
   */
  public ResourceNotFoundException(String message) {
    super(message);
  }
}