/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.advancevaluationrulings.utils
import java.net.ServerSocket

import scala.jdk.CollectionConverters._
import scala.util.Using

import play.api.libs.json.{Json, JsValue}
import uk.gov.hmrc.advancevaluationrulings.utils.WireMockHelper.{wireMockPort, MappingBuilderExt, ResponseDefinitionBuilderExt}

import com.github.tomakehurst.wiremock.{client, WireMockServer}
import com.github.tomakehurst.wiremock.client.{MappingBuilder, ResponseDefinitionBuilder, WireMock}
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import com.github.tomakehurst.wiremock.http.{HttpHeader, HttpHeaders}

trait WireMockHelper {
  val wireMockServer = new WireMockServer(wireMockConfig.port(wireMockPort))

  def startWireMock(): Unit = {
    WireMock.configureFor(wireMockPort)
    wireMockServer.start()
  }

  def stopWireMock(): Unit =
    wireMockServer.stop()

  def stubPost(
    url: String,
    requestBody: JsValue,
    statusCode: Int,
    responseBody: String,
    requestHeaders: Set[(String, String)] = Set.empty,
    responseHeaders: Set[(String, String)] = Set.empty
  ): Unit =
    stubFor(
      post(urlEqualTo(url))
        .withRequestBody(equalTo(Json.stringify(requestBody)))
        .withRequestHeaders(requestHeaders)
        .willReturn(
          aResponse()
            .withStatus(statusCode)
            .withResponseHeaders(responseHeaders)
            .withBody(responseBody)
        )
    )
}

object WireMockHelper {

  val wireMockPort: Int = Using(new ServerSocket(0))(_.getLocalPort)
    .getOrElse(throw new Exception("Failed to find random free port"))

  implicit class MappingBuilderExt(builder: client.MappingBuilder) {

    def withRequestHeaders(headers: Set[(String, String)]): MappingBuilder =
      headers.foldLeft(builder) {
        (builder, header) =>
          val (key, value) = header
          builder.withHeader(key, equalTo(value))
      }
  }

  implicit class ResponseDefinitionBuilderExt(builder: ResponseDefinitionBuilder) {

    def withResponseHeaders(headers: Set[(String, String)]): ResponseDefinitionBuilder = {
      val responseHeadersWithContentType = Set("Content-Type" -> "application/json; charset=utf-8")
        .union(headers)
        .toList
        .map { case (key, value) => HttpHeader.httpHeader(key, value) }
      builder.withHeaders(new HttpHeaders(responseHeadersWithContentType.asJava))
    }
  }
}
