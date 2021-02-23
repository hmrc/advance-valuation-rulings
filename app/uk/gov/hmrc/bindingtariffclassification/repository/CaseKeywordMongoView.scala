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
import play.api.libs.json.OFormat.oFormatFromReadsAndOWrites
import play.api.libs.json.{JsObject, JsString, Json}
import reactivemongo.bson.BSONObjectID
import reactivemongo.play.json.commands.JSONAggregationFramework
import reactivemongo.play.json.commands.JSONAggregationFramework._
import uk.gov.hmrc.bindingtariffclassification.model.MongoFormatters._
import uk.gov.hmrc.bindingtariffclassification.model.{CaseKeyword, Paged, Pagination}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@ImplementedBy(classOf[CaseKeywordMongoView])
trait CaseKeywordView {
  def fetchKeywordsFromCases(pagination: Pagination): Future[Paged[CaseKeyword]]
}

@Singleton
class CaseKeywordMongoView @Inject() (
  mongoDbProvider: MongoDbProvider
) extends ReactiveView[CaseKeyword, BSONObjectID](
      viewName       = "caseKeywords",
      collectionName = "cases",
      mongo          = mongoDbProvider.mongo
    )
    with CaseKeywordView {

  override protected val pipeline: Seq[JSONAggregationFramework.PipelineOperator] = {
    val addHeaderFields = AddFields(
      Json.obj("team" -> "$queueId", "goodsName" -> "$application.goodName", "caseType" -> "$application.type")
    )
    val projectCaseHeader = Project(
      Json.obj(
        "reference" -> 1,
        "status"    -> 1,
        "assignee"  -> 1,
        "team"      -> 1,
        "goodsName" -> 1,
        "caseType"  -> 1,
        "keywords"  -> 1
      )
    )
    val unwindKeywords  = UnwindField("keywords")
    val group           = Group(JsString("$keywords"))("cases" -> PushField("$ROOT"))
    val addKeywordField = AddFields(Json.obj("keyword.name" -> "$_id"))
    val project         = Project(Json.obj("_id" -> 0))

    Seq(
      addHeaderFields,
      projectCaseHeader,
      unwindKeywords,
      group,
      addKeywordField,
      project
    )
  }

  override def fetchKeywordsFromCases(pagination: Pagination): Future[Paged[CaseKeyword]] = {
    val runAggregation = view
      .aggregateWith[CaseKeyword](allowDiskUse = true)(_ =>
        (
          Project(Json.obj("_id" -> 0)),
          List(Skip((pagination.page - 1) * pagination.pageSize), Limit(pagination.pageSize))
        )
      )
      .collect[List](Int.MaxValue, reactivemongo.api.Cursor.FailOnError())

    val runCount = view
      .aggregateWith[JsObject](allowDiskUse = true)(_ => (Count("resultCount"), Nil))
      .head

    for {
      results <- runAggregation
      count   <- runCount
    } yield Paged(results, pagination, count("resultCount").as[Int])
  }
}
