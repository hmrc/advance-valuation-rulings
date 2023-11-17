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

package uk.gov.hmrc.advancevaluationrulings.controllers

import javax.inject.{Inject, Singleton}

import play.api.Logging
import play.api.mvc.{Action, ControllerComponents}
import uk.gov.hmrc.advancevaluationrulings.models.dms.{NotificationRequest, SubmissionItemStatus}
import uk.gov.hmrc.internalauth.client._
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendBaseController

@Singleton
class DmsSubmissionCallbackController @Inject() (
  override val controllerComponents: ControllerComponents,
  auth: BackendAuthComponents
) extends BackendBaseController
    with Logging {

  private val predicate = Predicate.Permission(
    resource = Resource(
      resourceType = ResourceType("advance-valuation-rulings"),
      resourceLocation = ResourceLocation("dms/callback")
    ),
    action = IAAction("WRITE")
  )

  private val authorised = auth.authorizedAction(predicate)

  def callback: Action[NotificationRequest] = authorised(parse.json[NotificationRequest]) { implicit request =>
    val notification = request.body

    if (notification.status == SubmissionItemStatus.Failed) {
      logger.error(
        s"[DmsSubmissionCallbackController][callback] DMS notification received for ${notification.id} failed with error: ${notification.failureReason
          .getOrElse("Error details not provided")}"
      )
    } else {
      logger.info(
        s"[DmsSubmissionCallbackController][callback] DMS notification received for ${notification.id} with status ${notification.status}"
      )
    }

    Ok
  }
}
