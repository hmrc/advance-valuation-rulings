/*
 * Copyright 2025 HM Revenue & Customs
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
import uk.gov.hmrc.advancevaluationrulings.views.xml.MethodThreeSnippet

class MethodThreeSnippetViewSpec extends BaseViewSpec {

  private val view: MethodThreeSnippet = app.injector.instanceOf[MethodThreeSnippet]

  val viewViaApply: Xml  = view.apply(methodThree)
  val viewViaRender: Xml = view.render(methodThree, messages)
  val viewViaF: Xml      = view.f(methodThree)(messages)

  private val expectedContent: Seq[String] = Seq(
    messages("pdf.method"),
    messages("pdf.whyNotOtherMethods.3"),
    messages("pdf.previousSimilarGoods"),
    s"""<fo:block margin-bottom="3mm">${messages("pdf.method.3")}</fo:block>""",
    s"""<fo:block margin-bottom="3mm">$randomString</fo:block>"""
  )

  "MethodThreeSnippetView" - {
    normalPage(expectedContent)
    view.ref must not be None.orNull
  }
}
