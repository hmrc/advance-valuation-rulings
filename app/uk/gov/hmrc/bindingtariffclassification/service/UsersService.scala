package uk.gov.hmrc.bindingtariffclassification.service

import java.time.Instant
import java.util.UUID

import javax.inject._
import akka.stream.Materializer
import akka.stream.scaladsl.Sink
import uk.gov.hmrc.bindingtariffclassification.config.AppConfig
import uk.gov.hmrc.bindingtariffclassification.model._
import uk.gov.hmrc.bindingtariffclassification.repository._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UsersService @Inject()(
  appConfig: AppConfig,
  usersRepository: UsersRepository,
)(implicit mat: Materializer) {

  def getById(id: String): Future[Option[Operator]] =
    usersRepository.getById(id)
}
