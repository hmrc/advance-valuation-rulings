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
import uk.gov.hmrc.advancevaluationrulings.views.xml.MethodSixSnippet

class MethodSixSnippetViewSpec extends BaseViewSpec {

  private val view: MethodSixSnippet = app.injector.instanceOf[MethodSixSnippet]

  val viewViaApply: Xml  = view.apply(methodSix)
  val viewViaRender: Xml = view.render(methodSix, messages)
  val viewViaF: Xml      = view.f(methodSix)(messages)

  private val expectedContent: Seq[String] = Seq(
    messages("pdf.method"),
    messages("pdf.whyNotOtherMethods.6"),
    messages("pdf.adaptedMethod"),
    messages("pdf.valuationDescription"),
    s"""<fo:block margin-bottom="3mm">${messages("pdf.method.6")}</fo:block>""",
    s"""<fo:block margin-bottom="3mm">${messages(s"pdf.adaptedMethod.${adaptedMethod.toString}")}</fo:block>""",
    s"""<fo:block margin-bottom="3mm">$randomString</fo:block>"""
  )

  "MethodSixSnippetView" - {
    normalPage(expectedContent)
    view.ref must not be None.orNull
  }
}
