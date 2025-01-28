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
import uk.gov.hmrc.advancevaluationrulings.views.xml.BookmarkTree

class BookmarkTreeViewSpec extends BaseViewSpec {

  private val view: BookmarkTree = app.injector.instanceOf[BookmarkTree]

  val viewViaApply: Xml  = view.apply()
  val viewViaRender: Xml = view.render(messages)
  val viewViaF: Xml      = view.f()(messages)

  private val expectedContent: Seq[String] = Seq(
    """<fo:bookmark internal-destination="title">""",
    s"""<fo:bookmark-title>${messages("pdf.title")}</fo:bookmark-title>"""
  )

  "BookmarkTreeView" - {
    normalPage(expectedContent)
    view.ref must not be None.orNull
  }
}
