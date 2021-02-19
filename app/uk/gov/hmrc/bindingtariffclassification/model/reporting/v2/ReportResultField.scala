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

sealed abstract class ReportResultField[A](val fieldName: String, val data: A) extends Product with Serializable

case class NumberResultField(override val fieldName: String, override val data: Long)
    extends ReportResultField[Long](fieldName, data)

case class StatusResultField(override val fieldName: String, override val data: CaseStatus.Value)
    extends ReportResultField[CaseStatus.Value](fieldName, data)

case class CaseTypeResultField(override val fieldName: String, override val data: ApplicationType.Value)
    extends ReportResultField[ApplicationType.Value](fieldName, data)

case class ChapterResultField(override val fieldName: String, override val data: String)
    extends ReportResultField[String](fieldName, data)

case class DateResultField(override val fieldName: String, override val data: Instant)
    extends ReportResultField[Instant](fieldName, data)

case class UserResultField(override val fieldName: String, override val data: Operator)
    extends ReportResultField[Operator](fieldName, data)

case class StringResultField(override val fieldName: String, override val data: String)
    extends ReportResultField[String](fieldName, data)
