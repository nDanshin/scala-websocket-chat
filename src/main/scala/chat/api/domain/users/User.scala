package chat.api.domain.users

import chat.api.utils.Entity

case class User(userName: String,
                firstName: String,
                lastName: String,
                email: String,
                hash: String,
                id: User.Id)

object User extends Entity[Long]
