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
import play.api.libs.json._
import reactivemongo.bson.BSONObjectID
import reactivemongo.play.json.collection.JSONCollection
import uk.gov.hmrc.bindingtariffclassification.model.MongoFormatters._
import uk.gov.hmrc.bindingtariffclassification.model.{Keyword, MongoFormatters}
import uk.gov.hmrc.mongo.ReactiveRepository

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


@ImplementedBy(classOf[KeywordsMongoRepository])
trait KeywordsRepository {

  def insert(keyword: Keyword): Future[Keyword]

  def update(keyword: Keyword, upsert: Boolean): Future[Option[Keyword]]

  def delete(name: String): Future[Unit]

}

@Singleton
class KeywordsMongoRepository @Inject()(mongoDbProvider: MongoDbProvider)
  extends ReactiveRepository[Keyword, BSONObjectID](
    collectionName = "keywords",
    mongo = mongoDbProvider.mongo,
    domainFormat = MongoFormatters.formatKeywords
  )
    with KeywordsRepository
    with MongoCrudHelper[Keyword] {

  override protected val mongoCollection: JSONCollection = collection

  override def indexes = Seq(
    createSingleFieldAscendingIndex(indexFieldKey = "name", isUnique = true)
  )

  override def insert(keyword: Keyword): Future[Keyword] = createOne(keyword)

  override def update(keyword: Keyword,
                      upsert: Boolean): Future[Option[Keyword]] = {
    updateDocument(selector = byName(keyword.name), update = keyword, upsert = upsert)
  }

  private def byName(name: String): JsObject =
    Json.obj("name" -> name)

  override def delete(name: String): Future[Unit] =
    remove("name" -> name).map(_ => ())

}
