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

package uk.gov.hmrc.bindingtariffclassification.base

import akka.stream.Materializer
import org.scalatest.Matchers
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.{BodyParsers, MessagesControllerComponents}
import uk.gov.hmrc.bindingtariffclassification.connector.ResourceFiles
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.play.test.UnitSpec

abstract class BaseSpec
  extends UnitSpec
    with GuiceOneAppPerSuite
    with MockitoSugar
    with ResourceFiles
    with Matchers {

  override lazy val fakeApplication: Application = GuiceApplicationBuilder()
    .configure(
      "metrics.jvm" -> false,
      "metrics.enabled" -> false
    ).build()

  implicit val mat: Materializer = fakeApplication.materializer
  implicit val hc: HeaderCarrier = HeaderCarrier()

  lazy val serviceConfig: ServicesConfig = fakeApplication.injector.instanceOf[ServicesConfig]
  lazy val parser: BodyParsers.Default = fakeApplication.injector.instanceOf[BodyParsers.Default]
  lazy val mcc: MessagesControllerComponents = fakeApplication.injector.instanceOf[MessagesControllerComponents]

}
