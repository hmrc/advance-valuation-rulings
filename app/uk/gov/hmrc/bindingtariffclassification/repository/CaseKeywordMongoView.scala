/*
 * Copyright 2022 HM Revenue & Customs
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

import org.mongodb.scala.MongoCollection
import org.mongodb.scala.bson.BsonDocument
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model.Accumulators.push
import org.mongodb.scala.model.Aggregates._
import org.mongodb.scala.model.Projections.include
import org.mongodb.scala.model.{Aggregates, Field, Projections}
import play.api.libs.json.Json
import uk.gov.hmrc.bindingtariffclassification.model.{CaseKeyword, MongoCodecs, Paged, Pagination}
import uk.gov.hmrc.bindingtariffclassification.repository.BaseMongoOperations.{countField, pagedResults}
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.Codecs

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}

@Singleton
class CaseKeywordMongoView @Inject()(mongoComponent: MongoComponent) {

  lazy val view: MongoCollection[CaseKeyword] = Await.result(awaitable = initView, atMost = 30.seconds)

  private def initView: Future[MongoCollection[CaseKeyword]] = {
    def getView: MongoCollection[CaseKeyword] = mongoComponent.database.getCollection[CaseKeyword]("caseKeywords")
      .withCodecRegistry(MongoCodecs.caseKeyword)

    getView
      .drop()
      .flatMap(_ => mongoComponent.database.createView("caseKeywords", "cases", pipeline))
      .toFuture()
      .map(_ => getView)
  }

  private val projectCaseHeader = Aggregates.project(
    Projections.fields(
      include(
        "reference",
        "status",
        "assignee",
        "team",
        "goodsName",
        "caseType",
        "keywords",
        "daysElapsed",
        "liabilityStatus"
      )
    )
  )

  private val addHeaderFields = addFields(
    Field("team", "$queueId"),
    Field("goodsName", "$application.goodName"),
    Field("caseType", "$application.type"),
    Field("liabilityStatus", "$application.status")
  )

  private val unwindKeywords = unwind("$keywords")
  private val group = Aggregates.group("$keywords", push("cases", "$$ROOT"))
  private val addKeywordField = addFields(Field("keyword.name", "$_id"))
  private val project = Aggregates.project(Codecs.toBson(Json.obj("_id" -> 0)).asDocument()) // todo fix this

  protected val pipeline: Seq[Bson] = {
    Seq(
      addHeaderFields,
      projectCaseHeader,
      unwindKeywords,
      group,
      addKeywordField,
      project
    )
  }

  def fetchKeywordsFromCases(pagination: Pagination): Future[Paged[CaseKeyword]] = {
    val runAggregation = view
      .aggregate[CaseKeyword] {
        Seq(
          Aggregates.project(Codecs.toBson(Json.obj("_id" -> 0)).asDocument()),
          skip((pagination.page - 1) * pagination.pageSize),
          limit(pagination.pageSize)
        )
      }
      .allowDiskUse(true)
      .toFuture()

    val futureCount = view
      .aggregate[BsonDocument] {
        Seq(count(countField))
      }
      .allowDiskUse(true)
      .headOption()

    pagedResults(futureCount, runAggregation, pagination)
  }
}
