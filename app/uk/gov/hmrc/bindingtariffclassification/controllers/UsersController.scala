package uk.gov.hmrc.bindingtariffclassification.controllers

import javax.inject.{Inject, Singleton}
import play.api.libs.json.{JsValue, Json}
import play.api.mvc._
import uk.gov.hmrc.bindingtariffclassification.config.AppConfig
import uk.gov.hmrc.bindingtariffclassification.model.ErrorCode.NOTFOUND
import uk.gov.hmrc.bindingtariffclassification.model.RESTFormatters._
import uk.gov.hmrc.bindingtariffclassification.model._
import uk.gov.hmrc.bindingtariffclassification.service.{CaseService, UsersService}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future.successful

@Singleton
class UsersController @Inject()(
                                 appConfig: AppConfig,
                                usersService: UsersService,
                                mcc: MessagesControllerComponents
                               )
    extends CommonController(mcc) {

  def fetchUserDetails(id: String): Action[AnyContent] =
    Action.async { implicit request =>
      ???
    }
}
/*
 *
 *
 *   def get(search: CaseSearch, pagination: Pagination): Action[AnyContent] = Action.async {
    caseService.get(search, pagination) map { cases =>
      Ok(Json.toJson(cases))
    } recover recovery
  }

  def getByReference(reference: String): Action[AnyContent] = Action.async {
    caseService.getByReference(reference) map handleNotFound recover recovery
  }
 *
 *
 *
 *
 * */
