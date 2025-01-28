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
import uk.gov.hmrc.advancevaluationrulings.views.xml.AttachmentLine

class AttachmentLineViewSpec extends BaseViewSpec {

  private val view: AttachmentLine = app.injector.instanceOf[AttachmentLine]

  private val messagesKey: String = "pdf.attachments"

  val viewViaApply: Xml  = view.apply(messagesKey, attachments)
  val viewViaRender: Xml = view.render(messagesKey, attachments, messages)
  val viewViaF: Xml      = view.f(messagesKey, attachments)(messages)

  private val expectedContent: Seq[String] = Seq(
    s"""<fo:block font-weight="bold">${messages(messagesKey)}</fo:block>""",
    """<fo:block font-weight="bold">File name</fo:block>""",
    """<fo:block font-weight="bold">Confidentiality</fo:block>""",
    s"""<fo:block wrap-option="wrap" keep-together="auto">${attachments.head.name}</fo:block>""",
    s"""<fo:block wrap-option="wrap" keep-together="auto">${attachments.head.privacy}</fo:block>"""
  )

  "AttachmentLineView" - {
    normalPage(expectedContent)
    view.ref must not be None.orNull
  }
}
