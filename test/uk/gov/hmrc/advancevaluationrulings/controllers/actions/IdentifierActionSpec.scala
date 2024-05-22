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

package uk.gov.hmrc.advancevaluationrulings.controllers.actions

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.freespec.AnyFreeSpec
import org.scalatestplus.mockito.MockitoSugar
import play.api.Application
import play.api.libs.json.Json
import play.api.mvc.Results.Ok
import play.api.mvc.{Action, AnyContent, BodyParsers}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.advancevaluationrulings.base.SpecBase
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.~

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class IdentifierActionSpec extends AnyFreeSpec with SpecBase with MockitoSugar {

  class Harness(identify: IdentifierAction) {
    def onPageLoad(): Action[AnyContent] = identify { request =>
      Ok {
        Json.obj(
          "eori"           -> request.eori,
          "internalId"     -> request.internalId,
          "affinityGroup"  -> request.affinityGroup,
          "credentialRole" -> request.credentialRole
        )
      }
    }
  }

  private val app: Application = applicationBuilder.build()
  private val bodyParsers      = app.injector.instanceOf[BodyParsers.Default]

  private val mockAuthConnector: AuthConnector = mock[AuthConnector]

  "Identifier action" - {

    "when the user has the correct enrolment" - {

      "and all retrievals" - {

        "must execute the request" in {

          val atarEnrolment = Enrolments(
            Set(
              Enrolment(
                "HMRC-ATAR-ORG",
                Seq(EnrolmentIdentifier("EORINumber", "eori")),
                "Activated"
              )
            )
          )

          when(
            mockAuthConnector.authorise[Enrolments ~ Option[String] ~ Option[AffinityGroup]
              ~ Option[CredentialRole]](any(), any())(any(), any())
          )
            .thenReturn(
              Future.successful(
                new ~(
                  new ~(new ~(atarEnrolment, Some("internalId")), Some(AffinityGroup.Organisation)),
                  Some(Assistant)
                )
              )
            )

          val identifierAction = new IdentifierAction(mockAuthConnector, bodyParsers)
          val controller       = new Harness(identifierAction)
          val result           = controller.onPageLoad()(FakeRequest())

          status(result) mustBe OK

          val body = contentAsJson(result)
          (body \ "eori").as[String] mustEqual "eori"
          (body \ "internalId").as[String] mustEqual "internalId"
          (body \ "affinityGroup").as[AffinityGroup] mustEqual AffinityGroup.Organisation
          (body \ "credentialRole").asOpt[CredentialRole].value mustEqual Assistant
        }
      }

      "and no credential role" - {

        "must execute the request" in {

          val atarEnrolment = Enrolments(
            Set(
              Enrolment(
                "HMRC-ATAR-ORG",
                Seq(EnrolmentIdentifier("EORINumber", "eori")),
                "Activated"
              )
            )
          )

          when(
            mockAuthConnector.authorise[Enrolments ~ Option[String] ~ Option[AffinityGroup]
              ~ Option[CredentialRole]](any(), any())(any(), any())
          )
            .thenReturn(
              Future.successful(
                new ~(
                  new ~(new ~(atarEnrolment, Some("internalId")), Some(AffinityGroup.Individual)),
                  None
                )
              )
            )

          val identifierAction = new IdentifierAction(mockAuthConnector, bodyParsers)
          val controller       = new Harness(identifierAction)
          val result           = controller.onPageLoad()(FakeRequest())

          status(result) mustBe OK

          val body = contentAsJson(result)
          (body \ "eori").as[String] mustEqual "eori"
          (body \ "internalId").as[String] mustEqual "internalId"
          (body \ "affinityGroup").as[AffinityGroup] mustEqual AffinityGroup.Individual
          (body \ "credentialRole").asOpt[CredentialRole] mustBe None
        }
      }
    }

    "when the user does not have the correct enrolment" - {

      "must return Unauthorized" in {

        val otherEnrolment = Enrolments(
          Set(Enrolment("FOO", Seq(EnrolmentIdentifier("EORINumber", "eori")), "Activated"))
        )

        when(
          mockAuthConnector.authorise[Enrolments ~ Option[String] ~ Option[AffinityGroup]
            ~ Option[CredentialRole]](any(), any())(any(), any())
        )
          .thenReturn(
            Future.successful(
              new ~(
                new ~(new ~(otherEnrolment, Some("internalId")), Some(AffinityGroup.Individual)),
                None
              )
            )
          )

        val identifierAction = new IdentifierAction(mockAuthConnector, bodyParsers)
        val controller       = new Harness(identifierAction)
        val result           = controller.onPageLoad()(FakeRequest())

        status(result) mustBe UNAUTHORIZED
      }
    }

    "when the user does not have an affinity group" - {

      "must return Unauthorized" in {

        val atarEnrolment = Enrolments(
          Set(
            Enrolment("HMRC-ATAR-ORG", Seq(EnrolmentIdentifier("EORINumber", "eori")), "Activated")
          )
        )

        when(
          mockAuthConnector.authorise[Enrolments ~ Option[String] ~ Option[AffinityGroup]
            ~ Option[CredentialRole]](any(), any())(any(), any())
        )
          .thenReturn(
            Future.successful(new ~(new ~(new ~(atarEnrolment, Some("internalId")), None), None))
          )

        val identifierAction = new IdentifierAction(mockAuthConnector, bodyParsers)
        val controller       = new Harness(identifierAction)
        val result           = controller.onPageLoad()(FakeRequest())

        status(result) mustBe UNAUTHORIZED
      }
    }

    "when the user does not have an internal id" - {

      "must return Unauthorized" in {

        val atarEnrolment = Enrolments(
          Set(
            Enrolment("HMRC-ATAR-ORG", Seq(EnrolmentIdentifier("EORINumber", "eori")), "Activated")
          )
        )

        when(
          mockAuthConnector.authorise[Enrolments ~ Option[String] ~ Option[AffinityGroup]
            ~ Option[CredentialRole]](any(), any())(any(), any())
        )
          .thenReturn(
            Future.successful(
              new ~(new ~(new ~(atarEnrolment, None), Some(AffinityGroup.Individual)), None)
            )
          )

        val identifierAction = new IdentifierAction(mockAuthConnector, bodyParsers)
        val controller       = new Harness(identifierAction)
        val result           = controller.onPageLoad()(FakeRequest())

        status(result) mustBe UNAUTHORIZED
      }
    }

    "when the user has no enrolments" - {

      "must return Unauthorized" in {

        when(
          mockAuthConnector.authorise[Enrolments ~ Option[String] ~ Option[AffinityGroup]
            ~ Option[CredentialRole]](any(), any())(any(), any())
        )
          .thenReturn(
            Future.successful(
              new ~(
                new ~(
                  new ~(Enrolments(Set.empty), Some("internalId")),
                  Some(AffinityGroup.Individual)
                ),
                None
              )
            )
          )

        val identifierAction = new IdentifierAction(mockAuthConnector, bodyParsers)
        val controller       = new Harness(identifierAction)
        val result           = controller.onPageLoad()(FakeRequest())

        status(result) mustBe UNAUTHORIZED
      }
    }
  }
}
