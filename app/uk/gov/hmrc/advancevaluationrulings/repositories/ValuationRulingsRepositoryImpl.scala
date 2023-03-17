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
import uk.gov.hmrc.advancevaluationrulings.models.{Application, ValuationRulingsApplication}
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import org.mongodb.scala.model.{Filters, IndexModel, IndexOptions, Indexes}

@Singleton
class ValuationRulingsRepositoryImpl @Inject() (
  val mongoComponent: MongoComponent
)(implicit ec: ExecutionContext)
    extends PlayMongoRepository[Application](
      mongoComponent,
      collectionName = "valuation-rulings-application",
      domainFormat = Application.format,
      indexes = Seq(
        IndexModel(
          Indexes.ascending("applicationNumber"),
          IndexOptions().name("applicationNumberIdx").unique(true)
        ),
        IndexModel(
          Indexes.descending("lastUpdated"),
          IndexOptions().name("lastUpdatedIdx")
        ),
        IndexModel(
          Indexes.ascending("applicantholder.eori"),
          IndexOptions().name("eoriNumberIdx")
        )
      )
    )
    with ValuationRulingsRepository {

  override def insert(application: Application): Future[Boolean] =
    collection
      .insertOne(application)
      .toFuture()
      .map(_.wasAcknowledged())

  override def getItems(eoriNumber: String): Future[Seq[Application]] =
    collection
      .find(Filters.equal("data.checkRegisteredDetails.eori", eoriNumber))
      .toFuture()

}
