name := "scala-websocket-chat"

version := "0.1"

scalaVersion := "2.12.8"

resolvers += Resolver.sonatypeRepo("snapshots")

val http4sVersion = "0.18.21"

libraryDependencies ++= Seq(
  "org.http4s" %% "http4s-dsl" % http4sVersion,
  "org.http4s" %% "http4s-blaze-server" % http4sVersion,
  "org.http4s" %% "http4s-blaze-client" % http4sVersion
)

scalacOptions ++= Seq("-Ypartial-unification")