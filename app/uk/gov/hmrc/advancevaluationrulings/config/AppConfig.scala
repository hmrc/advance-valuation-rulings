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

package uk.gov.hmrc.advancevaluationrulings.config

import javax.inject.{Inject, Singleton}

import play.api.Configuration
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

@Singleton
class AppConfig @Inject() (config: Configuration, servicesConfig: ServicesConfig) {

  val appName: String = config.get[String]("appName")

  val integrationFrameworkBaseUrl: String = servicesConfig.baseUrl("integration-framework")
  val integrationFrameworkToken: String   = getConfigString("integration-framework.token")
  val integrationFrameworkEnv: String     = getConfigString("integration-framework.env")

  val etmpSubscriptionDisplayEndpoint: String = getConfigString(
    "integration-framework.etmp.subscription-display-endpoint"
  )

  private def getConfigString(confKey: String) =
    servicesConfig
      .getConfString(confKey, throw new RuntimeException(s"Could not find config key '$confKey'"))
}