/*
 * Copyright 2021 HM Revenue & Customs
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

package uk.gov.hmrc.bindingtariffclassification.repository

import javax.inject.{Inject, Singleton}

import com.google.inject.ImplementedBy
import reactivemongo.bson.BSONObjectID
import reactivemongo.core.errors.DatabaseException
import reactivemongo.play.json.collection.JSONCollection
import uk.gov.hmrc.bindingtariffclassification.model.MongoFormatters.formatJobRunEvent
import uk.gov.hmrc.bindingtariffclassification.model.{JobRunEvent, MongoFormatters}
import uk.gov.hmrc.mongo.ReactiveRepository

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.control.NonFatal

@ImplementedBy(classOf[SchedulerLockMongoRepository])
trait SchedulerLockRepository {

  def lock(e: JobRunEvent): Future[Boolean]

}

@Singleton
class SchedulerLockMongoRepository @Inject() (mongoDbProvider: MongoDbProvider)
    extends ReactiveRepository[JobRunEvent, BSONObjectID](
      collectionName = "scheduler",
      mongo          = mongoDbProvider.mongo,
      domainFormat   = MongoFormatters.formatJobRunEvent
    )
    with SchedulerLockRepository
    with MongoCrudHelper[JobRunEvent] {

  override val mongoCollection: JSONCollection = collection

  val mongoDuplicateKeyErrorCode: Int = 11000

  override def indexes = Seq(
    createCompoundIndex(Seq("name", "runDate"), isUnique = true)
  )

  override def lock(e: JobRunEvent): Future[Boolean] =
    createOne(e) map { _ =>
      logger.debug(s"Took Lock for [${e.name}] at [${e.runDate}]")
      true
    } recover {
      case error: DatabaseException if error.isNotAPrimaryError =>
        // Allow the scheduled job to proceed despite errors on secondary nodes
        logger.warn(s"Lock failed on secondary node", error)
        logger.debug(s"Took Lock for [${e.name}] at [${e.runDate}]")
        true
      case error: DatabaseException if error.code.contains(mongoDuplicateKeyErrorCode) =>
        logger.debug(s"Lock already exists for [${e.name}]", error)
        false
      case NonFatal(error) =>
        logger.error(s"Unable to take Lock for [${e.name}]", error)
        false
    }

}
