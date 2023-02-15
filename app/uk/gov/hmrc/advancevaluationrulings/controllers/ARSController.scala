package uk.gov.hmrc.advancevaluationrulings.controllers

import play.api.libs.json.JsValue
import play.api.mvc.{Action, ControllerComponents}
import uk.gov.hmrc.advancevaluationrulings.models.traderdetails.{TraderDetailsRequest, TraderDetailsResponse}
import uk.gov.hmrc.advancevaluationrulings.services.TraderDetailsService
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton()
class ARSController @Inject() (
  cc: ControllerComponents,
  traderDetailsService: TraderDetailsService
) extends BackendController(cc) with BaseController {

  implicit val ec: ExecutionContext = cc.executionContext

  def retrieveTraderDetails(): Action[JsValue] = {
    // convert request from Json to TraderDetailsRequest
    // authenticate ? maybe
    // call service with TraderDetailsRequest and return TraderDetailsResponse

    Action.async(parse.json) { implicit request =>
      withCustomJsonBody[TraderDetailsRequest] { traderDetailsRequest =>
        inHttpResponse[TraderDetailsResponse](200) {
          import traderDetailsRequest._
          traderDetailsService.getTraderDetails(regime, acknowledgementReference, taxPayerID, EORI)
        }
      }
    }
  }

}
