package uk.gov.hmrc.advancevaluationrulings.connectors

import akka.stream.Materializer
import akka.stream.scaladsl.Sink
import akka.util.ByteString
import cats.implicits.toFoldableOps
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.http.Fault
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import play.api.Application
import play.api.http.Status.{NOT_FOUND, OK}
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.advancevaluationrulings.connectors.DraftAttachmentsConnector.DraftAttachmentsConnectorException
import uk.gov.hmrc.advancevaluationrulings.utils.WireMockHelper
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}

class DraftAttachmentsConnectorSpec
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
        "internal-auth.token"                                           -> "authKey"
      )
      .build()

  private lazy val connector: DraftAttachmentsConnector =
    app.injector.instanceOf[DraftAttachmentsConnector]
  private implicit lazy val mat: Materializer           = app.injector.instanceOf[Materializer]

  "get" - {

    "must return the contents of the file when the server responds" in {

      wireMockServer.stubFor(
        get(urlPathEqualTo("/attachments/foobar"))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withHeader("Content-Type", "application/pdf")
              .withHeader("Digest", "md5=grtBN0au5C+J3qK1lhT57w==")
              .withBody("Hello, World!")
          )
      )

      val result = connector.get("foobar").futureValue

      result.contentType mustEqual "application/pdf"
      result.contentMd5 mustEqual "grtBN0au5C+J3qK1lhT57w=="

      val content = result.content
        .runWith(Sink.reduce[ByteString](_ ++ _))
        .futureValue
        .decodeString("UTF-8")
      content mustEqual "Hello, World!"
    }

    "must fail when the server doesn't return a content-type header" in {

      wireMockServer.stubFor(
        get(urlPathEqualTo("/attachments/foobar"))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withHeader("Digest", "md5=contentMd5")
              .withBody("Hello, World!")
          )
      )

      val error = connector.get("foobar").failed.futureValue
      error mustBe an[DraftAttachmentsConnectorException]

      error
        .asInstanceOf[DraftAttachmentsConnectorException]
        .errors
        .toList must contain only "Content-Type header missing"
    }

    "must fail when the server doesn't return a digest header" in {

      wireMockServer.stubFor(
        get(urlPathEqualTo("/attachments/foobar"))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withHeader("Content-Type", "application/pdf")
              .withBody("Hello, World!")
          )
      )

      val error = connector.get("foobar").failed.futureValue
      error mustBe an[DraftAttachmentsConnectorException]

      error
        .asInstanceOf[DraftAttachmentsConnectorException]
        .errors
        .toList must contain only "Digest header missing"
    }

    "must fail when the server returns a digest which is not md5" in {

      wireMockServer.stubFor(
        get(urlPathEqualTo("/attachments/foobar"))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withHeader("Content-Type", "application/pdf")
              .withHeader("Digest", "sha1=contentMd5")
              .withBody("Hello, World!")
          )
      )

      val error = connector.get("foobar").failed.futureValue
      error mustBe an[DraftAttachmentsConnectorException]

      error
        .asInstanceOf[DraftAttachmentsConnectorException]
        .errors
        .toList must contain only "Digest algorithm must be md5"
    }

    "must fail when the server returns NOT_FOUND" in {

      wireMockServer.stubFor(
        get(urlPathEqualTo("/attachments/foobar"))
          .willReturn(
            aResponse()
              .withStatus(NOT_FOUND)
          )
      )

      val error = connector.get("foo/bar").failed.futureValue
      error mustBe an[UpstreamErrorResponse]
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
