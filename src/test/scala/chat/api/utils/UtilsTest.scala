package chat.api.utils

import org.scalatest.{FlatSpec, Matchers}
import io.circe.{Encoder, Json}
import io.circe.generic.semiauto._
import io.circe.syntax._

class UtilsTest extends FlatSpec with Matchers {

  case class A(id: A.Id)

  object A extends Entity[String] {
    implicit val idEncoder: Encoder[A.Id] = Encoder.encodeString.contramap[A.Id](_.toString)
  }

  implicit val aEncoder: Encoder[A] = deriveEncoder

  "entity" should "encode entity class instance" in {
    val a = new A(id = A.Id @@ "123")
    a.asJson shouldBe Json.fromFields(Seq(
      ("id", Json.fromString("123"))
    ))
  }

}
