package parabank

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._
import parabank.Data._

class PagoServiciosAltaConcurrencia extends Simulation {

  val httpProtocol = http
    .baseUrl(loanUrl) // ejemplo: "https://parabank.parasoft.com/parabank"
    .inferHtmlResources(
      BlackList(""".*\.css""", """.*\.js""", """.*\.png""", """.*\.jpg""", """.*\.gif""", """.*\.svg""", """.*\.ico"""),
      WhiteList()
    )
    .acceptHeader("application/json, text/plain, */*")
    .contentTypeHeader("application/json")
    .userAgentHeader("Gatling")

  val scn = scenario("Pago de servicios - flujo REST funcional")

    // Home
    .exec(
      http("Home")
        .get("/index.htm")
        .check(status.is(200))
    )

    // Login REST (recupera customerId)
    .exec(
      http("Login REST")
        .post("/services/bank/login/${username}/${password}")
        .check(status.is(200))
        .check(jsonPath("$.id").saveAs("customerId"))
    )

    // Obtener cuentas del cliente (para sacar el accountId)
    .exec(
      http("Cuentas del cliente")
        .get("/services/bank/customers/${customerId}/accounts")
        .check(status.is(200))
        .check(jsonPath("$[0].id").saveAs("fromAccountId"))
    )

    // Realizar pago de servicios
    .exec(
      http("Ejecutar pago de servicios")
        .post("/services/bank/billpay")
        .body(StringBody(
          """{
            "name": "Electric Company",
            "address": {
              "street": "123 Main St",
              "city": "Springfield",
              "state": "IL",
              "zipCode": "62701"
            },
            "phoneNumber": "5555555555",
            "accountNumber": "12345",
            "amount": 100.00,
            "fromAccountId": ${fromAccountId}
          }"""
        )).asJson
        .check(status.in(200, 201))
    )

  // Inyección de usuarios concurrentes
  setUp(
    scn.inject(rampUsers(200) during (30.seconds))
  ).protocols(httpProtocol)
   .assertions(
     global.responseTime.mean.lte(3000),
     global.failedRequests.percent.lte(1)
   )
}



