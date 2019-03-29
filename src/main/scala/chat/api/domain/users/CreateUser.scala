package chat.api.domain.users

case class CreateUser(userName: String,
                      firstName: String,
                      lastName: String,
                      email: String,
                      hash: String)
