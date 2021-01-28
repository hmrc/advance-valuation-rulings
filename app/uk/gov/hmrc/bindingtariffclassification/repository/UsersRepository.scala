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
import play.api.libs.json.Json.toJson
import play.api.libs.json._
import reactivemongo.api.indexes.Index
import reactivemongo.bson.BSONObjectID
import reactivemongo.play.json.collection.JSONCollection
import uk.gov.hmrc.bindingtariffclassification.model.MongoFormatters._
import uk.gov.hmrc.bindingtariffclassification.model._
import uk.gov.hmrc.mongo.ReactiveRepository

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@ImplementedBy(classOf[UsersMongoRepository])
trait UsersRepository {

  def insert(user: Operator): Future[Operator]

  def update(user: Operator, upsert: Boolean): Future[Option[Operator]]

  def getById(id: String): Future[Option[Operator]]

  def search(search: UserSearch,
             pagination: Pagination): Future[Paged[Operator]]
}

@Singleton
class UsersMongoRepository @Inject()(mongoDbProvider: MongoDbProvider)
    extends ReactiveRepository[Operator, BSONObjectID](
      collectionName = "users",
      mongo = mongoDbProvider.mongo,
      domainFormat = MongoFormatters.formatOperator
    )
    with UsersRepository
    with MongoCrudHelper[Operator] {

  override protected val mongoCollection: JSONCollection = collection

  override def indexes = Seq(
    createSingleFieldAscendingIndex(indexFieldKey = "id", isUnique = true),
    createSingleFieldAscendingIndex(indexFieldKey = "role", isUnique = false),
    createSingleFieldAscendingIndex(
      indexFieldKey = "memberOfTeams",
      isUnique = false
    ),
  )

  private val defaultSortBy = Json.obj("timestamp" -> -1)

  override def getById(id: String): Future[Option[Operator]] = {
    getOne(byId(id))
  }

  override def search(search: UserSearch,
                      pagination: Pagination): Future[Paged[Operator]] = {
    getMany(selector(search), defaultSortBy, pagination)
  }

  override def insert(user: Operator): Future[Operator] = createOne(user)

  override def update(user: Operator, upsert: Boolean): Future[Option[Operator]] = {
    updateDocument(selector = byId(user.id), update = user, upsert = upsert)
  }

  private def byId(id: String): JsObject =
    Json.obj("id" -> id)

  private def selector(search: UserSearch): JsObject = {
    val queries = Seq[JsObject]()
      .++(search.role.map(r => Json.obj("role" -> in(Set(r)))))
      .++(
        search.team.map(t => Json.obj("memberOfTeams" -> mappingNoneOrSome(t)))
      )

    queries match {
      case Nil           => Json.obj()
      case single :: Nil => single
      case many          => JsObject(Seq("$and" -> JsArray(many)))
    }
  }

  private def in[T](set: Set[T])(implicit fmt: Format[T]): JsValue =
    Json.obj("$in" -> JsArray(set.map(elm => Json.toJson(elm)).toSeq))

  private def mappingNoneOrSome: String => JsValue = {
    case "none" => Json.obj("$eq" -> JsArray.empty)
    case "some" => Json.obj("$gt" -> JsArray.empty)
    case v      => in(Set(v))
  }

}
