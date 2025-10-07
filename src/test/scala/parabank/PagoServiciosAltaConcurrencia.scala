package parabank

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._
import parabank.Data._

class PagoServiciosAltaConcurrencia extends Simulation {

  val httpProtocol = http
    .baseUrl(url)
    .acceptHeader("text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
    .contentTypeHeader("application/x-www-form-urlencoded")
    .userAgentHeader("Gatling")
    .disableWarmUp
    .inferHtmlResources() // detecta recursos automáticamente
    .shareConnections // reutiliza conexiones HTTP

  // Feeder para generar datos dinámicos
  val billPayFeeder = Iterator.continually(
    Map(
      "payeeName"     -> ("Empresa" + util.Random.nextInt(100)),
      "amount"        -> (10 + util.Random.nextInt(500)), // 10–510
      "accountNumber" -> ("000" + util.Random.nextInt(9999))
    )
  )

  val scn = scenario("Pago de servicios con alta concurrencia")
    .feed(billPayFeeder)

    // Login
    .exec(
      http("Login")
        .post("/login")
        .formParam("username", username)
        .formParam("password", password)
        .check(status.is(200))
        .check(regex("Accounts Overview").exists)
    )

    // Abrir módulo Bill Pay
    .exec(
      http("Abrir módulo Bill Pay")
        .get("/billpay.htm")
        .check(status.is(200))
        .check(regex("Payee Name").exists)
    )

    // Ejecutar el pago con encabezado correcto
    .exec(
      http("Ejecutar Bill Pay")
        .post("/billpay")
        .formParam("payee.name", "${payeeName}")
        .formParam("payee.address.street", "Calle 123")
        .formParam("payee.address.city", "Bogota")
        .formParam("payee.address.state", "DC")
        .formParam("payee.address.zipCode", "110111")
        .formParam("payee.phoneNumber", "3001234567")
        .formParam("payee.accountNumber", "${accountNumber}")
        .formParam("verifyAccount", "${accountNumber}")
        .formParam("amount", "${amount}")
        .formParam("fromAccountId", "12345") // puedes parametrizarlo si lo deseas
        .check(status.is(200))
        .check(regex("Bill Payment Complete").exists)
    )

  setUp(
    scn.inject(
      rampUsers(200) during (30.seconds) // 200 usuarios en 30 s
    )
  ).protocols(httpProtocol)
    .assertions(
      global.responseTime.mean.lte(3000), // ≤ 3 s
      global.successfulRequests.percent.gte(98), // ≥ 98 %
      global.failedRequests.percent.lte(2) // ≤ 2 %
    )
}


