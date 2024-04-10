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

package uk.gov.hmrc.advancevaluationrulings.services

import org.apache.pdfbox.Loader
import org.apache.pdfbox.text.PDFTextStripper
import org.scalatest.concurrent.IntegrationPatience
import org.scalatest.freespec.AnyFreeSpec
import play.api
import play.api.i18n.MessagesApi
import play.api.test.FakeRequest
import uk.gov.hmrc.advancevaluationrulings.base.SpecBase
import uk.gov.hmrc.advancevaluationrulings.models.application.Privacy.{Confidential, Public}
import uk.gov.hmrc.advancevaluationrulings.models.application._
import uk.gov.hmrc.advancevaluationrulings.views.xml.ApplicationPdf

import java.io.File
import java.nio.file.{Files, Paths}
import java.time.Instant
import java.util.stream
import scala.io.Source

class FopServiceSpec extends AnyFreeSpec with SpecBase with IntegrationPatience {

  private val app: api.Application = applicationBuilder.build()
  private val fopService: FopService = app.injector.instanceOf[FopService]

  private val attachmentId: Int = 12345
  private val attachmentSize: Long = 12345L

  private val attachments: Seq[Attachment] = Seq(
    Attachment(
      attachmentId,
      "SomeFile1",
      Some("description of file"),
      "someLocation",
      Confidential,
      "pdf",
      attachmentSize
    ),
    Attachment(
      attachmentId,
      "SomeFile2",
      Some("description of file"),
      "someLocation",
      Public,
      "pdf",
      attachmentSize
    ),
    Attachment(
      attachmentId,
      "SomeFile3",
      Some("description of file"),
      "someLocation",
      Confidential,
      "pdf",
      attachmentSize
    )
  )

  private val letterOfAuthority: Attachment = Attachment(
    attachmentId,
    "SomeFile3",
    Some("description of file"),
    "someLocation",
    Confidential,
    "pdf",
    attachmentSize
  )

  private val longSampleText: String =
    """Lorem ipsum dolor sit amet. Sed internos corporis qui quod ipsum sit saepe dolores ab
      | quas similique ut commodi tempora et facilis porro ut officiis nihil.
      |
      |Ut eveniet assumenda sit quod fugit ut quae illo est amet iste. Ab nulla quia aut ipsam
      | cumque aut aspernatur enim hic maiores voluptas aut dolores repudiandae eum maxim
      | odio. In nesciunt mollitia quo reprehenderit natus qui soluta sequi.""".stripMargin

  "render" - {

    "must render some fop content as a pdf" in {

      val input = Source.fromResource("fop/simple.fo").mkString
      val result = fopService.render(input).futureValue
      val pdd = Loader.loadPDF(result)
      val textStripper = new PDFTextStripper

      textStripper.getText(pdd) must include
      """Extensible Markup Language (XML) 1.0
        |The Extensible Markup Language (XML) is a subset of SGML that is completely
        |described in this document. Its goal is to enable generic SGML to be served, received,
        |and processed on the Web in the way that is now possible with HTML. XML has been
        |designed for ease of implementation and for interoperability with both SGML and HTML.
        |""".stripMargin
    }

    "after finishing setup pdf for test" - {

      val application = Application(
        id = ApplicationId(attachmentId),
        applicantEori = "GB905360708861",
        trader = TraderDetail(
          eori = "GB905360708861",
          businessName = "Some business",
          addressLine1 = "1 The Street",
          addressLine2 = Some("Some town"),
          addressLine3 = None,
          postcode = "AA11 1AA",
          countryCode = "GB",
          phoneNumber = Some("07777 777777"),
          isPrivate = Some(true)
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
            phoneNumber = Some("07777 777778"),
            isPrivate = None
          )
        ),
        contact = ContactDetails(
          name = "Contact name",
          email = "contact.email@example.com",
          phone = Some("0191 1919191"),
          companyName = Some("Bob Inc"),
          jobTitle = Some("CEO")
        ),
        requestedMethod = MethodOne(
          saleBetweenRelatedParties = Some(longSampleText),
          goodsRestrictions = Some(longSampleText),
          saleConditions = Some(longSampleText)
        ),
        goodsDetails = GoodsDetails(
          goodsDescription = "A short description of the goods",
          similarRulingMethodInfo = Some("method info"),
          similarRulingGoodsInfo = Some("goods info"),
          envisagedCommodityCode = Some("070190"),
          knownLegalProceedings = Some(longSampleText),
          confidentialInformation = Some("Some confidential information")
        ),
        attachments = attachments,
        whatIsYourRoleResponse = Some(WhatIsYourRole.EmployeeOrg),
        letterOfAuthority = Some(letterOfAuthority),
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

      "must generate a test PDF" in {

        val file = new File("test/resources/fop/test.pdf")
        val document = Loader.loadPDF(file)
        val textStripper = new PDFTextStripper
        val text: String = textStripper.getText(document)
        val lines: List[String] = text.split("\n").toList.map(_.trim)

        lines.headOption mustBe Some("Advance Valuation Ruling")
        lines.length mustBe 136
      }
    }
  }
}
