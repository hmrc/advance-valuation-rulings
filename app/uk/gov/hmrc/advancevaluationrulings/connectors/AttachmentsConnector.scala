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

package uk.gov.hmrc.advancevaluationrulings.connectors

import akka.stream.Materializer
import akka.stream.scaladsl.Source
import akka.util.ByteString
import config.Service
import play.api.Configuration
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}
import uk.gov.hmrc.http.client.HttpClientV2

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AttachmentsConnector @Inject() (
                                       httpClient: HttpClientV2,
                                       configuration: Configuration
                                     )(implicit ec: ExecutionContext, mat: Materializer) {

  private val advanceValuationRulingsFrontend = configuration.get[Service]("microservice.services.advance-valuation-rulings-frontend")

  def get(path: String)(implicit hc: HeaderCarrier): Future[Source[ByteString, _]] =
    httpClient.get(url"$advanceValuationRulingsFrontend/attachments/$path")
      .stream[Source[ByteString, _]]
}
