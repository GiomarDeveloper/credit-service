package com.bank.credit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.kafka.annotation.EnableKafka;

/**
 * Clase principal de la aplicación del servicio de créditos.
 * <p>
 * Esta clase inicia la aplicación Spring Boot para el servicio de gestión de créditos,
 * configurando el cliente Eureka para el registro en el servicio de descubrimiento
 * y habilitando la integración con Kafka para el procesamiento de mensajes asíncronos.
 * </p>
 *
 * @see org.springframework.boot.SpringApplication
 * @see org.springframework.boot.autoconfigure.SpringBootApplication
 * @see org.springframework.cloud.netflix.eureka.EnableEurekaClient
 * @see org.springframework.kafka.annotation.EnableKafka
 */
@SpringBootApplication
@EnableEurekaClient
@EnableKafka
public class CreditServiceApplication {

  /**
   * Método principal que inicia la aplicación Spring Boot.
   * <p>
   * Este método inicializa el contexto de Spring y arranca el servicio de créditos
   * con todas las configuraciones necesarias para la integración con Eureka y Kafka.
   * </p>
   *
   * @param args argumentos de línea de comandos pasados a la aplicación
   *
   * @see SpringApplication#run(Class, String...)
   */
  public static void main(String[] args) {
    SpringApplication.run(CreditServiceApplication.class, args);
  }

}
