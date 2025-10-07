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
    "payAmount" -> (100 + util.Random.nextInt(900)).toString,
    "payeeName" -> s"Utility Company ${util.Random.nextInt(1000)}",
    "payeeAccount" -> s"ACC${100000 + util.Random.nextInt(899999)}"
  ))

  // Escenario principal de pago de servicios
  val scn = scenario("Pago de servicios con alta concurrencia")
    .feed(payFeeder)
    
    // 1. Login
    .exec(
      http("Login")
        .post("/login.htm")
        .formParam("username", username)
        .formParam("password", password)
        .check(status.is(200))
        .check(css("div#leftPanel").exists) // Verifica que el login fue exitoso
    )
    
    // 2. Navegar a la página de bill pay
    .exec(
      http("Navegar a Bill Pay")
        .get("/billpay.htm")
        .check(status.is(200))
        .check(css("input[name='accountId']", "value").find.saveAs("accountId"))
    )
    
    // 3. Realizar pago de servicio
    .exec(
      http("Realizar Pago de Servicio")
        .post("/billpay.htm")
        .formParam("input", "button")
        .formParam("name", "${payeeName}")
        .formParam("address.street", "123 Main St")
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
          css("h1.title, title")
            .find
            .transform(_.toLowerCase)
            .saveAs("pageTitle")
        )
    )
    
    // 4. Validación simple del resultado
    .exec { session =>
      val title = session("pageTitle").asOption[String].getOrElse("")
      if (title.contains("bill payment") || title.contains("complete")) {
        println(s"Pago exitoso: ${session("payeeName").as[String]}")
        session
      } else {
        println(s"Posible fallo en pago: ${session("payeeName").as[String]}")
        session
      }
    }

  // Configuración de la simulación
  setUp(
    scn.inject(
      rampUsers(200) during (30.seconds) // Exactamente como tu prueba exitosa
    )
  ).protocols(httpProtocol)
   .assertions(
     global.responseTime.mean.lte(3000),
     global.responseTime.percentile3.lte(3000),
     global.failedRequests.percent.lte(1)
   )
}

