package parabank

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._
import parabank.Data._

class CargaSolicitudPrestamo extends Simulation {

  val httpProtocol = http
    .baseUrl(loanUrl)
    .acceptHeader("application/json, text/html")
    .check(status.not(500))

  // Escenario: login + solicitud de préstamo
  val solicitudPrestamo = scenario("Solicitud de Préstamo Bajo Carga")
    // 🔹 Paso 1: login (autenticación previa)
    .exec(
      http("Login")
        .post("/login.htm")
        .formParam("username", username)
        .formParam("password", password)
        .check(status.is(200))
    )
    .pause(1.second)
    // 🔹 Paso 2: solicitud de préstamo
    .exec(
      http("POST Solicitud de Préstamo")
        .post("/requestloan.htm")
        .formParam("amount", amount * 50)    //prestamo de 50
        .formParam("downPayment", amount * 10)    //pago inicial de 10
        .formParam("fromAccountId", fromAccountId)
        .check(status.is(200))
        .check(regex("Loan Request Processed").exists)
    )
    .pause(1.second)

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
