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
        .formParam("customerId", username)
        .formParam("amount", amount * 500)
        .formParam("downPayment", amount * 50)
        .formParam("fromAccountId", fromAccountId)
        .check(status.is(200))
        .check(regex("Loan Request Processed").exists) 
    )
    .pause(1.second)

  // Configuración de usuarios y carga
  setUp(
    solicitudPrestamo.inject(
      rampUsers(150) during (30.seconds)
    )
  )
  .protocols(httpProtocol)
  .assertions(
    global.responseTime.mean.lte(5000),        // tiempo promedio ≤ 5s
    global.successfulRequests.percent.gte(98), // tasa de éxito ≥ 98%
    global.failedRequests.percent.lte(2)       // errores inesperados ≤ 2%
  )
}
