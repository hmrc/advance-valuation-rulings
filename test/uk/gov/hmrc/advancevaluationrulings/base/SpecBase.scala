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

package uk.gov.hmrc.advancevaluationrulings.base

import org.scalatest.OptionValues
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.advancevaluationrulings.repositories.{ApplicationRepository, CounterRepository, UserAnswersRepository}

trait SpecBase extends AnyFreeSpec with Matchers with OptionValues with ScalaFutures {

  val mockApplicationRepository: ApplicationRepository = mock[ApplicationRepository]
  val mockCounterRepository: CounterRepository         = mock[CounterRepository]
  val mockUserAnswersRepository: UserAnswersRepository = mock[UserAnswersRepository]

  def applicationBuilder: GuiceApplicationBuilder = new GuiceApplicationBuilder()
    .overrides(
      bind[ApplicationRepository].to(mockApplicationRepository),
      bind[CounterRepository].to(mockCounterRepository),
      bind[UserAnswersRepository].to(mockUserAnswersRepository)
    )

}
