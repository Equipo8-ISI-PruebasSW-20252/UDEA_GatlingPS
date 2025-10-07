package parabank

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._
import parabank.Data._

class PagoServiciosAltaConcurrencia extends Simulation {

  val httpProtocol = http
    .baseUrl(loanUrl)
    .acceptHeader("text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
    .userAgentHeader("Gatling")
    .disableFollowRedirect

  // Feeder con datos variables
  val payFeeder = Iterator.continually {
    val amount = 10000 + util.Random.nextInt(90000)
    Map(
      "payAmount" -> amount,
      "payeeName" -> "Utility Co",
      "payeeAcct" -> "999999"
    )
  }

  val scn = scenario("Pago de servicios con alta concurrencia")
    .feed(payFeeder)

    // Paso 1: Login
    .exec(
      http("Login")
        .post("/login.htm")
        .formParam("username", username)
        .formParam("password", password)
        .check(status.is(200))
    )

    // Paso 2: Acceso al módulo de Bill Pay
    .exec(
      http("Abrir módulo Bill Pay")
        .get("/billpay.htm")
        .check(status.is(200))
    )

    // Paso 3: Realizar pago
    .exec(
      http("Ejecutar Bill Pay")
        .post("/services/bank/billpay")
        .formParam("payee.name", "${payeeName}")
        .formParam("payee.address.street", "Cra 123 #45-67")
        .formParam("payee.address.city", "Medellin")
        .formParam("payee.address.state", "ANT")
        .formParam("payee.address.zipCode", "050001")
        .formParam("payee.phoneNumber", "3000000000")
        .formParam("payee.accountNumber", "${payeeAcct}")
        .formParam("amount", "${payAmount}")
        .formParam("fromAccountId", fromAccountId)
        .check(status.is(200))
        .check(bodyString.saveAs("responseBody"))
    )

    // Paso 4: Validación simple
    .exec { session =>
      val response = session("responseBody").asOption[String].getOrElse("")
      if (!response.contains("Bill Payment Complete")) {
        session.markAsFailed
      } else session
    }

  // Inyección de usuarios concurrentes
  setUp(
    scn.inject(rampUsers(200) during (30.seconds))
  ).protocols(httpProtocol)
   .assertions(
     global.responseTime.mean.lte(3000),
     global.failedRequests.percent.lte(1)
   )
}
