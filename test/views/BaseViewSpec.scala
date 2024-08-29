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

import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.i18n.{Messages, MessagesApi}
import play.api.mvc.AnyContent
import play.api.test.FakeRequest
import play.twirl.api.Xml
import uk.gov.hmrc.advancevaluationrulings.base.SpecBase

trait BaseViewSpec extends SpecBase with GuiceOneAppPerSuite with ViewConstants {

  val viewViaApply: Xml
  val viewViaRender: Xml
  val viewViaF: Xml

  override lazy val app: Application = applicationBuilder.build()

  private val fakeRequest: FakeRequest[AnyContent] = FakeRequest()

  implicit val messages: Messages = app.injector.instanceOf[MessagesApi].preferred(fakeRequest)

  protected def checkRenderedContent(view: Xml, expectedContent: Seq[String], method: String = ".apply"): Unit =
    s"when rendered - using $method" in {
      expectedContent.foreach { content =>
        withClue(s"Expected content '$content' was not found in the rendered XML.") {
          view.body must include(content)
        }
      }
    }

  protected def normalPage(expectedContent: Seq[String]): Unit =
    "must behave like a normal page" - {
      Seq((".apply", viewViaApply), (".render", viewViaRender), (".f", viewViaF)).foreach { case (method, view) =>
        checkRenderedContent(view, expectedContent, method)
      }
    }
}
