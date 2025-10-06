package parabank

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._
import parabank.Data._

class LoginTest extends Simulation {

  // 1. HTTP Configuration
  val httpConf = http
    .baseUrl(url)
    .acceptHeader("application/json")
    .check(status.is(200))

  // 2. Scenario Definition
  val scn = scenario("Login - Tiempo de Respuesta")
    .exec(
      http("Solicitud de Login")
        .get(s"/login/$username/$password")
        .check(status.is(200))
    )

  // 3. Load Scenario
  setUp(
    scn.inject(
      // 🔹 Fase 1: Carga normal (hasta 100 usuarios)
      rampUsers(100).during(30.seconds),   // Incrementa de 0 a 100 usuarios en 30 segundos
      // 🔹 Fase 2: Carga pico (hasta 200 usuarios)
      rampUsers(100).during(20.seconds),   // Incrementa otros 100 (total 200) en 20 segundos
      constantUsersPerSec(200).during(30.seconds) // Mantiene 200 usuarios activos por 30 seg
    )
  ).protocols(httpConf)
   .assertions(
      // 💡 Validaciones de rendimiento
      global.responseTime.max.lte(5000),           // Bajo carga pico, máx 5 segundos
      global.responseTime.mean.lte(2000),          // En promedio (carga normal), máx 2 segundos
      global.successfulRequests.percent.gte(99)    // Al menos 99% de éxito
   )
}
