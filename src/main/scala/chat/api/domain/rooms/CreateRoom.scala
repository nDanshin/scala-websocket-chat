package chat.api.domain.rooms

import chat.api.domain.users.User

case class CreateRoom(creator: User.Id, name: String)
