/*
 * Copyright 2019 HM Revenue & Customs
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

import javax.inject.Singleton
import play.api.libs.json.Json.JsValueWrapper
import play.api.libs.json._
import uk.gov.hmrc.bindingtariffclassification.model.CaseStatus.CaseStatus
import uk.gov.hmrc.bindingtariffclassification.model.MongoFormatters.formatInstant
import uk.gov.hmrc.bindingtariffclassification.model.{Filter, Sort}
import uk.gov.hmrc.bindingtariffclassification.sort.SortField._

@Singleton
class SearchMapper {

  def filterBy(filter: Filter): JsObject = {
    JsObject(
      Map() ++
        filter.queueId.map("queueId" -> nullifyNoneValues(_)) ++
        filter.assigneeId.map("assignee.id" -> nullifyNoneValues(_)) ++
        filter.statuses.map("status" -> inArray[CaseStatus](_)) ++
        filter.traderName.map("application.holder.businessName" -> nullifyNoneValues(_)) ++
        filter.minDecisionEnd.map("decision.effectiveEndDate" -> greaterThan(_)(formatInstant)) ++
        filter.commodityCode.map("decision.bindingCommodityCode" -> numberStartingWith(_)) ++
        filter.goodDescription.map( gd => any(matchingGoodsDescription(gd) ) ) ++
        filter.keywords.map("keywords" -> containsAll(_))
    )
  }

  private lazy val goodsDescriptionFields = {
    Seq("decision.goodsDescription", "decision.methodCommercialDenomination")
  }

  private def matchingGoodsDescription: String => Seq[(String, JsValueWrapper)] = { gd: String =>
    goodsDescriptionFields map ( _ -> contains(gd) )
  }

  private def any(conditions: Seq[(String, JsValueWrapper)]): (String, JsValue) = {
    "$or" -> Json.toJson( conditions map (Json.obj(_)) )
  }

  def sortBy(sort: Sort): JsObject = {
    Json.obj( toMongoField(sort.field) -> sort.direction.id )
  }

  private def containsAll(s: Set[String]): JsObject = {
    Json.obj("$all" -> s)
  }

  private def greaterThan[T](value: T)(implicit writes: Writes[T]): JsObject = {
    Json.obj("$gte" -> value)
  }

  def reference(reference: String): JsObject = {
    Json.obj("reference" -> reference)
  }

  def updateField(fieldName: String, fieldValue: String): JsObject = {
    Json.obj("$set" -> Json.obj(fieldName -> fieldValue))
  }

  private def inArray[T](values: TraversableOnce[T])(implicit writes: Writes[T]): JsObject = {
    JsObject( Map( "$in" -> JsArray(values.toSeq.map(writes.writes)) ) )
  }

  private def nullifyNoneValues: String => JsValue = { v: String =>
    v match {
      case "none" => JsNull
      case _ => JsString(v)
    }
  }

  private def numberStartingWith(value: String): JsObject = {
    Json.obj( regexFilter(s"^$value\\d*") )
  }

  private def contains(value: String): JsValueWrapper = {
    Json.obj( regexFilter(s".*$value.*"), caseInsensitiveFilter )
  }

  private def regexFilter(reg: String): (String, JsValueWrapper) = {
    "$regex" -> reg
  }

  private lazy val caseInsensitiveFilter: (String, JsValueWrapper) = {
    "$options" -> "i"
  }

  private def notEqualFilter(value: String): JsObject = {
    Json.obj("$ne" -> value)
  }

  private def toMongoField(sort: SortField): String = {
    sort match {
      case DAYS_ELAPSED => "daysElapsed"
      case COMMODITY_CODE => "decision.bindingCommodityCode"
      case s => throw new IllegalArgumentException(s"cannot sort by field: $s")
    }
  }

}
