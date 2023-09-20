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

package uk.gov.hmrc.advancevaluationrulings.models.application


import enumeratum.{Enum, EnumEntry, PlayJsonEnum}

sealed abstract class WhatIsYourRoleResponse(override val entryName: String) extends EnumEntry

object WhatIsYourRoleResponse
  extends Enum[WhatIsYourRoleResponse]
    with PlayJsonEnum[WhatIsYourRoleResponse] {
  val values: IndexedSeq[WhatIsYourRoleResponse] = findValues


  case object EmployeeOrg extends WhatIsYourRoleResponse("EmployeeOrg")

  case object AgentOrg extends WhatIsYourRoleResponse("AgentOrg")

  case object AgentTrader extends WhatIsYourRoleResponse("AgentTrader")

  case object UnansweredLegacySupport extends WhatIsYourRoleResponse("UnansweredLegacySupport")
}
