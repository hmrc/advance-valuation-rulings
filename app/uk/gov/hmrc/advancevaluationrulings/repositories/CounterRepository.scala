/*
 * Copyright 2025 HM Revenue & Customs
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

package uk.gov.hmrc.advancevaluationrulings.repositories

import com.google.inject.ImplementedBy
import org.mongodb.scala.{MongoBulkWriteException, SingleObservableFuture}
import org.mongodb.scala.model.{Filters, FindOneAndUpdateOptions, ReturnDocument, Updates}
import uk.gov.hmrc.advancevaluationrulings.models.Done
import uk.gov.hmrc.advancevaluationrulings.models.application.{CounterId, CounterWrapper}
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository

import javax.inject.{Inject, Singleton}
import scala.annotation.nowarn
import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.CollectionConverters.*

@ImplementedBy(classOf[CounterMongoRepository])
trait CounterRepository {
  def ensureApplicationIdIsCorrect(): Future[Done]

  def seed: Future[Done]

  def nextId(id: CounterId): Future[Long]

}

@Singleton
class CounterMongoRepository @Inject() (
  mongoComponent: MongoComponent
)(implicit ec: ExecutionContext)
    extends PlayMongoRepository[CounterWrapper](
      collectionName = "counters",
      mongoComponent = mongoComponent,
      domainFormat = CounterWrapper.format,
      indexes = Nil
    )
    with CounterRepository {

  private val duplicateErrorCode  = 11000
  private def byId(id: CounterId) = Filters.eq("_id", id.toString)

  override lazy val requiresTtlIndex: Boolean = false

  val applicationStartingIndex = 2137409L

  private[repositories] val seeds: Seq[CounterWrapper] = Seq(
    CounterWrapper(CounterId.ApplicationId, applicationStartingIndex),
    CounterWrapper(CounterId.AttachmentId, 0)
  )

  @nowarn
  private val seedDatabase =
    seed // Eagerly call seed to ensure records are created on startup if needed

  override def ensureApplicationIdIsCorrect(): Future[Done] =
    collection
      .find(byId(CounterId.ApplicationId))
      .headOption()
      .flatMap(_.map { applicationId =>
        if (applicationId.index < applicationStartingIndex) {
          collection
            .findOneAndUpdate(
              filter = byId(CounterId.ApplicationId),
              update = Updates.set("index", applicationStartingIndex),
              options = FindOneAndUpdateOptions()
                .upsert(true)
                .bypassDocumentValidation(false)
            )
            .toFuture()
            .map(_ => Done)
        } else {
          Future.successful(Done)
        }
      }.getOrElse(Future.successful(Done)))

  override def seed: Future[Done] =
    collection
      .insertMany(seeds)
      .toFuture()
      .map(_ => Done)
      .recoverWith {
        case e: MongoBulkWriteException if e.getWriteErrors.asScala.forall(x => x.getCode == duplicateErrorCode) =>
          ensureApplicationIdIsCorrect()
      }

  override def nextId(id: CounterId): Future[Long] =
    collection
      .findOneAndUpdate(
        filter = byId(id),
        update = Updates.inc("index", 1),
        options = FindOneAndUpdateOptions()
          .upsert(true)
          .bypassDocumentValidation(false)
          .returnDocument(ReturnDocument.AFTER)
      )
      .toFuture()
      .map(_.index)
}
