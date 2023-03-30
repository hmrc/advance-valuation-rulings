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

import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.advancevaluationrulings.models.application.{Application, ApplicationId, ApplicationRequest, ApplicationSubmissionResponse, ApplicationSummaryResponse}
import uk.gov.hmrc.advancevaluationrulings.repositories.{ApplicationRepository, CounterRepository}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import java.time.{Clock, Instant}
import javax.inject.Inject

class ApplicationController @Inject()(
                                       cc: ControllerComponents,
                                       clock: Clock
                                     ) extends BackendController(cc) {

  def submit: Action[ApplicationRequest] = Action(parse.json[ApplicationRequest]) {
    implicit request =>
      Ok(Json.toJson(ApplicationSubmissionResponse(ApplicationId(1))))
  }

  def summaries: Action[AnyContent] = Action {
    implicit request =>
      Ok(Json.toJson(ApplicationSummaryResponse(Nil)))
  }

  def get(applicationId: ApplicationId): Action[AnyContent] = Action {
    implicit request =>
      val application = Application(applicationId, Instant.now(clock), Instant.now(clock))

      Ok(Json.toJson(application))
  }
}
