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

package uk.gov.hmrc.advancevaluationrulings.controllers

import javax.inject.{Inject, Singleton}

import scala.concurrent.ExecutionContext

import play.api.Logging
import play.api.mvc._
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.objectstore.client.Path
import uk.gov.hmrc.objectstore.client.play._
import uk.gov.hmrc.objectstore.client.play.Implicits._
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import _root_.play.api.libs.streams.Accumulator
import akka.actor.ActorSystem
import akka.stream.scaladsl.Source
import akka.util.ByteString

@Singleton()
class HelloWorldObjectStoreController @Inject() (
  objectStoreClient: PlayObjectStoreClient,
  cc: ControllerComponents
)(implicit
  val ec: ExecutionContext,
  system: ActorSystem
) extends BackendController(cc)
    with Logging {

  /** The directory to store the files.
    *
    * The directory in object-store is stored under the owner's name. e.g.
    * "$owner/$directory/$filename"
    *
    * In this case, the owner of these files is the service itself (hello-world-object-store) as
    * defined in application.confg#appName, so does not need to be provided when interacting with
    * the store.
    */
  private val directory: Path.Directory =
    Path.Directory("account/summary")

  /** Example Play Body Parser used for streaming a body.
    */
  private val streaming: BodyParser[Source[ByteString, _]] = BodyParser {
    _ => Accumulator.source[ByteString].map(Right.apply)
  }

  def putObject(fileName: String): Action[Source[ByteString, _]] = Action.async(streaming) {
    implicit request =>
      /* Provide a pointer to the "fileName" under the directory. */
      objectStoreClient
        .putObject(directory.file(fileName), request.body)
        .map(_ => Created("Document stored."))
        /* Error handling can be left to Bootstrap or customised */
        .recover {
          case UpstreamErrorResponse(message, statusCode, _, _) =>
            logger.error(s"Upstream error with status code '$statusCode' and message: $message")
            InternalServerError("Upstream error encountered")
          case e: Exception                                     =>
            logger.error(s"An error was encountered saving the document.", e)
            InternalServerError("Error saving the document")
        }
  }

  def deleteObject(fileName: String) = Action.async {
    implicit request => objectStoreClient.deleteObject(directory.file(fileName)).map(_ => NoContent)
  }
}
