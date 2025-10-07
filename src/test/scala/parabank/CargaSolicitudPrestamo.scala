package parabank

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._
import parabank.Data._

class CargaSolicitudPrestamo extends Simulation {

  // Usar SIEMPRE la API REST para pruebas de carga
  val httpProtocol = http
    .baseUrl(url) // <- https://parabank.parasoft.com/parabank/services/bank
    .acceptHeader("application/json, text/plain, */*")
    .userAgentHeader("Gatling")
    .disableWarmUp

  // Genera montos variados para cubrir casos realistas
  val loanFeeder = Iterator.continually(
    Map(
      "amount"       -> (1000 + util.Random.nextInt(7000)), // 1000–8000
      "downPayment"  -> (100  + util.Random.nextInt(900))   // 100–1000
    )
  )

  /** Flujo:
    * 1) Login -> guarda customerId
    * 2) Obtiene cuentas del cliente -> guarda fromAccountId
    * 3) POST /requestLoan con params (customerId, fromAccountId, amount, downPayment)
    * 4) Checks: HTTP 200 y body con Approved/Denied (respuesta válida)
    */
  val solicitudPrestamo =
    scenario("Solicitud de préstamo bajo carga")
      .feed(loanFeeder)
      // 1) Login
      .exec(
        http("Login")
          .get(s"/login/$username/$password")
          .check(status.is(200))
          .check(jsonPath("$.id").saveAs("customerId"))
      )
      // 2) Cuentas del cliente
      .exec(
        http("Cuentas del cliente")
          .get("/customers/${customerId}/accounts")
          .check(status.is(200))
          // toma la primera cuenta válida
          .check(jsonPath("$[0].id").saveAs("fromAccountId"))
      )
      // 3) Request Loan (POST con query params)
      .exec(
        http("Request Loan")
          .post("/requestLoan")
          .queryParam("customerId", "${customerId}")
          .queryParam("fromAccountId", "${fromAccountId}")
          .queryParam("amount", "${amount}")
          .queryParam("downPayment", "${downPayment}")
          .check(status.is(200))
          .check(regex("Approved|Denied").exists) // respuesta válida (sin errores)
      )

  // Inyección de usuarios y aserciones de tu HU
  setUp(
    solicitudPrestamo.inject(
      rampUsers(150) during (30.seconds)   // 150 concurrentes gradualmente en 30s
    )
  )
  .protocols(httpProtocol)
  .assertions(
    global.responseTime.mean.lte(5000),         // promedio ≤ 5s
    global.successfulRequests.percent.gte(98),  // éxito ≥ 98 %
    global.failedRequests.percent.lte(2)        // errores inesperados ≤ 2 %
  )
}
