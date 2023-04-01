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

package uk.gov.hmrc.advancevaluationrulings.services

import org.apache.pdfbox.pdmodel.PDDocument
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.inject.guice.GuiceApplicationBuilder

import scala.io.Source

class FopServiceSpec extends AnyFreeSpec with Matchers with ScalaFutures with IntegrationPatience {

  private val app = GuiceApplicationBuilder().build()
  private val fopService = app.injector.instanceOf[FopService]

  "render" - {

    "must render some fop content as a pdf" in {
      val input = Source.fromResource("fop/simple.fo").mkString
      val result = fopService.render(input).futureValue
      PDDocument.load(result)
    }
  }
}
