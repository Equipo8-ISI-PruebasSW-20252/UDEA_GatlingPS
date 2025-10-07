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

  val scn = scenario("Simulación de alta concurrencia - flujo Bill Pay simulado")

    // Paso 1: acceso al sitio
    .exec(
      http("Home")
        .get("/index.htm")
        .check(status.is(200))
    )

    // Paso 2: intento de login
    .exec(
      http("Login")
        .post("/login.htm")
        .formParam("username", username)
        .formParam("password", password)
        .check(status.in(200, 302)) // tolera redirect
    )

    // Paso 3: apertura de módulo Bill Pay
    .exec(
      http("Abrir módulo Bill Pay")
        .get("/billpay.htm")
        .check(status.is(200))
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

