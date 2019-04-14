package chat.api.domain.messages

import chat.api.domain.rooms.Room
import chat.api.domain.users.User

case class CreateMessage(user: User.Id,
                         room: Room.Id,
                         content: String)
