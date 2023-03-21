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

package testOnly

import com.google.inject.Inject
import model.MethodOne
import model.{BTIApplication, Case, CaseStatus, Contact, EORIDetails}
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import repository.CaseRepository
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import java.time.Instant
import java.util.UUID
import scala.concurrent.ExecutionContext

class TestOnlyController @Inject()(
                                  cc: ControllerComponents,
                                  caseRepository: CaseRepository,
                                  )(implicit executionContext: ExecutionContext) extends BackendController(cc){

  def createCase(): Action[AnyContent] = Action.async {
    _ =>
      val caseId = UUID.randomUUID().toString
      caseRepository.insert(Case(
        caseId,
        CaseStatus.NEW,
        Instant.now(),
        application = BTIApplication(
          EORIDetails("eori", "business-name", "line1", "line2", "line3", "postcode", "country"),
          Contact("Pan", "example@example.com", Some("0121 do 1")),
          goodName = "Thing",
          goodDescription = "a thing",
          requestedMethod = MethodOne(None, None, None)
        )
      )).map(
        _ => Ok(s"Added case: $caseId")
      )
  }

}
