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

package uk.gov.hmrc.advancevaluationrulings.config

import uk.gov.hmrc.advancevaluationrulings.base.SpecBase

class AppConfigSpec extends SpecBase {

  private val app = applicationBuilder.build()

  private val defaultUserAnswersTtlInDays = 28
  private val defaultApplicationTtlInDays = 366

  "AppConfig" - {
    "should return the correct values" in {
      val appConfig = app.injector.instanceOf[AppConfig]
      appConfig.integrationFrameworkBaseUrl mustBe "http://localhost:6754"
      appConfig.integrationFrameworkToken mustBe "someEncryptedToken"
      appConfig.etmpSubscriptionDisplayEndpoint mustBe "/subscriptions/subscriptiondisplay/v1"
      appConfig.userAnswersTtlInDays mustBe defaultUserAnswersTtlInDays
      appConfig.applicationTtlInDays mustBe defaultApplicationTtlInDays
    }

    "should throw an exception when the config value is not found" in {
      val appConfig = app.injector.instanceOf[AppConfig]
      val exception = intercept[RuntimeException] {
        appConfig.getConfigString("test")
      }

      exception.getMessage mustBe "Could not find config key 'test'"
    }
  }

}
