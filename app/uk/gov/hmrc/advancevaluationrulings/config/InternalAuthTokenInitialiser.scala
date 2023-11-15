/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.advancevaluationrulings.config

import akka.actor.ActorSystem
import config.Service
import play.api.http.Status.{CREATED, OK}
import play.api.libs.json.Json
import play.api.{Configuration, Logging}
import uk.gov.hmrc.advancevaluationrulings.models.Done
import uk.gov.hmrc.http.HttpReads.Implicits.readRaw
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, StringContextOps}

import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.concurrent.{Await, ExecutionContext, Future}

abstract class InternalAuthTokenInitialiser {
  val initialised: Future[Done]
}

@Singleton
class NoOpInternalAuthTokenInitialiser @Inject() () extends InternalAuthTokenInitialiser {
  override val initialised: Future[Done] = Future.successful(Done)
}

@Singleton
class InternalAuthTokenInitialiserImpl @Inject() (
  configuration: Configuration,
  httpClient: HttpClientV2,
  actorSystem: ActorSystem
)(implicit ec: ExecutionContext)
    extends InternalAuthTokenInitialiser
    with Logging {

  private val internalAuthService: Service =
    configuration.get[Service]("microservice.services.internal-auth")

  private val authToken: String =
    configuration.get[String]("internal-auth.token")

  private val appName: String =
    configuration.get[String]("appName")

  private val delay: FiniteDuration =
    configuration.get[FiniteDuration]("internal-auth-token-initialiser.delay")

  override lazy val initialised: Future[Done] =
    setup()

  actorSystem.scheduler.scheduleOnce(delay) {
    Await.result(setup(), 30.seconds)
  }

  private def setup(): Future[Done] = for {
    _ <- ensureAuthToken()
    _ <- addDmsSubmissionAttachmentGrants()
  } yield Done

  private def ensureAuthToken(): Future[Done] =
    authTokenIsValid.flatMap {
      isValid =>
        if (isValid) {
          logger.info("[InternalAuthTokenInitialiser][ensureAuthToken] Auth token is already valid")
          Future.successful(Done)
        } else {
          createClientAuthToken()
        }
    }

  private def createClientAuthToken(): Future[Done] = {
    logger.info("[InternalAuthTokenInitialiser][createClientAuthToken] Initialising auth token")
    httpClient
      .post(url"${internalAuthService.baseUrl}/test-only/token")(HeaderCarrier())
      .withBody(
        Json.obj(
          "token"       -> authToken,
          "principal"   -> appName,
          "permissions" -> Seq(
            Json.obj(
              "resourceType"     -> "dms-submission",
              "resourceLocation" -> "submit",
              "actions"          -> List("WRITE")
            ),
            Json.obj(
              "resourceType"     -> "object-store",
              "resourceLocation" -> "advance-valuation-rulings",
              "actions"          -> List("READ", "WRITE")
            )
          )
        )
      )
      .execute
      .flatMap {
        response =>
          if (response.status == CREATED) {
            logger.info("[InternalAuthTokenInitialiser][createClientAuthToken] Auth token initialised")
            Future.successful(Done)
          } else {
            logger.error("[InternalAuthTokenInitialiser][createClientAuthToken] Unable to initialise internal-auth token")
            Future.failed(new RuntimeException("Unable to initialise internal-auth token"))
          }
      }
  }

  private def addDmsSubmissionAttachmentGrants(): Future[Done] = {
    logger.info("[InternalAuthTokenInitialiser][addDmsSubmissionsAttachmentGrants] Initialising dms-submission grants")
    httpClient
      .post(url"${internalAuthService.baseUrl}/test-only/token")(HeaderCarrier())
      .withBody(
        Json.obj(
          "token"       -> UUID.randomUUID(),
          "principal"   -> "dms-submission",
          "permissions" -> Seq(
            Json.obj(
              "resourceType"     -> "advance-valuation-rulings",
              "resourceLocation" -> "dms/callback",
              "actions"          -> List("WRITE")
            )
          )
        )
      )
      .execute
      .flatMap {
        response =>
          if (response.status == CREATED) {
            logger.info("[InternalAuthTokenInitialiser][addDmsSubmissionAttachmentGrants] dms-submission grants added")
            Future.successful(Done)
          } else {
            logger.error("[InternalAuthTokenInitialiser][addDmsSubmissionAttachmentGrants] Unable to add dms-submission grants")
            Future.failed(new RuntimeException("Unable to add dms-submission grants"))
          }
      }
  }

  private def authTokenIsValid: Future[Boolean] = {
    logger.info("[InternalAuthTokenInitialiser][authTokenIsValid] Checking auth token")
    httpClient
      .get(url"${internalAuthService.baseUrl}/test-only/token")(HeaderCarrier())
      .setHeader("Authorization" -> authToken)
      .execute
      .map(_.status == OK)
  }
}
