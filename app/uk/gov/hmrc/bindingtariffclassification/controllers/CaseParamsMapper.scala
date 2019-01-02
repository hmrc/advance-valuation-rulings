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

package uk.gov.hmrc.bindingtariffclassification.controllers

import javax.inject.Singleton
import uk.gov.hmrc.bindingtariffclassification.model.search.CaseParamsFilter

@Singleton
class CaseParamsMapper {

  def from(queueId: Option[String], assigneeId: Option[String], status: Option[String]): CaseParamsFilter = {
    CaseParamsFilter(
      queueId = queueId,
      assigneeId = assigneeId,
      status = status.map(_.split(",").map(_.trim))
    )
  }

}
