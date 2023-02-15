/*
 * Copyright 2023 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.advancevaluationrulings.controllers

import play.api.libs.json._
import play.api.mvc.{Request, Result}
import uk.gov.hmrc.advancevaluationrulings.models.common.Envelope.Envelope
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

trait BaseController {
  def createResponse[T](successStatusCode: Int)(result: => Envelope[T])(
      implicit ec: ExecutionContext,
      hc:          HeaderCarrier,
      format:      Format[T]
    ): Future[Result] = ???

  def withCustomJsonBody[T](f: T => Future[Result])(
      implicit request: Request[JsValue],
      reads:            Reads[T]
    ): Future[Result] = ???
}
