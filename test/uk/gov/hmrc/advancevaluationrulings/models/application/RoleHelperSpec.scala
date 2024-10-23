/*
 * Copyright 2024 HM Revenue & Customs
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

import uk.gov.hmrc.advancevaluationrulings.base.SpecBase

import java.time.Instant
import java.time.temporal.ChronoUnit

class RoleHelperSpec extends SpecBase {

  private val trader: TraderDetail       =
    TraderDetail("eori", "name", "line1", None, None, "postcode", "GB", None, Some(true))
  private val goodsDetails: GoodsDetails = GoodsDetails("description", None, None, None, None, None)
  private val method: MethodOne          = MethodOne(None, None, None)
  private val contact: ContactDetails    = ContactDetails("name", "email", None, None, None)
  private val now: Instant               = Instant.now.truncatedTo(ChronoUnit.MILLIS)

  val roleHelper: RoleHelper = new RoleHelper()

  val application: Application = Application(
    id = ApplicationId(1),
    applicantEori = "applicantEori",
    trader = trader,
    agent = None,
    contact = contact,
    goodsDetails = goodsDetails,
    requestedMethod = method,
    attachments = Nil,
    whatIsYourRoleResponse = Some(WhatIsYourRole.AgentTrader),
    letterOfAuthority = None,
    submissionReference = "submissionReference",
    created = now,
    lastUpdated = now
  )

  "RoleHelper" - {
    "messagesForAgentTraderOrOtherRole method" - {
      "should return the correct message for AgentTrader role" in {
        val messageForAgentTrader = "AgentTrader Message"
        val messageForOther       = "Other Message"

        val result = roleHelper.messagesForAgentTraderOrOtherRole(application, messageForAgentTrader, messageForOther)

        result mustBe messageForAgentTrader
      }

      val roles = Seq(WhatIsYourRole.EmployeeOrg, WhatIsYourRole.AgentOrg, WhatIsYourRole.UnansweredLegacySupport)

      roles.foreach { role =>
        s"should return the correct message for ${role.getClass.getSimpleName} role" in {
          val application           = this.application.copy(
            whatIsYourRoleResponse = Some(role)
          )
          val messageForAgentTrader = "AgentTrader Message"
          val messageForOther       = "Other Message"

          val result = roleHelper.messagesForAgentTraderOrOtherRole(application, messageForAgentTrader, messageForOther)

          result mustBe messageForOther
        }
      }

    }
  }
}
