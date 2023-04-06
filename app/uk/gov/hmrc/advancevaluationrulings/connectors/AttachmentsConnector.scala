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
