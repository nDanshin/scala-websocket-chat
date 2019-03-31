package chat.api.utils

import io.circe.Encoder
import supertagged.TaggedType

trait Entity[T] {
  object Id extends TaggedType[T]
  type Id = Id.Type

  //implicit val idEncoder: Encoder[Id] =
}
