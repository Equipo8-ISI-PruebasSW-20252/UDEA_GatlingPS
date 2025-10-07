package parabank

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._
import parabank.Data._

class PagoServiciosAltaConcurrencia extends Simulation {

  val httpProtocol = http
    .baseUrl(url)
    .acceptHeader("application/json, text/plain, */*")
    .contentTypeHeader("application/json;charset=UTF-8")
    .userAgentHeader("Gatling")
    .disableWarmUp

  // Feeder: datos del pago + monto aleatorio amplio para minimizar colisiones entre VUs
  val payFeeder = Iterator.continually {
    val amount = 10000 + util.Random.nextInt(90000) // 10000..99999 (reduce colisiones)
    Map(
      "payAmount" -> amount,
      "payeeName" -> "Utility Co",
      "payeeAddr" -> "Cra 123 #45-67",
      "payeeCity" -> "Medellin",
      "payeeState"-> "ANT",
      "payeeZip"  -> "050001",
      "payeePhone"-> "3000000000",
      "payeeAcct" -> "999999" // referencia del beneficiario
    )
  }

  val scn =
    scenario("Pago de servicios con alta concurrencia")
      .feed(payFeeder)

      // 1) Login
      .exec(
        http("Login")
          .get(s"/login/$username/$password")
          .check(status.is(200))
          .check(jsonPath("$.id").saveAs("customerId"))
      )

      // 2) Cuentas del cliente -> tomamos la primera cuenta
      .exec(
        http("Cuentas del cliente")
          .get("/customers/${customerId}/accounts")
          .check(status.is(200))
          .check(jsonPath("$[0].id").saveAs("accountId"))
      )

      // 3) Preconteo de transacciones con el mismo monto
      .exec(
        http("Preconteo transacciones por monto")
          .get("/accounts/${accountId}/transactions/amount/${payAmount}")
          .check(status.is(200))
          .check(jsonPath("$[*]").count.saveAs("preCount"))
      )

      // 4) Pagar servicio (Bill Pay)
      //    Endpoint: POST /billpay?accountId=...&amount=...
      //    Body JSON: datos del beneficiario (forma similar al billpay web)
      .exec(
        http("Bill Pay")
          .post("/billpay")
          .queryParam("accountId", "${accountId}")
          .queryParam("amount",    "${payAmount}")
          .body(StringBody(
            """{
              "name":        "${payeeName}",
              "address":     "${payeeAddr}",
              "city":        "${payeeCity}",
              "state":       "${payeeState}",
              "zipCode":     "${payeeZip}",
              "phoneNumber": "${payeePhone}",
              "accountNumber":"${payeeAcct}"
            }"""
          ))
          .check(status.is(200))
          // Cualquier respuesta JSON válida sirve; aseguramos que exista algún campo
          .check(bodyString.exists)
      )

      // 5) Postconteo y validación de NO-DUPLICACIÓN (exactamente +1)
      .exec(
        http("Postconteo transacciones por monto")
          .get("/accounts/${accountId}/transactions/amount/${payAmount}")
          .check(status.is(200))
          .check(jsonPath("$[*]").count.saveAs("postCount"))
      )
      .exec { session =>
        val pre  = session("preCount").as[Int]
        val post = session("postCount").as[Int]
        if (post != pre + 1) {
          // Marca el VU como fallo funcional (no se registró o hubo duplicación)
          session.markAsFailed
        } else session
      }

  // Inyección: 200 usuarios concurrentes
  setUp(
    scn.inject(
      rampUsers(200) during (30.seconds)
    )
  )
  .protocols(httpProtocol)
  .assertions(
    // Rendimiento: por transacción ≤ 3s (promedio y p95)
    global.responseTime.mean.lte(3000),
    global.responseTime.percentile3.lte(3000),
    // Confiabilidad: errores funcionales ≤ 1%
    global.successfulRequests.percent.gte(99),
    global.failedRequests.percent.lte(1)
  )
}
