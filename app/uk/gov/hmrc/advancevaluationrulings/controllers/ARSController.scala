package uk.gov.hmrc.advancevaluationrulings.controllers

import javax.inject.{Inject, Singleton}

import scala.concurrent.ExecutionContext

import play.api.libs.json.JsValue
import play.api.mvc.{Action, ControllerComponents}
import uk.gov.hmrc.advancevaluationrulings.models.traderdetails.{TraderDetailsRequest, TraderDetailsResponse}
import uk.gov.hmrc.advancevaluationrulings.services.TraderDetailsService
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

@Singleton()
class ARSController @Inject() (
  cc: ControllerComponents,
  traderDetailsService: TraderDetailsService
) extends BackendController(cc) {

  implicit val ec: ExecutionContext = cc.executionContext

  def retrieveTraderDetails(): Action[JsValue] = {
    // convert request from Json to TraderDetailsRequest
    // authenticate ? maybe
    // call service with TraderDetailsRequest and return TraderDetailsResponse
  }

}
