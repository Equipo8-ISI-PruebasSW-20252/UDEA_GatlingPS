package parabank

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._
import parabank.Data._

class PagoServiciosAltaConcurrencia extends Simulation {

  val httpProtocol = http
    .baseUrl(loanUrl) // ejemplo: "https://parabank.parasoft.com/parabank"
    // Excluir recursos estáticos
    .inferHtmlResources(
      BlackList(""".*\.css.*""", """.*\.js.*""", """.*\.png.*""", """.*\.jpg.*""", 
                 """.*\.gif.*""", """.*\.ico.*""", """.*\.svg.*""", 
                 """.*\.woff.*""", """.*\.ttf.*"""),
      WhiteList()
    )
    .acceptHeader("application/json, text/plain, */*")
    .contentTypeHeader("application/json")
    .userAgentHeader("Gatling")

  val scn = scenario("Pago de servicios - flujo JSON correcto")

    // Home
    .exec(
      http("Home")
        .get("/index.htm")
        .check(status.is(200))
    )

    // Login
    .exec(
      http("Login")
        .post("/login.htm")
        .formParam("username", username)
        .formParam("password", password)
        .check(status.in(200, 302))
    )

    // Ir a la sección de Bill Pay
    .exec(
      http("Abrir módulo Bill Pay")
        .get("/billpay.htm")
        .check(status.is(200))
    )

    // Realizar el pago real con JSON
    .exec(
      http("Ejecutar pago de servicios")
        .post("/services_proxy/bank/billpay?accountId=12567&amount=100.00")
        .header("Content-Type", "application/json")
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
            "accountNumber": "12345"
          }"""
        )).asJson
        .check(status.in(200, 201, 302))
    )

  // Carga de usuarios concurrentes
  setUp(
    scn.inject(rampUsers(200) during (30.seconds))
  ).protocols(httpProtocol)
   .assertions(
     global.responseTime.mean.lte(3000),
     global.failedRequests.percent.lte(1)
   )
}


