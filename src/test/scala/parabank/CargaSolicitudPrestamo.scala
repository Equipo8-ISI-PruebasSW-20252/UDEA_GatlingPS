package parabank

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._
import parabank.Data._

class CargaSolicitudPrestamo extends Simulation {

  // Configuración del protocolo HTTP
  val httpProtocol = http
    .baseUrl(url)
    .acceptHeader("application/json")
    .check(status.is(200))

  // Escenario: solicitud de préstamo bajo carga
  val solicitudPrestamo = scenario("Solicitud de Préstamo Bajo Carga")
    .exec(
      http("POST Solicitud de Préstamo")
        .post("/requestloan")
        .formParam("customerId", username)      // usuario del préstamo
        .formParam("amount", amount * 500)      // monto base adaptado (usa Data.amount)
        .formParam("downPayment", amount * 50)  // pago inicial proporcional
        .formParam("fromAccountId", fromAccountId)
        .check(status.is(200))
        .check(regex("Loan Request Processed").exists.optional) // valida respuesta esperada
    )
    .pause(1.second) // pausa entre solicitudes para simular comportamiento real

  // Configuración de usuarios concurrentes y duración
  setUp(
    solicitudPrestamo.inject(
      rampUsers(150) during (30.seconds) // simula 150 usuarios en 30s
    )
  )
  .protocols(httpProtocol)
  .assertions(
    global.responseTime.mean.lte(5000),        // tiempo promedio ≤ 5s
    global.successfulRequests.percent.gte(98), // tasa de éxito ≥ 98%
    global.failedRequests.percent.lte(2)       // errores inesperados ≤ 2%
  )
}
