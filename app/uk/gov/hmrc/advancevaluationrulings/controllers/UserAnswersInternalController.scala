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

import javax.inject.Inject

import scala.concurrent.ExecutionContext

import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.advancevaluationrulings.controllers.actions.IdentifierAction
import uk.gov.hmrc.advancevaluationrulings.models.{DraftId, UserAnswers}
import uk.gov.hmrc.advancevaluationrulings.models.application.DraftSummaryResponse
import uk.gov.hmrc.advancevaluationrulings.repositories.UserAnswersRepository
import uk.gov.hmrc.internalauth.client.{BackendAuthComponents, IAAction, Predicate, Resource, ResourceLocation, ResourceType}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

class UserAnswersInternalController @Inject() (
  cc: ControllerComponents,
  repository: UserAnswersRepository,
  auth: BackendAuthComponents
)(implicit ec: ExecutionContext)
    extends BackendController(cc) {

  private def predicate(action: IAAction) = Predicate.Permission(
    resource = Resource(
      resourceType = ResourceType("advance-valuation-rulings"),
      resourceLocation = ResourceLocation("user-answers")
    ),
    action = action
  )

  private def authorised(action: IAAction) = auth.authorizedAction(predicate(action))

  def get(draftId: DraftId): Action[AnyContent] = authorised(IAAction("READ")).async {
    implicit request =>
      repository
        .get(draftId)
        .map {
          _.map(answers => Ok(Json.toJson(answers)))
            .getOrElse(NotFound)
        }
  }

  def set(): Action[UserAnswers] = authorised(IAAction("WRITE"))(parse.json[UserAnswers]).async {
    implicit request =>
      repository
        .set(request.body)
        .map(_ => NoContent)
  }
}
