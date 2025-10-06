package parabank

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._
import parabank.Data._

class TransferSim extends Simulation {

  // 1️Configuración HTTP
  val httpConf = http
    .baseUrl(url)
    .acceptHeader("application/json")
    .check(status.is(200))

  // 2️Feeder desde CSV
  // Archivo: src/test/resources/data/transferData.csv
  // Columnas: fromAccountId,toAccountId,amount
  val feeder = csv("src/test/resources/data/transferData.csv").circular

  // 3️Escenario de prueba: Transferencias simultáneas
  val scn = scenario("Escalabilidad en Transferencias Simultáneas")
    .feed(feeder)
    .exec(
      http("Solicitud de Transferencia")
        .post("/transfer")
        .queryParam("fromAccountId", "${fromAccountId}")
        .queryParam("toAccountId", "${toAccountId}")
        .queryParam("amount", "${amount}")
        .check(status.is(200))
    )

  // 4️Configuración de carga
  setUp(
    scn.inject(
      rampUsersPerSec(10).to(150).during(60.seconds),  // Escala progresivamente hasta 150 usuarios/seg
      constantUsersPerSec(150).during(120.seconds)     // Mantiene 150 usuarios/seg durante 2 minutos
    )
  )
  .protocols(httpConf)
  .assertions(
    global.successfulRequests.percent.gte(99),         // No más del 1% de errores
    global.responseTime.percentile3.lte(5000)          // 95% de respuestas < 5s
  )
}
