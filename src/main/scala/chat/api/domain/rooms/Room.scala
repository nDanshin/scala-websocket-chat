package chat.api.domain.rooms

import chat.api.domain.users.User
import chat.api.utils.Entity

case class Room(id: Room.Id,
                name: String,
                //roomType: RoomType,
                members: List[User.Id])

object Room extends Entity[Long]
