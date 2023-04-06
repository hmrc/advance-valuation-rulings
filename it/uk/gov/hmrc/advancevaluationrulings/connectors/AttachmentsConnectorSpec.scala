package uk.gov.hmrc.advancevaluationrulings.connectors

import akka.stream.Materializer
import akka.stream.scaladsl.Sink
import akka.util.ByteString
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.http.Fault
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.Application
import play.api.http.Status.{NOT_FOUND, OK}
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.advancevaluationrulings.utils.WireMockHelper
import uk.gov.hmrc.http.HeaderCarrier

class AttachmentsConnectorSpec
  extends AnyFreeSpec
    with WireMockHelper
    with ScalaFutures
    with Matchers
    with IntegrationPatience
    with BeforeAndAfterEach
    with BeforeAndAfterAll {

  override def beforeAll(): Unit = {
    super.beforeAll()
    startWireMock()
  }

  override def afterAll(): Unit = {
    stopWireMock()
    super.afterAll()
  }

  override def beforeEach(): Unit = {
    super.beforeEach()
    resetWireMock()
  }

  implicit private lazy val hc: HeaderCarrier = HeaderCarrier()

  private lazy val app: Application =
    GuiceApplicationBuilder()
      .configure(
        "microservice.services.advance-valuation-rulings-frontend.port" -> wireMockServer.port,
        "internal-auth.token" -> "authKey"
      )
      .build()

  private lazy val connector: AttachmentsConnector = app.injector.instanceOf[AttachmentsConnector]
  private implicit lazy val mat: Materializer = app.injector.instanceOf[Materializer]

  "get" - {

    "must return the contents of the file when the server responds" in {

      wireMockServer.stubFor(
        get(urlPathEqualTo("/attachments/foobar"))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody("Hello, World!")
          )
      )

      val result = connector.get("foobar").futureValue
        .runWith(Sink.reduce[ByteString](_ ++ _)).futureValue
        .decodeString("UTF-8")

      result mustEqual "Hello, World!"
    }

    "must fail when the server returns NOT_FOUND" in {

      wireMockServer.stubFor(
        get(urlPathEqualTo("/attachments/foobar"))
          .willReturn(
            aResponse()
              .withStatus(NOT_FOUND)
          )
      )

      connector.get("foo/bar").failed.futureValue
    }

    "must fail when the server fails" in {

      wireMockServer.stubFor(
        get(urlPathEqualTo("/attachments/foobar"))
          .willReturn(
            aResponse()
              .withFault(Fault.CONNECTION_RESET_BY_PEER)
          )
      )

      connector.get("foo/bar").failed.futureValue
    }
  }
}
