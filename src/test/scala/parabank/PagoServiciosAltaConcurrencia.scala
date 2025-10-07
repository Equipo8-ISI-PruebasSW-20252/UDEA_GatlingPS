package parabank

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._
import parabank.Data._

class PagoServiciosAltaConcurrencia extends Simulation {

  val httpProtocol = http
    .baseUrl(loanUrl)
    .inferHtmlResources(
      BlackList(""".*\.css.*""", """.*\.js.*""", """.*\.png.*""", """.*\.jpg.*""", 
                 """.*\.gif.*""", """.*\.ico.*""", """.*\.svg.*""", 
                 """.*\.woff.*""", """.*\.ttf.*"""),
      WhiteList()
    )
    .acceptHeader("application/json, text/plain, */*")
    .contentTypeHeader("application/json")
    .userAgentHeader("Gatling")
    .disableWarmUp
    .shareConnections

  val scn = scenario("Pago de servicios - flujo REST funcional")

    // 1. Login vía servicio REST
    .exec(
      http("Login REST")
        .get(s"/services_proxy/bank/login/${username}/${password}")
        .check(status.is(200))
        .check(jsonPath("$.id").saveAs("customerId"))
    )

    // 2. Obtener cuentas del cliente
    .exec(
      http("Cuentas del cliente")
        .get("/services_proxy/bank/customers/${customerId}/accounts")
        .check(status.is(200))
        .check(jsonPath("$[0].id").saveAs("fromAccountId"))
    )

    // 3. Ejecutar pago
    .exec(
      http("Ejecutar pago de servicios")
        .post("/services_proxy/bank/billpay")
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
            "fromAccountId": "${fromAccountId}",
            "amount": 100.00
          }"""
        )).asJson
        .check(status.is(200))
        .check(jsonPath("$.payeeName").exists)
    )

  setUp(
    scn.inject(rampUsers(200) during (30.seconds))
  ).protocols(httpProtocol)
   .assertions(
     global.responseTime.mean.lte(3000),
     global.failedRequests.percent.lte(1)
   )
}


