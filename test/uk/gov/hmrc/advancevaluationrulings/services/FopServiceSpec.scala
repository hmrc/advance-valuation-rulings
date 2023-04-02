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
import play.api.i18n.MessagesApi
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.FakeRequest
import uk.gov.hmrc.advancevaluationrulings.models.application._
import uk.gov.hmrc.advancevaluationrulings.views.xml.ApplicationPdf

import java.nio.file.{Files, Paths}
import java.time.Instant
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

    "must generate a test PDF" in {

      val application = Application(
        id = ApplicationId(25467987),
        applicantEori = "GB905360708861",
        trader = TraderDetail(
          eori = "GB905360708861",
          businessName = "Some business",
          addressLine1 = "1 The Street",
          addressLine2 = Some("Some town"),
          addressLine3 = None,
          postcode = "AA11 1AA",
          countryCode = "GB",
          phoneNumber = Some("07777 777777")
        ),
        agent = None,
        contact = ContactDetails(
          name = "Contact name",
          email = "contact.email@example.com",
          phone = Some("0191 1919191")
        ),
        requestedMethod = MethodOne(
          saleBetweenRelatedParties = Some("Some details of a sale between related parties.\n\nThis shows some line breaks."),
          goodsRestrictions = Some("Information about any restrictions on sales"),
          saleConditions = Some("Conditions on sale")
        ),
        goodsDetails = GoodsDetails(
          goodsName = "The name of the goods",
          goodsDescription = "A short description of the goods",
          envisagedCommodityCode = Some("070190"),
          knownLegalProceedings = Some("Information about some known legal proceedings"),
          confidentialInformation = Some("confidential information")
        ),
        attachments = Nil,
        submissionReference = "submissionReference",
        created = Instant.now,
        lastUpdated = Instant.now
      )

      val view = app.injector.instanceOf[ApplicationPdf]
      val messages = app.injector.instanceOf[MessagesApi].preferred(FakeRequest())
      val xmlString = view.render(application, messages).body
      val result = fopService.render(xmlString).futureValue

      val fileName = "test/resources/fop/test.pdf"
      Files.write(Paths.get(fileName), result)
    }
  }
}
