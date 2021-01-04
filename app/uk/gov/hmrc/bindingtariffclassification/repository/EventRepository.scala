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

import com.google.inject.ImplementedBy
import javax.inject.{Inject, Singleton}
import play.api.libs.json._
import reactivemongo.bson.BSONObjectID
import reactivemongo.play.json.ImplicitBSONHandlers._
import reactivemongo.play.json.collection.JSONCollection
import uk.gov.hmrc.bindingtariffclassification.model.MongoFormatters._
import uk.gov.hmrc.bindingtariffclassification.model._
import uk.gov.hmrc.mongo.ReactiveRepository

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@ImplementedBy(classOf[EventMongoRepository])
trait EventRepository {

  def insert(e: Event): Future[Event]

  def search(search: EventSearch, pagination: Pagination): Future[Paged[Event]]

  def deleteAll(): Future[Unit]

  def delete(search: EventSearch): Future[Unit]
}

@Singleton
class EventMongoRepository @Inject() (mongoDbProvider: MongoDbProvider)
    extends ReactiveRepository[Event, BSONObjectID](
      collectionName = "events",
      mongo          = mongoDbProvider.mongo,
      domainFormat   = MongoFormatters.formatEvent
    )
    with EventRepository
    with MongoCrudHelper[Event] {

  override val mongoCollection: JSONCollection = collection

  override def indexes = Seq(
    // TODO: We need to create an index (composed by a single or multiple fields) considering all possible searches needed by the UI.
    createSingleFieldAscendingIndex(indexFieldKey = "id", isUnique            = true),
    createSingleFieldAscendingIndex(indexFieldKey = "caseReference", isUnique = false),
    createSingleFieldAscendingIndex(indexFieldKey = "type", isUnique          = false)
  )

  override def insert(e: Event): Future[Event] =
    createOne(e)

  private val defaultSortBy = Json.obj("timestamp" -> -1)

  private def in[T](set: Set[T])(implicit fmt: Format[T]): JsValue =
    Json.obj("$in" -> JsArray(set.map(elm => Json.toJson(elm)).toSeq))

  override def search(search: EventSearch, pagination: Pagination): Future[Paged[Event]] =
    getMany(selector(search), defaultSortBy, pagination)

  override def deleteAll(): Future[Unit] =
    removeAll().map(_ => ())

  override def delete(search: EventSearch): Future[Unit] = {
    val delete = collection.delete()
    for {
      elems <- delete.element(q = selector(search), limit = None)
      _     <- delete.many(Seq(elems))
    } yield ()
  }

  private def selector(search: EventSearch): JsObject = {
    val queries = Seq[JsObject]()
      .++(search.caseReference.map(r => Json.obj("caseReference" -> in(r))))
      .++(search.`type`.map(t => Json.obj("details.type" -> in(t))))
      .++(search.timestampMin.map(t => Json.obj("timestamp" -> Json.obj("$gte" -> t))))
      .++(search.timestampMax.map(t => Json.obj("timestamp" -> Json.obj("$lte" -> t))))

    queries match {
      case Nil           => Json.obj()
      case single :: Nil => single
      case many          => JsObject(Seq("$and" -> JsArray(many)))
    }
  }

}
