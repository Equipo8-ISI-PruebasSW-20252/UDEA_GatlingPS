package parabank

object Data {

  // 🔹 URL base del servicio REST
  // Nota: Gatling añadirá las rutas (/login, /transfer, etc.)
  val url: String = "https://parabank.parasoft.com/parabank/services/bank"

    // 🔹 URL base del formulario web (para endpoints tipo .htm)
  val loanUrl: String = "https://parabank.parasoft.com/parabank"

  // 🔹 Credenciales de prueba
  val username: String = "albert"
  val password: String = "123"

  // 🔹 Cuentas disponibles para pruebas de transferencia
  // 14343: saldo negativo (emisor)
  // 14565: cuenta con fondos (receptor)
  val fromAccountId: String = "14565"   // cuenta con fondos disponibles
  val toAccountId: String = "14343"     // cuenta destino
  val amount: Int = 10                  // monto a transferir

  // 🔹 Listado de cuentas disponibles (opcional)
  val allAccounts: Seq[String] = Seq("14343", "14565", "26553", "26664", "26775")
}

