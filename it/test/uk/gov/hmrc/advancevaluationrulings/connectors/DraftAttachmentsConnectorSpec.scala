/*
 * Copyright 2024 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.advancevaluationrulings.connectors

import cats.data.NonEmptyChain
import org.apache.pekko.stream.Materializer
import org.apache.pekko.stream.scaladsl.Sink
import org.apache.pekko.util.ByteString
import cats.implicits.toFoldableOps
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.http.Fault
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import org.scalatest.PrivateMethodTester
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import play.api.Application
import play.api.http.Status.{NOT_FOUND, OK}
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.advancevaluationrulings.WireMockHelper
import uk.gov.hmrc.advancevaluationrulings.connectors.DraftAttachmentsConnector.DraftAttachmentsConnectorException
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}

import java.net.{MalformedURLException, URI, URL}
import scala.Left

class DraftAttachmentsConnectorSpec
    extends AnyFreeSpec
    with WireMockHelper
    with ScalaFutures
    with Matchers
    with IntegrationPatience
    with BeforeAndAfterEach
    with PrivateMethodTester
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

  private val invalidPath = "invalid path"

  private lazy val connector: DraftAttachmentsConnector =
    app.injector.instanceOf[DraftAttachmentsConnector]
  private implicit lazy val mat: Materializer           = app.injector.instanceOf[Materializer]
  private val getUrl                                    = PrivateMethod[Either[NonEmptyChain[String], URL]](Symbol("getUrl"))
  private val convertUriToUrl                           = PrivateMethod[Either[MalformedURLException, URL]](Symbol("convertUriToUrl"))

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

      error.asInstanceOf[DraftAttachmentsConnectorException].getMessage mustBe "Errors: Content-Type header missing"
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

    "must fail when URI contains an illegal character" in {

      wireMockServer.stubFor(
        get(urlPathEqualTo(""))
          .willReturn(
            aResponse()
              .withStatus(OK)
          )
      )

      val result = connector.invokePrivate(getUrl(invalidPath))
      result mustBe a[Left[_, _]]
      result.left.foreach(errors => errors.head should include("Illegal character in path"))
    }

    "must fail when URI to URL conversation fails with a MalformedURLException" in {

      wireMockServer.stubFor(
        get(urlPathEqualTo(""))
          .willReturn(
            aResponse()
              .withStatus(OK)
          )
      )

      val malformedUri = URI.create("tp://example.com")
      val result       = connector.invokePrivate(convertUriToUrl(malformedUri))
      result shouldBe a[Left[_, _]]
      result.left.foreach(error => error.getMessage should include("unknown protocol"))
    }
  }
}
