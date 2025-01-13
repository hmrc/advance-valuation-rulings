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

import org.apache.fop.apps.FopFactory
import org.apache.xmlgraphics.util.MimeConstants

import java.io.{OutputStream, StringReader}
import javax.inject.{Inject, Singleton}
import javax.xml.transform.TransformerFactory
import javax.xml.transform.sax.SAXResult
import javax.xml.transform.stream.StreamSource
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Using

@Singleton
class FopService @Inject() (
  fopFactory: FopFactory
)(implicit ec: ExecutionContext) {

  def render(input: String): Future[Array[Byte]] = Future {

    Using.resource(new ByteArrayOutput()) { out =>
      // Turn on accessibility features
      val userAgent = fopFactory.newFOUserAgent()
      userAgent.setAccessibility(true)

      val fop = fopFactory.newFop(MimeConstants.MIME_PDF, userAgent, out)

      val transformerFactory = TransformerFactory.newInstance()
      val transformer        = transformerFactory.newTransformer()

      val source = new StreamSource(new StringReader(input))
      val result = new SAXResult(fop.getDefaultHandler)

      transformer.transform(source, result)

      out.toByteArray
    }
  }
}
class ByteArrayOutput extends OutputStream {
  private val buffer = ArrayBuffer[Byte]()

  // Override the write method to add a single byte
  override def write(byte: Int): Unit = buffer += byte.toByte

  // Convert the buffer to a byte array
  def toByteArray: Array[Byte] = buffer.toArray

  // Reset the buffer
  def reset(): Unit = buffer.clear()
}
