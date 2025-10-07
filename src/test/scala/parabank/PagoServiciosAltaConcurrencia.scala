package parabank

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._
import parabank.Data._

class PagoServiciosAltaConcurrencia extends Simulation {

  val httpProtocol = http
    .baseUrl(loanUrl)
    .acceptHeader("text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
    .acceptEncodingHeader("gzip, deflate")
    .acceptLanguageHeader("en-US,en;q=0.5")
    .userAgentHeader("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
    .disableFollowRedirect

  val scn = scenario("Pago de servicios con alta concurrencia")
    
    // 1. Login - versión mejorada
    .exec(
      http("Login")
        .post("/login.htm")
        .formParam("username", username)
        .formParam("password", password)
        .check(status.is(200))
        .check(css("a[href*='overview']").exists)
    )
    .pause(1.second)
    
    // 2. Ir a overview primero para estabilizar la sesión
    .exec(
      http("Overview Page")
        .get("/overview.htm")
        .check(status.is(200))
        .check(css("td#balance").exists)
    )
    .pause(1.second)
    
    // 3. Ir a bill pay
    .exec(
      http("Bill Pay Page")
        .get("/billpay.htm")
        .check(status.is(200))
        .check(
          css("select[name='fromAccountId'] option", "value")
            .find
            .saveAs("accountId")
        )
    )
    .pause(1.second)
    
    // 4. Pago
    .exec(
      http("Bill Payment")
        .post("/billpay.htm")
        .formParam("input", "Send Payment")
        .formParam("payee.name", "Test Utility Company")
        .formParam("payee.address.street", "123 Main St")
        .formParam("payee.address.city", "Medellin")
        .formParam("payee.address.state", "ANT")
        .formParam("payee.address.zipCode", "050001")
        .formParam("payee.phoneNumber", "3001234567")
        .formParam("payee.accountNumber", "123456789")
        .formParam("verifyAccount", "123456789")
        .formParam("amount", "50.00")
        .formParam("fromAccountId", "${accountId}")
        .check(
          status.in(200, 302),
          substring("Bill Payment Complete").or(substring("bill payment complete"))
        )
    )

  setUp(
    scn.inject(
      rampUsers(50) during (30.seconds) 
    )
  ).protocols(httpProtocol)
   .assertions(
     global.responseTime.mean.lte(3000),
     global.failedRequests.percent.lte(1)
   )
}
