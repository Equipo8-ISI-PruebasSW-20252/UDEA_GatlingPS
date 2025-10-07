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

  // Feeder
  val payFeeder = Iterator.continually(Map(
    "payAmount" -> (10 + util.Random.nextInt(90)).toString,
    "payeeName" -> s"Utility-${1000 + util.Random.nextInt(9000)}",
    "payeeAccount" -> s"ACC${10000 + util.Random.nextInt(89999)}"
  ))

  val scn = scenario("Pago de servicios con alta concurrencia")
    .feed(payFeeder)
    
    // 1. Login directo
    .exec(
      http("Login")
        .post("/login.htm")
        .formParam("username", username)
        .formParam("password", password)
        .check(status.is(200))
        .check(css(".smallText").exists)
    )
    
    // 2. Obtener cuenta desde bill pay page
    .exec(
      http("Obtener cuenta para pago")
        .get("/billpay.htm")
        .check(status.is(200))
        .check(
          css("select[name='fromAccountId'] option", "value")
            .find
            .saveAs("accountId")
        )
    )
    
    // 3. Realizar pago
    .exec(
      http("Realizar Pago de Servicio")
        .post("/billpay.htm")
        .formParam("input", "button")
        .formParam("name", "${payeeName}")
        .formParam("address.street", "123 Main Street")
        .formParam("address.city", "Medellin")
        .formParam("address.state", "ANT")
        .formParam("address.zipCode", "050001")
        .formParam("phoneNumber", "3001234567")
        .formParam("accountNumber", "${payeeAccount}")
        .formParam("verifyAccount", "${payeeAccount}")
        .formParam("amount", "${payAmount}")
        .formParam("fromAccountId", "${accountId}")
        .check(status.is(200))
        .check(
          css("h1.title")
            .find
            .transform(_.toLowerCase)
            .saveAs("pageTitle")
        )
    )
    
    // 4. Validación simple
    .exec { session =>
      val title = session("pageTitle").asOption[String].getOrElse("")
      if (title.contains("bill payment complete")) {
        session
      } else {
        println(s"Título de página: $title")
        session
      }
    }

  setUp(
    scn.inject(
      rampUsers(200) during (30.seconds)
    )
  ).protocols(httpProtocol)
   .assertions(
     global.responseTime.mean.lte(3000),
     global.responseTime.percentile3.lte(3000),
     global.failedRequests.percent.lte(1)
   )
}
