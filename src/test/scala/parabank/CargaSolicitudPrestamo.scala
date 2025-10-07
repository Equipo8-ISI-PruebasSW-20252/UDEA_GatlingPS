package parabank

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._
import parabank.Data._

class CargaSolicitudPrestamo extends Simulation {

  val httpProtocol = http
    .baseUrl(url)
    .acceptHeader("application/json, text/plain, */*")
    .userAgentHeader("Gatling")
    .disableWarmUp
  
  // Feeder para variar montos de préstamo y cuota inicial
  val loanFeeder = Iterator.continually(
    Map(
      "amount"      -> (1000 + util.Random.nextInt(7000)), // 1000–8000
      "downPayment" -> (100  + util.Random.nextInt(900))   // 100–1000
    )
  )

  val scn =
    scenario("Solicitud de préstamo bajo carga")
      .feed(loanFeeder)
      // 1) Login -> customerId
      .exec(
        http("Login")
          .get(s"/login/$username/$password")
          .check(status.is(200))
          .check(jsonPath("$.id").saveAs("customerId"))
      )
      // 2) Cuentas del cliente -> fromAccountId
      .exec(
        http("Cuentas del cliente")
          .get("/customers/${customerId}/accounts")
          .check(status.is(200))
          .check(jsonPath("$[0].id").saveAs("fromAccountId"))
      )
      // 3) Solicitud de préstamo (POST con query params)
      .exec(
        http("Request Loan")
          .post("/requestLoan")
          .queryParam("customerId",   "${customerId}")
          .queryParam("fromAccountId","${fromAccountId}")
          .queryParam("amount",       "${amount}")
          .queryParam("downPayment",  "${downPayment}")
          .check(status.is(200))
          // ✅ El servicio responde JSON con booleano "approved"
          .check(jsonPath("$.approved").exists)
          .check(jsonPath("$.approved").saveAs("approved"))
      )

  setUp(
    scn.inject(
      rampUsers(150) during (30.seconds) // 150 concurrentes en 30s
    )
  )
  .protocols(httpProtocol)
  .assertions(
    global.responseTime.mean.lte(5000),         // ≤ 5 s
    global.successfulRequests.percent.gte(98),  // ≥ 98 %
    global.failedRequests.percent.lte(2)        // ≤ 2 %
  )
}
