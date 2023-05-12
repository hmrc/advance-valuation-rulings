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

import java.time.{Clock, Instant}
import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Singleton}

import scala.concurrent.{ExecutionContext, Future}

import play.api.Logging
import play.api.libs.json._
import uk.gov.hmrc.advancevaluationrulings.config.AppConfig
import uk.gov.hmrc.advancevaluationrulings.models.Done
import uk.gov.hmrc.advancevaluationrulings.models.traderdetails.{CachedTraderDetails, TraderDetailsResponse}
import uk.gov.hmrc.crypto.{Decrypter, Encrypter}
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats

import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model._

@Singleton
class TraderDetailsRepository @Inject() (
  mongoComponent: MongoComponent,
  appConfig: AppConfig,
  clock: Clock
)(implicit ec: ExecutionContext, crypto: Encrypter with Decrypter)
    extends PlayMongoRepository[CachedTraderDetails](
      collectionName = "trader-details",
      mongoComponent = mongoComponent,
      domainFormat = CachedTraderDetails.encryptedFormat,
      indexes = Seq(
        IndexModel(
          Indexes.ascending("lastUpdated"),
          IndexOptions()
            .name("last-updated-index")
            .expireAfter(appConfig.traderDetailsTtlInSeconds, TimeUnit.SECONDS)
        ),
        IndexModel(
          Indexes.ascending("index"),
          IndexOptions()
            .name("id-index")
            .unique(true)
        )
      )
    )
    with Logging {

  implicit val instantFormat: Format[Instant] = MongoJavatimeFormats.instantFormat

  private def byEori(traderDetails: TraderDetailsResponse): Bson =
    Filters.eq("index", traderDetails.EORINo)

  private def byEori(eori: String): Bson =
    Filters.eq("index", eori)

  private def keepAlive(eori: String): Future[Done] =
    collection
      .updateOne(
        filter = byEori(eori),
        update = Updates.set("lastUpdated", Instant.now(clock))
      )
      .toFuture()
      .map(_ => Done)

  def get(eori: String): Future[Option[TraderDetailsResponse]] =
    for {
      _ <- keepAlive(eori)
      e <- collection.find(byEori(eori)).headOption()
    } yield {
      logger.debug("Retrieving value from cache")
      e.map(_.data)
    }

  def set(traderDetails: TraderDetailsResponse): Future[Done] = {
    logger.debug("Adding value to cache")
    val cacheValue = CachedTraderDetails(
      index = traderDetails.EORINo,
      data = traderDetails,
      lastUpdated = Instant.now(clock)
    )

    collection
      .replaceOne(
        filter = byEori(traderDetails),
        replacement = cacheValue,
        options = ReplaceOptions().upsert(true)
      )
      .toFuture()
      .map(_ => Done)
  }

  def clear(eori: String): Future[Done] =
    collection
      .deleteOne(byEori(eori))
      .toFuture()
      .map {
        _ =>
          logger.debug("Clearing value from cache")
          Done
      }

}
