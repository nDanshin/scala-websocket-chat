package chat.api.infrastructure.repository.inmemory

import cats.Applicative
import chat.api.domain.rooms.RoomRepositoryAlgebra

class RoomRepositoryInMemoryInterpreter[F[_]: Applicative] extends RoomRepositoryAlgebra[F] {

}
