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

package model

import play.api.libs.json.{JsResult, JsSuccess, JsValue, Json, Reads}

import java.time.{Clock, Instant}

case class NewCaseRequest(
  application: Application,
  attachments: Seq[Attachment] = Seq.empty
) {

  // TODO: Investigate alternative to passing clock here but will still make Case easily tested
  def toCase(reference: String, clock: Clock) = Case(
    reference = reference,
    status    = CaseStatus.NEW,
    createdDate = Instant.now(clock),
    sample = Sample(), // TODO remove sample
    application = application,
    attachments = attachments
  )
}

object NewCaseRequest {

  import RESTFormatters._

  val customReadsFromFE: Reads[NewCaseRequest] = new Reads[NewCaseRequest] {
    override def reads(json: JsValue): JsResult[NewCaseRequest] = {
      for {
        method <- (json \ "requestedMethod").validate[Method]
        holder <- (json \ "applicant" \ "holder").validate[EORIDetails]
        contact <- (json \ "applicant" \ "contact").validate[Contact]
        goodName <- (json \ "goodsDetails" \ "goodDescription").validate[String] //todo not in their model... will need added. SHOULD NOT BE \ "goodDescription"
        goodDescription <- (json \ "goodsDetails" \ "goodDescription").validate[String]
        envisagedCommodityCode = (json \ "goodsDetails" \ "envisagedCommodityCode").validate[String].asOpt
        knownLegalProceedings = (json \ "goodsDetails" \ "knownLegalProceedings").validate[String].asOpt
        confidentialInformation = (json \ "goodsDetails" \ "confidentialInformation").validate[String].asOpt
        attachments <- (json \ "attachments").validate[Seq[Attachment]] //todo this does not look like Seq[UploadedDocument] from FE, why not?
      } yield {
        NewCaseRequest(
          application = BTIApplication(
            holder = holder,
            contact = contact,
            agent = None, //todo not yet implimented.
            offline = false, //todo what even is this???
            goodName = goodName,
            goodDescription = goodDescription,
            requestedMethod = method,
            confidentialInformation = confidentialInformation,
            knownLegalProceedings = knownLegalProceedings,
            envisagedCommodityCode = envisagedCommodityCode,
            applicationPdf = None // todo we dont plan to use pdf generator, is this business requirement required.
          ),
          attachments = attachments
        )
      }
    }
  }
}
