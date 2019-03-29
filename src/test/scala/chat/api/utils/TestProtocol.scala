package chat.api.utils

import cats.effect.IO
import org.http4s.Response

trait TestProtocol {
  def testJsonProtocol(response: Response[IO], expectedJson: String): Unit = {
    val preparedExpectedJson = expectedJson.stripMargin.lines.map(_.trim).mkString
    val testJson = response.as[String].unsafeRunSync

    assert(
      assertion = testJson == preparedExpectedJson,
      message = "\nBeing tested Json does not match to expected Json." +
        "\nBeing tested Json string:\n" + testJson +
        "\nExpected Json string:\n" + preparedExpectedJson
    )
  }
}
