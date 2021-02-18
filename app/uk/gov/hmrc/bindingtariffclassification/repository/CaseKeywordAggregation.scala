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

import javax.inject.{Inject, Singleton}
import com.google.inject.ImplementedBy
import play.api.libs.json.Json.toJson
import play.api.libs.json.{JsArray, JsNull, JsObject, JsString, Json}
import reactivemongo.bson.{BSONDocument, BSONObjectID}
import reactivemongo.core.commands.{Group, SumField}
import reactivemongo.play.json.ImplicitBSONHandlers._
import reactivemongo.play.json.collection.JSONCollection
import reactivemongo.play.json.commands.JSONAggregationFramework
import reactivemongo.play.json.commands.JSONAggregationFramework.{Match, Project, ReplaceRoot, Unwind}
import uk.gov.hmrc.bindingtariffclassification.model.{Attachment, Case, CaseKeyword, Keyword}
import uk.gov.hmrc.bindingtariffclassification.model.MongoFormatters.formatCaseKeyword
import reactivemongo.play.json.commands.JSONAggregationFramework._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@ImplementedBy(classOf[CaseKeywordAggregation])
trait CaseKeywordView {
  def fetchKeywordsFromCases: Future[List[CaseKeyword]]
}

@Singleton
class CaseKeywordAggregation @Inject() (
                                         mongoDbProvider: MongoDbProvider
                                       ) extends ReactiveView[Attachment, BSONObjectID](
  viewName       = "caseKeywords",
  collectionName = "cases",
  mongo          = mongoDbProvider.mongo
)with CaseKeywordView {

  private val keywordsArrayField: Set[String] = Set(
    "keywords"
  )

  override protected val pipeline: Seq[JSONAggregationFramework.PipelineOperator] = {
    val unwindNestedKeywordsArrays = keywordsArrayField.toSeq.map(name => Unwind(name, None, Some(true)))

    val projectKeywords = Project(
      Json.obj(
        "keyword" -> keywordsArrayField.map(name => "$" + name)
      )
    )
    val unwindKeywords = Unwind("keyword", None, Some(false))
    val filterNotNull = Match(Json.obj("keyword" -> Json.obj("$ne" -> JsNull)))
    val toRoot = ReplaceRoot(Json.obj("$mergeObjects" -> List("$keyword")))

    unwindNestedKeywordsArrays ++ Seq(
      projectKeywords,
      unwindKeywords,
      filterNotNull,
      toRoot
    )
  }

  override def fetchKeywordsFromCases: Future[List[CaseKeyword]] =
    mongoDbProvider
      .mongo()
      .collection[JSONCollection]("cases")
      .aggregateWith[JsObject]()(_ => _)
      .collect[List](Int.MaxValue, reactivemongo.api.Cursor.FailOnError())
}
