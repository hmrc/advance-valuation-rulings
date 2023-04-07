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

import akka.stream.scaladsl.Source
import akka.util.ByteString
import cats.data.{EitherNec, EitherT, NonEmptyChain}
import cats.implicits._
import config.Service
import play.api.Configuration
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NoStackTrace
import DraftAttachmentsConnector._

@Singleton
class DraftAttachmentsConnector @Inject()(
                                       httpClient: HttpClientV2,
                                       configuration: Configuration
                                     )(implicit ec: ExecutionContext) {

  private val advanceValuationRulingsFrontend = configuration.get[Service]("microservice.services.advance-valuation-rulings-frontend")

  def get(path: String)(implicit hc: HeaderCarrier): Future[DraftAttachment] =
    httpClient.get(url"$advanceValuationRulingsFrontend/attachments/$path")
      .stream[HttpResponse].flatMap { response =>

        val result = (getContentType(response), getContentMd5(response)).parMapN {
          DraftAttachment(response.bodyAsSource, _, _)
        }

        result.fold(
          errors => Future.failed(DraftAttachmentsConnectorException(errors)),
          result => Future.successful(result)
        )
      }

  private def getContentType(response: HttpResponse): EitherNec[String, String] =
    response.header("Content-Type").toRightNec("Content-Type header missing")

  private def getContentMd5(response: HttpResponse): EitherNec[String, String] =
    response.header("Digest").toRightNec("Digest header missing").flatMap { digest =>
      val DigestPattern(alg, value) = digest
      if (alg == "md5") value.rightNec else "Digest algorithm must be md5".leftNec
    }

  private val DigestPattern = """^([^=]+)=(.+)$""".r
}

object DraftAttachmentsConnector {

  final case class DraftAttachmentsConnectorException(errors: NonEmptyChain[String]) extends Exception with NoStackTrace {
    override def getMessage: String = errors.toList.mkString("Errors: ", ", ", "")
  }
}

final case class DraftAttachment(content: Source[ByteString, _], contentType: String, contentMd5: String)