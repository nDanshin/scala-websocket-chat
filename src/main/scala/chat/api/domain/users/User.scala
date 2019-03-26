package chat.api.domain.users

case class User(userName: String,
                firstName: String,
                lastName: String,
                email: String,
                hash: String,
                id: Option[Long])
