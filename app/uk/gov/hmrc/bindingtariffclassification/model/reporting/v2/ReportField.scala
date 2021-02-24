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

package uk.gov.hmrc.bindingtariffclassification.model.reporting.v2

import java.time.Instant
import uk.gov.hmrc.bindingtariffclassification.model._

sealed abstract class ReportField[A](val fieldName: String, val underlyingField: String)
    extends Product
    with Serializable {
  def withValue(value: Option[A]): ReportResultField[A]
}

case class NumberField(override val fieldName: String, override val underlyingField: String)
    extends ReportField[Long](fieldName, underlyingField) {
  def withValue(value: Option[Long]): NumberResultField = NumberResultField(fieldName, value)
}
case class StatusField(override val fieldName: String, override val underlyingField: String)
    extends ReportField[CaseStatus.Value](fieldName, underlyingField) {
  def withValue(value: Option[CaseStatus.Value]): StatusResultField = StatusResultField(fieldName, value)
}
case class CaseTypeField(override val fieldName: String, override val underlyingField: String)
    extends ReportField[ApplicationType.Value](fieldName, underlyingField) {
  def withValue(value: Option[ApplicationType.Value]): CaseTypeResultField = CaseTypeResultField(fieldName, value)
}
case class ChapterField(override val fieldName: String, override val underlyingField: String)
    extends ReportField[String](fieldName, underlyingField) {
  def withValue(value: Option[String]): StringResultField = StringResultField(fieldName, value)
}
case class DateField(override val fieldName: String, override val underlyingField: String)
    extends ReportField[Instant](fieldName, underlyingField) {
  def withValue(value: Option[Instant]): DateResultField = DateResultField(fieldName, value)
}
case class StringField(override val fieldName: String, override val underlyingField: String)
    extends ReportField[String](fieldName, underlyingField) {
  def withValue(value: Option[String]): StringResultField = StringResultField(fieldName, value)
}
case class DaysSinceField(override val fieldName: String, override val underlyingField: String)
    extends ReportField[Long](fieldName, underlyingField) {
  def withValue(value: Option[Long]): NumberResultField = NumberResultField(fieldName, value)
}

object ReportField {
  val Count         = NumberField("count", "count")
  val Reference     = StringField("reference", "reference")
  val Status        = StatusField("status", "status")
  val CaseType      = CaseTypeField("case_type", "application.type")
  val Chapter       = ChapterField("chapter", "decision.bindingCommodityCode")
  val GoodsName     = StringField("goods_name", "application.goodName")
  val TraderName    = StringField("trader_name", "application.traderName")
  val User          = StringField("assigned_user", "assignee.id")
  val Team          = StringField("assigned_team", "queueId")
  val DateCreated   = DateField("date_created", "createdDate")
  val DateCompleted = DateField("date_completed", "decision.effectiveStartDate")
  val ElapsedDays   = NumberField("elapsed_days", "daysElapsed")
  val TotalDays     = DaysSinceField("total_days", "createdDate")
  val ReferredDays  = NumberField("referred_days", "referredDaysElapsed")

  val fields: Map[String, ReportField[_]] = List(
    Count,
    Reference,
    Status,
    CaseType,
    Chapter,
    GoodsName,
    TraderName,
    User,
    Team,
    DateCreated,
    DateCompleted,
    ElapsedDays,
    TotalDays,
    ReferredDays
  ).map(field => field.fieldName -> field).toMap
}