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

package uk.gov.hmrc.advancevaluationrulings.models

import java.time.Instant

import play.api.libs.json._
import uk.gov.hmrc.advancevaluationrulings.models.common.{UserAnswers, ValuationMethod}
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats

final case class ValuationRulingsApplication(
  data: UserAnswers,
  applicationNumber: String,
  lastUpdated: Instant
) {
  def toApplication(): Application =
    Application(
      applicationNumber,
      IndividualApplicant(
        holder = EORIDetails(
          data.checkRegisteredDetails.eori,
          data.checkRegisteredDetails.name,
          data.checkRegisteredDetails.streetAndNumber,
          None,
          data.checkRegisteredDetails.city,
          data.checkRegisteredDetails.postalCode,
          data.checkRegisteredDetails.country
        ),
        contact = ContactDetails(
          data.applicationContactDetails.name,
          data.applicationContactDetails.email,
          Option(data.applicationContactDetails.phone)
        )
      ),
      requestedMethod = resolveMethod,
      goodsDetails = GoodsDetails(
        data.descriptionOfGoods,
        data.commodityCode,
        Option(data.haveTheGoodsBeenSubjectToLegalChallenges.toString),
        Option(data.hasConfidentialInformation.toString)
      ),
      attachments = extractSupportingDocs
    )

  private def extractSupportingDocs = {
    data.uploadSupportingDocument.toSeq.flatMap {
      supportingDocs =>
        supportingDocs.files.map {
          case (id, value) =>
            UploadedDocument(
              id,
              value.fileName,
              value.downloadUrl,
              value.isConfidential,
              value.mimeType.getOrElse(""),
              value.size.getOrElse(0L)
            )
        }
    }
  }

  private def resolveMethod = {
    data.valuationMethod match {
      case ValuationMethod.Method1 =>
        MethodOne(
          data.isSaleBetweenRelatedParties.map(_.toString),
          data.areThereRestrictionsOnTheGoods.map(_.toString),
          data.isTheSaleSubjectToConditions.map(_.toString)
        )
      case ValuationMethod.Method2 =>
        MethodTwo(
          data.whyIdenticalGoods.getOrElse(""),
          PreviousIdenticalGoods(
            data.describeTheIdenticalGoods.getOrElse("")
          )
        )
      case ValuationMethod.Method3 =>
        MethodThree(
          data.whyIdenticalGoods.getOrElse(""),
          PreviousSimilarGoods(
            data.describeTheIdenticalGoods.getOrElse("")
          )
        )
      case ValuationMethod.Method4 =>
        MethodFour(
          data.whyIdenticalGoods.getOrElse(""),
          data.describeTheIdenticalGoods.getOrElse("")
        )
      case ValuationMethod.Method5 =>
        MethodFive(
          data.whyIdenticalGoods.getOrElse(""),
          data.describeTheIdenticalGoods.getOrElse("")
        )
      case ValuationMethod.Method6 =>
        MethodSix(
          data.whyIdenticalGoods.getOrElse(""),
          data.adoptMethod.getOrElse(""),
          data.valuationDescription.getOrElse("")
        )
    }
  }
}

object ValuationRulingsApplication {

  val reads: Reads[ValuationRulingsApplication] = {

    import play.api.libs.functional.syntax._

    (
      (__ \ "data").read[UserAnswers] and
        (__ \ "applicationNumber").read[String] and
        (__ \ "lastUpdated").read(MongoJavatimeFormats.instantFormat)
    )(ValuationRulingsApplication.apply _)
  }

  val writes: OWrites[ValuationRulingsApplication] = {

    import play.api.libs.functional.syntax._

    (
      (__ \ "data").write[UserAnswers] and
        (__ \ "applicationNumber").write[String] and
        (__ \ "lastUpdated").write(MongoJavatimeFormats.instantFormat)
    )(unlift(ValuationRulingsApplication.unapply))
  }

  implicit val format: OFormat[ValuationRulingsApplication] = OFormat(reads, writes)
}
