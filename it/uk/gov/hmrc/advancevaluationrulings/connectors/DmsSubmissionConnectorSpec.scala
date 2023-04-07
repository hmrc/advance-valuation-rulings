package uk.gov.hmrc.advancevaluationrulings.connectors

import akka.actor.ActorSystem
import akka.stream.scaladsl.Source
import akka.util.ByteString
import com.github.tomakehurst.wiremock.client.WireMock._
import org.mockito.MockitoSugar
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import play.api.Application
import play.api.http.Status.{ACCEPTED, INTERNAL_SERVER_ERROR}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.inject.bind
import play.api.libs.json.Json
import play.api.test.Helpers.{AUTHORIZATION, USER_AGENT}
import uk.gov.hmrc.advancevaluationrulings.models.application.{Attachment, Privacy}
import uk.gov.hmrc.advancevaluationrulings.utils.WireMockHelper
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.objectstore.client.Path
import uk.gov.hmrc.objectstore.client.RetentionPeriod.OneWeek
import uk.gov.hmrc.objectstore.client.play.test.stub
import uk.gov.hmrc.objectstore.client.config.ObjectStoreClientConfig
import uk.gov.hmrc.objectstore.client.play.PlayObjectStoreClient
import uk.gov.hmrc.objectstore.client.play.Implicits._

import java.time.{LocalDateTime, ZoneId}
import scala.concurrent.ExecutionContext.Implicits.global

class DmsSubmissionConnectorSpec
  extends AnyFreeSpec
    with WireMockHelper
    with MockitoSugar
    with ScalaFutures
    with Matchers
    with IntegrationPatience
    with BeforeAndAfterEach
    with BeforeAndAfterAll {

  implicit private lazy val as: ActorSystem = ActorSystem()

  override def beforeAll(): Unit = {
    super.beforeAll()
    startWireMock()
  }

  override def afterAll(): Unit = {
    stopWireMock()
    as.terminate().futureValue
    super.afterAll()
  }

  override def beforeEach(): Unit = {
    super.beforeEach()
    resetWireMock()
    objectStoreStub.deleteObject(Path.File("foo/bar.pdf")).futureValue
  }

  implicit private lazy val hc: HeaderCarrier = HeaderCarrier()

  private val baseUrl = "baseUrl"
  private val owner = "owner"
  private val token = "token"
  private val config = ObjectStoreClientConfig(baseUrl, owner, token, OneWeek)
  private val objectStoreStub = new stub.StubPlayObjectStoreClient(config)

  private lazy val app: Application =
    new GuiceApplicationBuilder()
      .overrides(
        bind[PlayObjectStoreClient].toInstance(objectStoreStub)
      )
      .configure(
        "microservice.services.dms-submission.port" -> wireMockServer.port,
        "microservice.services.dms-submission.callbackUrl" -> "http://localhost/callback",
        "microservice.services.dms-submission.store" -> "true",
        "microservice.services.dms-submission.source" -> "advance-ruling-service",
        "microservice.services.dms-submission.formId" -> "formId",
        "microservice.services.dms-submission.casKey" -> "casKey",
        "microservice.services.dms-submission.classificationType" -> "classificationType",
        "microservice.services.dms-submission.businessArea" -> "businessArea",
        "internal-auth.token" -> "authKey"
      )
      .build()

  private lazy val connector: DmsSubmissionConnector = app.injector.instanceOf[DmsSubmissionConnector]

  ".submitApplication" - {

    val source = Source.single(ByteString.fromString("Hello, World!"))

    val eori = "someEori"

    val submissionReference = "submissionReference"

    val timestamp = LocalDateTime.of(2022, 3, 2, 12, 30, 45)
      .atZone(ZoneId.of("UTC"))
      .toInstant

    val attachment = Attachment(
      id = 1,
      name = "attachment1",
      description = None,
      location = "foo/bar.pdf",
      privacy = Privacy.Public,
      mimeType = "application/pdf",
      size = 1337
    )

    "must return Done when the server returns ACCEPTED" in {

      objectStoreStub.putObject(
        path = Path.File("foo/bar.pdf"),
        content = Source.single(ByteString.fromString("Attachment 1")),
        contentType = Some("application/pdf")
      ).futureValue

      wireMockServer.stubFor(
        post(urlEqualTo("/dms-submission/submit"))
          .withHeader(AUTHORIZATION, equalTo("authKey"))
          .withHeader(USER_AGENT, equalTo("advance-valuation-rulings"))
          .withMultipartRequestBody(aMultipart().withName("submissionReference").withBody(equalTo("submissionReference")))
          .withMultipartRequestBody(aMultipart().withName("callbackUrl").withBody(equalTo("http://localhost/callback")))
          .withMultipartRequestBody(aMultipart().withName("metadata.source").withBody(equalTo("advance-ruling-service")))
          .withMultipartRequestBody(aMultipart().withName("metadata.timeOfReceipt").withBody(equalTo("2022-03-02T12:30:45")))
          .withMultipartRequestBody(aMultipart().withName("metadata.formId").withBody(equalTo("formId")))
          .withMultipartRequestBody(aMultipart().withName("metadata.customerId").withBody(equalTo("someEori")))
          .withMultipartRequestBody(aMultipart().withName("metadata.classificationType").withBody(equalTo("classificationType")))
          .withMultipartRequestBody(aMultipart().withName("metadata.businessArea").withBody(equalTo("businessArea")))
          .withMultipartRequestBody(aMultipart().withName("form")
            .withBody(equalTo("Hello, World!")))
          .withMultipartRequestBody(aMultipart().withName("attachment")
            .withBody(equalTo("Attachment 1"))
            .withHeader("Content-Disposition", containing("""filename="bar.pdf""""))
            .withHeader("Content-Type", equalTo("application/pdf")))
          .willReturn(
            aResponse()
              .withStatus(ACCEPTED)
              .withBody(Json.stringify(Json.obj("id" -> "foobar")))
          )
      )

      connector.submitApplication(eori, source, timestamp, submissionReference, Seq(attachment))(hc).futureValue
    }

    "must fail when an attachment cannot be found in object-store" in {

      wireMockServer.stubFor(
        post(urlEqualTo("/dms-submission/submit"))
          .willReturn(
            aResponse()
              .withStatus(ACCEPTED)
          )
      )

      val error = connector.submitApplication(eori, source, timestamp, submissionReference, Seq(attachment))(hc).failed.futureValue
      error mustBe an[DmsSubmissionConnector.AttachmentNotFoundException]

      error.asInstanceOf[DmsSubmissionConnector.AttachmentNotFoundException].file mustEqual "foo/bar.pdf"
    }

    "must fail when the server returns another status" in {

      wireMockServer.stubFor(
        post(urlEqualTo("/dms-submission/submit"))
          .willReturn(
            aResponse()
              .withStatus(INTERNAL_SERVER_ERROR)
          )
      )

      connector.submitApplication(eori, source, timestamp, submissionReference, Seq())(hc).failed.futureValue
    }
  }
}
