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

import uk.gov.hmrc.advancevaluationrulings.models.application.{Application, ApplicationId}
import org.mongodb.scala.model._
import uk.gov.hmrc.advancevaluationrulings.models.Done
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.{Codecs, PlayMongoRepository}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ApplicationRepository @Inject()(
                                       mongoComponent: MongoComponent
                                     )(implicit ec: ExecutionContext)
  extends PlayMongoRepository[Application](
    collectionName = "applications",
    mongoComponent = mongoComponent,
    domainFormat   = Application.format,
    indexes        = Seq(
      IndexModel(
        Indexes.ascending("id"),
        IndexOptions()
          .name("idIdx")
          .unique(true)
      )
    ),
    extraCodecs = Seq(Codecs.playFormatCodec(ApplicationId.format))
  ) {

  override lazy val requiresTtlIndex: Boolean = false

  def set(application: Application): Future[Done] =
    collection
      .insertOne(application)
      .toFuture()
      .map(_ => Done)

  def get(id: ApplicationId): Future[Option[Application]] =
    collection
      .find(Filters.eq("id", id))
      .toFuture()
      .map(_.headOption)
}
