package chat.api.config

final case class ServerConfig(host: String, port: Int)
final case class ChatConfig(server: ServerConfig, db: DatabaseConfig)
