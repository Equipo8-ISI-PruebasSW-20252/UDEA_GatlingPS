package parabank

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._
import parabank.Data._

class CargaSolicitudPrestamo extends Simulation {

  val httpProtocol = http
    .baseUrl(loanUrl)
    .acceptHeader("application/json, text/html")
    .inferHtmlResources() // 🔹 Captura recursos como cookies y sesione
    .check(status.not(500))

  // Escenario: login + solicitud de préstamo
  val solicitudPrestamo = scenario("Solicitud de Préstamo Bajo Carga")
    // 🔹 Paso 1: login (mantiene sesión)
    .exec(
      http("Login")
        .post("/login.htm")
        .formParam("username", username)
        .formParam("password", password)
        .check(status.is(200))
        .check(regex("Accounts Overview").exists) // asegura login exitoso
    )
    .pause(1.second)
    // 🔹 Paso 2: solicitud de préstamo (usa la sesión del login)
    .exec(
      http("POST Solicitud de Préstamo")
        .post("/requestloan.htm")
        .formParam("amount", amount * 50)          // monto total del préstamo
        .formParam("downPayment", amount * 10)     // pago inicial
        .formParam("fromAccountId", fromAccountId) // cuenta emisora válida
        .check(status.is(200))
        .check(regex("Loan Request Processed").exists) // confirma éxito
    )
    .pause(1.second)

  // 🔹 Configuración de carga
  setUp(
    solicitudPrestamo.inject(
      rampUsers(150) during (30.seconds)
    )
  )
  .protocols(httpProtocol)
  .assertions(
    global.responseTime.mean.lte(5000),        // promedio ≤ 5s
    global.successfulRequests.percent.gte(98), // ≥ 98% exitosos
    global.failedRequests.percent.lte(2)       // ≤ 2% fallidos
  )
}
