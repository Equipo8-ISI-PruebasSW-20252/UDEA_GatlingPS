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
  // 18117: saldo negativo (emisor)
  // 18561: cuenta con fondos (receptor)
  val fromAccountId: String = "18561"   // cuenta con fondos disponibles
  val toAccountId: String = "18117"     // cuenta destino
  val amount: Int = 10                  // monto a transferir

  // 🔹 Listado de cuentas disponibles (opcional)
  val allAccounts: Seq[String] = Seq("18006", "18117", "18228", "18339", "18450")
}


