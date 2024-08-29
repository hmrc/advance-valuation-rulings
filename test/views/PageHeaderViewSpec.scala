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
import uk.gov.hmrc.advancevaluationrulings.views.xml.PageHeader

class PageHeaderViewSpec extends BaseViewSpec {

  private val view: PageHeader = app.injector.instanceOf[PageHeader]

  val viewViaApply: Xml  = view.apply(isH1 = true)
  val viewViaRender: Xml = view.render(isH1 = true, messages = messages)
  val viewViaF: Xml      = view.f(true)(messages)

  private val expectedContent: Seq[String] = Seq(
    """<fo:external-graphic src="url(conf/resources/logo.jpg)" padding-right="1cm" """ +
      """fox:alt-text="HM Revenue and Customs logo" />""",
    """<fo:block role="H1" margin-left="7.5cm" margin-top="-1.5cm" """ +
      s"""text-align="right" font-size="18pt" font-weight="bold">${messages("pdf.title")}"""
  )

  "PageHeaderView" - {
    normalPage(expectedContent)
  }
}
