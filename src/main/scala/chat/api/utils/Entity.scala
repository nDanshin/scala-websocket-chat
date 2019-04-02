package chat.api.utils

import supertagged.TaggedType

trait Entity[T] {
  object Id extends TaggedType[T]
  type Id = Id.Type
}
