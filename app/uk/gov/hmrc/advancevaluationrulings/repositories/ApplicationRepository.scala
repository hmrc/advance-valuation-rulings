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

import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Singleton}

import scala.concurrent.{ExecutionContext, Future}

import uk.gov.hmrc.advancevaluationrulings.config.AppConfig
import uk.gov.hmrc.advancevaluationrulings.models.Done
import uk.gov.hmrc.advancevaluationrulings.models.application.{Application, ApplicationId, ApplicationSummary}
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.{Codecs, PlayMongoRepository}
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats

import org.mongodb.scala.model._

@Singleton
class ApplicationRepository @Inject() (
  mongoComponent: MongoComponent,
  appConfig: AppConfig
)(implicit ec: ExecutionContext)
    extends PlayMongoRepository[Application](
      collectionName = "applications",
      mongoComponent = mongoComponent,
      domainFormat = Application.format(MongoJavatimeFormats.instantFormat),
      indexes = Seq(
        IndexModel(
          Indexes.ascending("lastUpdated"),
          IndexOptions()
            .name("last-updated-index")
            .expireAfter(appConfig.applicationTtlInDays, TimeUnit.DAYS)
        ),
        IndexModel(
          Indexes.ascending("id"),
          IndexOptions()
            .name("idIdx")
            .unique(true)
        ),
        IndexModel(
          Indexes.ascending("applicantEori"),
          IndexOptions()
            .name("applicantEoriIdx")
            .unique(false)
        )
      ),
      extraCodecs = Seq(
        Codecs.playFormatCodec(ApplicationId.format),
        Codecs.playFormatCodec(ApplicationSummary.mongoFormat)
      )
    ) {

  override lazy val requiresTtlIndex: Boolean = false

  def set(application: Application): Future[Done] =
    collection
      .insertOne(application)
      .toFuture()
      .map(_ => Done)

  def get(id: ApplicationId, applicantEori: String): Future[Option[Application]] = {

    val filter = Filters.and(
      Filters.eq("id", id),
      Filters.eq("applicantEori", applicantEori)
    )

    collection
      .find(filter)
      .toFuture()
      .map(_.headOption)
  }

  def summaries(eori: String): Future[Seq[ApplicationSummary]] =
    collection
      .find[ApplicationSummary](Filters.eq("applicantEori", eori))
      .toFuture()
}
