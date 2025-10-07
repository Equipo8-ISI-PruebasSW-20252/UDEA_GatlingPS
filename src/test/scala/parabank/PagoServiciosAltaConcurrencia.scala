package parabank

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._
import parabank.Data._

class PagoServiciosAltaConcurrencia extends Simulation {

  val httpProtocol = http
    .baseUrl(url)
    .acceptHeader("application/json")
    .contentTypeHeader("application/json")
    .userAgentHeader("Gatling")
    .disableWarmUp

  // Datos dinámicos
  val feeder = Iterator.continually(
    Map(
      "payeeName" -> ("Empresa" + util.Random.nextInt(100)),
      "amount" -> (10 + util.Random.nextInt(500)),
      "accountNumber" -> ("000" + util.Random.nextInt(9999))
    )
  )

  val scn = scenario("Pago de servicios con alta concurrencia")
    .feed(feeder)

    // Login
    .exec(
      http("Login")
        .get(s"/login/$username/$password")
        .check(status.is(200))
        .check(jsonPath("$.id").saveAs("customerId"))
    )

    // Obtener cuentas del cliente
    .exec(
      http("Obtener cuentas")
        .get("/customers/${customerId}/accounts")
        .check(status.is(200))
        .check(jsonPath("$[0].id").saveAs("fromAccountId"))
    )

    // Pago de servicio
    .exec(
      http("Ejecutar Bill Pay")
        .post("/services/billpay")
        .body(StringBody(
          """{
            "customerId": ${customerId},
            "fromAccountId": ${fromAccountId},
            "payeeName": "${payeeName}",
            "accountNumber": "${accountNumber}",
            "amount": ${amount}
          }"""
        ))
        .check(status.is(200))
        .check(jsonPath("$.message").exists)
    )

  setUp(
    scn.inject(
      rampUsers(200) during (30.seconds)
    )
  )
  .protocols(httpProtocol)
  .assertions(
    global.responseTime.mean.lte(3000),
    global.successfulRequests.percent.gte(98),
    global.failedRequests.percent.lte(2)
  )
}


