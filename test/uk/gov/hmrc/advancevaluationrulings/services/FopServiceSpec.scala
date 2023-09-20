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

import java.nio.file.{Files, Paths}
import java.time.Instant

import scala.io.Source

import play.api.i18n.MessagesApi
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.FakeRequest
import uk.gov.hmrc.advancevaluationrulings.models.application._
import uk.gov.hmrc.advancevaluationrulings.models.application.Privacy.{Confidential, Public}
import uk.gov.hmrc.advancevaluationrulings.views.xml.ApplicationPdf

import org.apache.pdfbox.pdmodel.PDDocument
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers

class FopServiceSpec extends AnyFreeSpec with Matchers with ScalaFutures with IntegrationPatience {

  private val app        = GuiceApplicationBuilder().build()
  private val fopService = app.injector.instanceOf[FopService]
  val attmts             = Seq(
    Attachment(
      12345,
      "SomeFile1",
      Some("description of file"),
      "someLocation",
      Confidential,
      "pdf",
      12345
    ),
    Attachment(
      12345,
      "SomeFile2",
      Some("description of file"),
      "someLocation",
      Public,
      "pdf",
      12345
    ),
    Attachment(
      12345,
      "SomeFile3",
      Some("description of file"),
      "someLocation",
      Confidential,
      "pdf",
      12345
    )
  )

  "render" - {

    "must render some fop content as a pdf" in {
      val input  = Source.fromResource("fop/simple.fo").mkString
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
        agent = Some(
          TraderDetail(
            eori = "GB123123456456",
            businessName = "Some other business",
            addressLine1 = "2 The Street",
            addressLine2 = Some("Some town"),
            addressLine3 = None,
            postcode = "AA11 1AA",
            countryCode = "GB",
            phoneNumber = Some("07777 777778")
          )
        ),
        contact = ContactDetails(
          name = "Contact name",
          email = "contact.email@example.com",
          phone = Some("0191 1919191")
        ),
        requestedMethod = MethodOne(
          saleBetweenRelatedParties = Some(
            "Lorem ipsum dolor sit amet. Sed internos corporis qui quod ipsum sit saepe dolores ab quas similique ut commodi tempora et facilis porro ut officiis nihil.\n\nUt eveniet assumenda sit quod fugit ut quae illo est amet iste. Ab nulla quia aut ipsam cumque aut aspernatur enim hic maiores voluptas aut dolores repudiandae eum maxime odio. In nesciunt mollitia quo reprehenderit natus qui soluta sequi."
          ),
          goodsRestrictions = Some(
            "Lorem ipsum dolor sit amet. Sed internos corporis qui quod ipsum sit saepe dolores ab quas similique ut commodi tempora et facilis porro ut officiis nihil.\n\nUt eveniet assumenda sit quod fugit ut quae illo est amet iste. Ab nulla quia aut ipsam cumque aut aspernatur enim hic maiores voluptas aut dolores repudiandae eum maxime odio. In nesciunt mollitia quo reprehenderit natus qui soluta sequi."
          ),
          saleConditions = Some(
            "Lorem ipsum dolor sit amet. Sed internos corporis qui quod ipsum sit saepe dolores ab quas similique ut commodi tempora et facilis porro ut officiis nihil."
          )
        ),
        goodsDetails = GoodsDetails(
          goodsName = "The name for the goods",
          goodsDescription = "A short description of the goods",
          envisagedCommodityCode = Some("070190"),
          knownLegalProceedings = Some(
            "Lorem ipsum dolor sit amet. Sed internos corporis qui quod ipsum sit saepe dolores ab quas similique ut commodi tempora et facilis porro ut officiis nihil.\n\nUt eveniet assumenda sit quod fugit ut quae illo est amet iste. Ab nulla quia aut ipsam cumque aut aspernatur enim hic maiores voluptas aut dolores repudiandae eum maxime odio. In nesciunt mollitia quo reprehenderit natus qui soluta sequi."
          ),
          confidentialInformation = Some("Some confidential information")
        ),
        attachments = attmts,
        whatIsYourRoleResponse = Some(WhatIsYourRoleResponse.EmployeeOrg),
        submissionReference = "submissionReference",
        created = Instant.now,
        lastUpdated = Instant.now
      )

      val view      = app.injector.instanceOf[ApplicationPdf]
      val messages  = app.injector.instanceOf[MessagesApi].preferred(FakeRequest())
      val xmlString = view.render(application, messages).body
      val result    = fopService.render(xmlString).futureValue

      val fileName = "test/resources/fop/test.pdf"
      Files.write(Paths.get(fileName), result)
    }
  }
}
