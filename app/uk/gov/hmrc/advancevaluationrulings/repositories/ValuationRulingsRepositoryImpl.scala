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

package uk.gov.hmrc.advancevaluationrulings.repositories

import javax.inject.{Inject, Singleton}

import scala.concurrent.{ExecutionContext, Future}

import uk.gov.hmrc.advancevaluationrulings.models.ValuationRulingsApplication
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository

import org.mongodb.scala.model.{Filters, Indexes, IndexModel, IndexOptions}

@Singleton
class ValuationRulingsRepositoryImpl @Inject() (
  val mongoComponent: MongoComponent
)(implicit ec: ExecutionContext)
    extends PlayMongoRepository[ValuationRulingsApplication](
      mongoComponent,
      collectionName = "valuation-rulings-application",
      domainFormat = ValuationRulingsApplication.format,
      indexes = Seq(
        IndexModel(
          Indexes.descending("lastUpdated"),
          IndexOptions().name("lastUpdatedIdx")
        )
      )
    )
    with ValuationRulingsRepository {

  override def insert(application: ValuationRulingsApplication): Future[Boolean] =
    collection
      .insertOne(application)
      .toFuture()
      .map(_.wasAcknowledged())

  override def getItem(id: String): Future[Option[ValuationRulingsApplication]] =
    collection
      .find(Filters.equal("_id", id))
      .headOption()

}
