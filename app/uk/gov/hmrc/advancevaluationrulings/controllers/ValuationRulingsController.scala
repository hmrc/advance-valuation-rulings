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

import play.api.libs.json.{JsValue, Json}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import play.api.mvc.{Action, AnyContent, ControllerComponents, Results}
import uk.gov.hmrc.advancevaluationrulings.logging.RequestAwareLogger
import uk.gov.hmrc.advancevaluationrulings.models.ValuationRulingsApplication
import uk.gov.hmrc.advancevaluationrulings.models.common.{AcknowledgementReference, EoriNumber}
import uk.gov.hmrc.advancevaluationrulings.models.common.Envelope._
import uk.gov.hmrc.advancevaluationrulings.models.etmp.CDSEstablishmentAddress
import uk.gov.hmrc.advancevaluationrulings.models.traderdetails.TraderDetailsResponse
import uk.gov.hmrc.advancevaluationrulings.services.TraderDetailsService
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

@Singleton()
class ValuationRulingsController @Inject() (
  cc: ControllerComponents,
  traderDetailsService: TraderDetailsService
) extends BackendController(cc) {

  protected lazy val logger: RequestAwareLogger = new RequestAwareLogger(this.getClass)

  implicit val ec: ExecutionContext = cc.executionContext

  def retrieveTraderDetails(
    acknowledgementReference: String,
    eoriNumber: String
  ): Action[AnyContent] =
    Action.async {
      implicit request =>
//        traderDetailsService
//          .getTraderDetails(AcknowledgementReference(acknowledgementReference), eoriNumber = EoriNumber(eoriNumber))
//          .toResult

        val resp = TraderDetailsResponse(
          EORINo = eoriNumber,
          CDSFullName = "John Doe",
          CDSEstablishmentAddress = CDSEstablishmentAddress(
            streetAndNumber = "1 Test Street",
            city = "Cardiff",
            countryCode = "GB",
            postalCode = Option("CD11 123")
          )
        )

        Future.successful(Results.Status(200)(Json.toJson(resp)))
    }

  def submitAnswers(): Action[JsValue] =
    Action.async(parse.json) {
      implicit request =>
        extractFromJson[ValuationRulingsApplication] {
          userAnswers =>
            logger.warn(s"User answers: ${Json.prettyPrint(Json.toJson(userAnswers))}")
            Future.successful(Results.Status(200))
        }
    }
}
