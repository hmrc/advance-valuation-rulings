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
import uk.gov.hmrc.advancevaluationrulings.views.xml.Layout

class LayoutViewSpec extends BaseViewSpec {

  private val view: Layout = app.injector.instanceOf[Layout]

  val viewViaApply: Xml  = view.apply()
  val viewViaRender: Xml = view.render()
  val viewViaF: Xml      = view.f()

  private val expectedContent: Seq[String] = Seq(
    """<fo:layout-master-set>""",
    """</fo:layout-master-set>""",
    """<fo:region-before region-name="xsl-region-before"/>""",
    """<fo:region-after region-name="xsl-region-after"/>"""
  )

  "LayoutView" - {
    normalPage(expectedContent)
    view.ref must not be None.orNull
  }
}
