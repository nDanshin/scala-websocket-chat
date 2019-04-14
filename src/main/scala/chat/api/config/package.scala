package chat.api

import io.circe.Decoder
import io.circe.generic.semiauto._

package object config {
  implicit val serverConfigDecoder: Decoder[ServerConfig] = deriveDecoder
  //implicit val dbconnDec: Decoder[DatabaseConnectionsConfig] = deriveDecoder
  implicit val dbConfigDecoder: Decoder[DatabaseConfig] = deriveDecoder
  implicit val chatConfigDecoder: Decoder[ChatConfig] = deriveDecoder
}
