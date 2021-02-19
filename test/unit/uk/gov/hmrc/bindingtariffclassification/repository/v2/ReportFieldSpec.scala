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

package uk.gov.hmrc.bindingtariffclassification.repository.v2

import java.time.{LocalDateTime, ZoneOffset}
import uk.gov.hmrc.bindingtariffclassification.base.BaseSpec
import uk.gov.hmrc.bindingtariffclassification.model._
import uk.gov.hmrc.bindingtariffclassification.model.reporting.v2._

class ReportFieldSpec extends BaseSpec {
  "ReportField" should {
    "produce ReportResultField using withValue" in {
      for (field <- ReportField.fields.values) field match {
        case field @ CaseTypeField(fieldName, underlyingField) =>
          field.withValue(ApplicationType.BTI) shouldBe CaseTypeResultField(fieldName, ApplicationType.BTI)
        case field @ ChapterField(fieldName, underlyingField) =>
          field.withValue("87") shouldBe ChapterResultField(fieldName, "87")
        case field @ DateField(fieldName, underlyingField) =>
          val dateTime = LocalDateTime.of(2021, 2, 19, 18, 20, 0).toInstant(ZoneOffset.UTC)
          field.withValue(dateTime) shouldBe DateResultField(fieldName, dateTime)
        case field @ DaysSinceField(fieldName, underlyingField) =>
          field.withValue(5L) shouldBe NumberResultField(fieldName, 5L)
        case field @ NumberField(fieldName, underlyingField) =>
          field.withValue(3L) shouldBe NumberResultField(fieldName, 3L)
        case field @ StatusField(fieldName, underlyingField) =>
          field.withValue(CaseStatus.OPEN) shouldBe StatusResultField(fieldName, CaseStatus.OPEN)
        case field @ StringField(fieldName, underlyingField) =>
          field.withValue("Chipotle Paste") shouldBe StringResultField(fieldName, "Chipotle Paste")
        case field @ UserField(fieldName, underlyingField) =>
          field.withValue(Operator("666333", None)) shouldBe UserResultField(fieldName, Operator("666333", None))
      }
    }
  }

}