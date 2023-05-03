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

import play.api.libs.json.Format
import uk.gov.hmrc.advancevaluationrulings.config.AppConfig
import uk.gov.hmrc.advancevaluationrulings.models.{Done, DraftId, UserAnswers}
import uk.gov.hmrc.advancevaluationrulings.models.application.DraftSummary
import uk.gov.hmrc.crypto.{Decrypter, Encrypter}
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.{Codecs, PlayMongoRepository}
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats

import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model._

@Singleton
class UserAnswersRepository @Inject() (
  mongoComponent: MongoComponent,
  appConfig: AppConfig,
  clock: Clock
)(implicit ec: ExecutionContext, crypto: Encrypter with Decrypter)
    extends PlayMongoRepository[UserAnswers](
      collectionName = "user-answers",
      mongoComponent = mongoComponent,
      domainFormat = UserAnswers.encryptedFormat,
      indexes = Seq(
        IndexModel(
          Indexes.ascending("lastUpdated"),
          IndexOptions()
            .name("last-updated-index")
            .expireAfter(appConfig.userAnswersTtlInDays, TimeUnit.DAYS)
        ),
        IndexModel(
          Indexes.ascending("userId", "draftId"),
          IndexOptions()
            .name("id-index")
            .unique(true)
        ),
        IndexModel(
          Indexes.ascending("draftId"),
          IndexOptions()
            .name("draft-id-index")
            .unique(true)
        )
      ),
      extraCodecs = Seq(Codecs.playFormatCodec(DraftId.format))
    ) {

  implicit val instantFormat: Format[Instant] = MongoJavatimeFormats.instantFormat

  private def byUserIdAndDraftId(userId: String, draftId: DraftId): Bson =
    Filters.and(
      Filters.eq("userId", userId),
      Filters.eq("draftId", draftId)
    )

  def keepAlive(userId: String, draftId: DraftId): Future[Done] =
    collection
      .updateOne(
        filter = byUserIdAndDraftId(userId, draftId),
        update = Updates.set("lastUpdated", Instant.now(clock))
      )
      .toFuture()
      .map(_ => Done)

  def get(userId: String, draftId: DraftId): Future[Option[UserAnswers]] =
    keepAlive(userId, draftId).flatMap {
      _ =>
        collection
          .find(byUserIdAndDraftId(userId, draftId))
          .headOption()
    }

  def get(draftId: DraftId): Future[Option[UserAnswers]] =
    collection
      .find(Filters.eq("draftId", draftId))
      .headOption()

  def set(answers: UserAnswers): Future[Done] = {

    val updatedUserAnswers = answers copy (lastUpdated = Instant.now(clock))

    collection
      .replaceOne(
        filter = byUserIdAndDraftId(answers.userId, answers.draftId),
        replacement = updatedUserAnswers,
        options = ReplaceOptions().upsert(true)
      )
      .toFuture()
      .map(_ => Done)
  }

  def clear(userId: String, draftId: DraftId): Future[Done] =
    collection
      .deleteOne(byUserIdAndDraftId(userId, draftId))
      .toFuture()
      .map(_ => Done)

  def summaries(userId: String): Future[Seq[DraftSummary]] =
    collection
      .find(Filters.eq("userId", userId))
      .toFuture()
      .map(_.map(DraftSummary.apply))
}
