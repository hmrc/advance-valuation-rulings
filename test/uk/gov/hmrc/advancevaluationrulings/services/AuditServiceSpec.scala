package uk.gov.hmrc.advancevaluationrulings.services

import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchersSugar.eqTo
import org.mockito.MockitoSugar
import org.scalatest.BeforeAndAfterEach
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.advancevaluationrulings.models.application.{Application, ApplicationId, ContactDetails, GoodsDetails, MethodOne, TraderDetail}
import uk.gov.hmrc.advancevaluationrulings.models.audit.ApplicationSubmissionEvent
import uk.gov.hmrc.auth.core.{AffinityGroup, Assistant}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector

import java.time.Instant
import java.time.temporal.ChronoUnit

class AuditServiceSpec extends AnyFreeSpec with Matchers with MockitoSugar with BeforeAndAfterEach {

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockAuditConnector)
  }

  private val mockAuditConnector = mock[AuditConnector]

  private lazy val app = GuiceApplicationBuilder()
    .overrides(
      bind[AuditConnector].toInstance(mockAuditConnector)
    ).build()

  private lazy val service: AuditService = app.injector.instanceOf[AuditService]

  private val hc: HeaderCarrier = HeaderCarrier()

  private val trader = TraderDetail("eori", "name", "line1", None, None, "postcode", "GB", None)
  private val goodsDetails = GoodsDetails("name", "description", None, None, None)
  private val method = MethodOne(None, None, None)
  private val contact = ContactDetails("name", "email", None)
  private val now = Instant.now.truncatedTo(ChronoUnit.MILLIS)

  private val application = Application(
    id = ApplicationId(1),
    applicantEori = "applicantEori",
    trader = trader,
    agent = None,
    contact = contact,
    goodsDetails = goodsDetails,
    requestedMethod = method,
    attachments = Nil,
    submissionReference = "submissionReference",
    created = now,
    lastUpdated = now
  )

  "auditSubmitRequest" - {

    "must call the audit connector with the right values" in {

      val event = ApplicationSubmissionEvent(
        internalId = "internalId",
        affinityGroup = AffinityGroup.Organisation,
        credentialRole = Some(Assistant),
        application = application
      )

      service.auditSubmitRequest(event)(hc)

      verify(mockAuditConnector).sendExplicitAudit(eqTo("ApplicationSubmissionEvent"), eqTo(event))(eqTo(hc), any(), any())
    }
  }
}
