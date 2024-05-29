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

import org.apache.commons.io.output.ByteArrayOutputStream
import org.apache.fop.apps.FopFactory
import org.apache.xmlgraphics.util.MimeConstants

import java.io.StringReader
import javax.inject.{Inject, Singleton}
import javax.xml.transform.TransformerFactory
import javax.xml.transform.sax.SAXResult
import javax.xml.transform.stream.StreamSource
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Using

@Singleton
class FopService @Inject() (
  fopFactory: FopFactory
)(implicit ec: ExecutionContext) {

  def render(input: String): Future[Array[Byte]] = Future {

    Using.resource(new ByteArrayOutputStream()) { out =>
      // Turn on accessibility features
      val userAgent = fopFactory.newFOUserAgent()
      userAgent.setAccessibility(true)

      //TODO from lib Note: Due to their internal use of either a Reader or InputStream instance, StreamSource instances may only be used once.
      //TODO this has been done to add the xslt to the transformerFactory.newTransformer()
      val xslt = new StreamSource(new StringReader(input))

      val fop = fopFactory.newFop(MimeConstants.MIME_PDF, userAgent, out)

      val source: StreamSource = new StreamSource(new StringReader(input))
      val result = new SAXResult(fop.getDefaultHandler)

      val transformerFactory = TransformerFactory.newInstance()
      //TODO Attach the xslt here allows to create CSS-like styling
      val transformer = transformerFactory.newTransformer(xslt)

      transformer.transform(source, result)

      out.toByteArray
    }
  }
}
