package parabank

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._
import parabank.Data._

class CargaEstadosCuenta extends Simulation {

  // Configuración base del escenario HTTP
  val httpProtocol = http
    .baseUrl(url)
    .acceptHeader("application/json")
    .check(status.is(200))

  // 📋 Escenario: consulta de estado de cuenta
  val consultaEstadoCuenta = scenario("Consulta Estado de Cuenta")
    .exec(
      http("GET Estado de Cuenta")
        .get(s"/login/$username/$password") // endpoint 
        .check(status.is(200))                // asegura que la respuesta sea 200 OK
    )

  // Configuración de usuarios y carga
  setUp(
    consultaEstadoCuenta.inject(
      rampUsers(200) during (30.seconds) // simula 200 usuarios entrando gradualmente en 30s
    )
  )
  .protocols(httpProtocol)
  //.assertions(
    //global.responseTime.max.lte(3000),     // tiempo de respuesta máximo ≤ 3 segundos
    //global.failedRequests.percent.lte(1)   // tasa de error ≤ 1%
  //)
}

