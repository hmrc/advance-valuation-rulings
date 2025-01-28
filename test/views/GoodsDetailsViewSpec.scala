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

package views

import play.twirl.api.Xml
import uk.gov.hmrc.advancevaluationrulings.views.xml.GoodsDetails

class GoodsDetailsViewSpec extends BaseViewSpec {

  private val view: GoodsDetails = app.injector.instanceOf[GoodsDetails]

  val viewViaApply: Xml  = view.apply(application)
  val viewViaRender: Xml = view.render(application, messages)
  val viewViaF: Xml      = view.f(application)(messages)

  private val commonExpectedContent: Seq[String] = Seq(
    messages("pdf.goodsDetails"),
    messages("pdf.goodsDescription"),
    messages("pdf.methodRulingQuestion"),
    messages("pdf.methodRuling"),
    messages("pdf.goodsRulingQuestion"),
    messages("pdf.goodsRuling"),
    messages("pdf.commodityCode"),
    messages("pdf.knownLegalProceedings"),
    messages("pdf.knownLegalProceedingsDescription"),
    messages("pdf.confidentialInformation"),
    messages("pdf.confidentialInformationDescription"),
    s"""<fo:block margin-bottom="3mm">${application.goodsDetails.goodsDescription}</fo:block>""",
    s"""<fo:block margin-bottom="3mm">${messages("pdf.yes")}</fo:block>""",
    s"""<fo:block margin-bottom="3mm">$randomString</fo:block>""",
    s"""<fo:block wrap-option="wrap" keep-together="auto">${application.attachments.head.name}</fo:block>""",
    s"""<fo:block wrap-option="wrap" keep-together="auto">${application.attachments.head.privacy}</fo:block>"""
  )

  private val methodOneExpectedContent: Seq[String] = Seq(
    messages("pdf.method"),
    messages("pdf.saleInvolved"),
    messages("pdf.saleBetweenRelatedParties"),
    messages("pdf.saleBetweenRelatedPartiesDescription"),
    messages("pdf.goodsRestrictions"),
    messages("pdf.goodsRestrictionsDescription"),
    messages("pdf.saleConditions"),
    messages("pdf.saleConditionsDescription"),
    s"""<fo:block margin-bottom="3mm">${messages("pdf.method.1")}</fo:block>"""
  )

  private val methodTwoExpectedContent: Seq[String] = Seq(
    messages("pdf.method"),
    messages("pdf.whyNotOtherMethods.2"),
    messages("pdf.previousIdenticalGoods"),
    s"""<fo:block margin-bottom="3mm">${messages("pdf.method.2")}</fo:block>"""
  )

  private val methodThreeExpectedContent: Seq[String] = Seq(
    messages("pdf.method"),
    messages("pdf.whyNotOtherMethods.3"),
    messages("pdf.previousSimilarGoods"),
    s"""<fo:block margin-bottom="3mm">${messages("pdf.method.3")}</fo:block>"""
  )

  private val methodFourExpectedContent: Seq[String] = Seq(
    messages("pdf.method"),
    messages("pdf.whyNotOtherMethods.4"),
    messages("pdf.deductiveMethod"),
    s"""<fo:block margin-bottom="3mm">${messages("pdf.method.4")}</fo:block>"""
  )

  private val methodFiveExpectedContent: Seq[String] = Seq(
    messages("pdf.method"),
    messages("pdf.whyNotOtherMethods.5"),
    messages("pdf.computedValue"),
    s"""<fo:block margin-bottom="3mm">${messages("pdf.method.5")}</fo:block>"""
  )

  private val methodSixExpectedContent: Seq[String] = Seq(
    messages("pdf.method"),
    messages("pdf.whyNotOtherMethods.6"),
    messages("pdf.adaptedMethod"),
    messages("pdf.valuationDescription"),
    s"""<fo:block margin-bottom="3mm">${messages("pdf.method.6")}</fo:block>""",
    s"""<fo:block margin-bottom="3mm">${messages(s"pdf.adaptedMethod.${adaptedMethod.toString}")}</fo:block>"""
  )

  "GoodsDetailsView" - {
    normalPage(commonExpectedContent ++ methodOneExpectedContent)
  }

  Seq(
    (methodTwo, methodTwoExpectedContent),
    (methodThree, methodThreeExpectedContent),
    (methodFour, methodFourExpectedContent),
    (methodFive, methodFiveExpectedContent),
    (methodSix, methodSixExpectedContent)
  ).foreach { case (requestedMethod, expectedContent) =>
    s"must display correct content for RequestedMethod $requestedMethod" - {
      val renderedView: Xml = view.apply(application.copy(requestedMethod = requestedMethod))
      checkRenderedContent(renderedView, expectedContent, ".apply")
    }
  }

  view.ref must not be None.orNull
}
