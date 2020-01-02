/*
 * Copyright 2020 HM Revenue & Customs
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

package util

import java.time.Instant
import java.util.UUID

import uk.gov.hmrc.bindingtariffclassification.model.CaseStatus.CaseStatus
import uk.gov.hmrc.bindingtariffclassification.model._

object Cases {

  private val eoriDetailsExample = EORIDetails(
    "eori", "trader-business-name", "line1", "line2", "line3", "postcode", "country"
  )
  private val eoriAgentDetailsExample = AgentDetails(EORIDetails(
    "eori", "agent-business-name", "line1", "line2", "line3", "postcode", "country"), Some(Attachment("letter-id", public = true, None, Instant.now()))
  )
  private val contactExample = Contact(
    "name", "email", Some("phone")
  )
  private val btiApplicationExample = BTIApplication(
    eoriDetailsExample, contactExample, Some(eoriAgentDetailsExample), offline = false, "Laptop", "Personal Computer", None, None, None, None, None, None
  )
  private val liabilityApplicationExample = LiabilityOrder(
    contactExample, Some("good name"), LiabilityStatus.LIVE, "trader name"
  )
  private val decision = Decision(
    "040900", Some(Instant.now()), Some(Instant.now().plusSeconds(2 * 3600 * 24 * 365)), "justification", "good description", None, None, Some("denomination"), Seq.empty
  )
  private val btiCaseExample = Case(
    UUID.randomUUID().toString, CaseStatus.OPEN, Instant.now(), 0, 0, None, None, None, btiApplicationExample, Some(decision), Seq()
  )

  def aCase(withModifier: (Case => Case)*): Case = {
    withModifier.foldLeft(btiCaseExample.copy(reference = UUID.randomUUID().toString))((current: Case, modifier) => modifier.apply(current))
  }

  def withAssignee(operator: Option[Operator]): Case => Case = {
    _.copy(assignee = operator)
  }

  def withoutAssignee(): Case => Case = {
    _.copy(assignee = None)
  }

  def withActiveDaysElapsed(elapsed: Int): Case => Case = {
    _.copy(daysElapsed = elapsed)
  }

  def withReferredDaysElapsed(elapsed: Int): Case => Case = {
    _.copy(referredDaysElapsed = elapsed)
  }

  def withQueue(queue: String): Case => Case = {
    _.copy(queueId = Some(queue))
  }

  def withoutQueue(): Case => Case = {
    _.copy(queueId = None)
  }

  def withLiabilityDetails(goodName: Option[String] = Some("good name")
                          ): Case => Case = { c =>
    c.copy(application = liabilityApplicationExample.copy(
      goodName = goodName
    ))

  }

  def withBTIDetails(offline: Boolean = false,
                     goodName: String = "good name",
                     goodDescription: String = "good description",
                     confidentialInformation: Option[String] = None,
                     otherInformation: Option[String] = None,
                     reissuedBTIReference: Option[String] = None,
                     relatedBTIReference: Option[String] = None,
                     knownLegalProceedings: Option[String] = None,
                     envisagedCommodityCode: Option[String] = None,
                     sampleToBeProvided: Boolean = false,
                     sampleToBeReturned: Boolean = false): Case => Case = { c =>
    c.copy(application = btiApplicationExample.copy(
      offline = offline,
      goodName = goodName,
      goodDescription = goodDescription,
      confidentialInformation = confidentialInformation,
      otherInformation = otherInformation,
      reissuedBTIReference = reissuedBTIReference,
      relatedBTIReference = relatedBTIReference,
      knownLegalProceedings = knownLegalProceedings,
      envisagedCommodityCode = envisagedCommodityCode,
      sampleToBeProvided = sampleToBeProvided,
      sampleToBeReturned = sampleToBeReturned
    ))
  }

  def withHolder(eori: String = "eori",
                 businessName: String = "business name",
                 addressLine1: String = "address line 1",
                 addressLine2: String = "address line 2",
                 addressLine3: String = "address line 3",
                 postcode: String = "postcode",
                 country: String = "country"): Case => Case = { c =>
    c.copy(application = c.application.asInstanceOf[BTIApplication].copy(holder = EORIDetails(
      eori,
      businessName,
      addressLine1,
      addressLine2,
      addressLine3,
      postcode,
      country
    )))
  }

  def withOptionalApplicationFields(confidentialInformation: Option[String] = None,
                                    otherInformation: Option[String] = None,
                                    reissuedBTIReference: Option[String] = None,
                                    relatedBTIReference: Option[String] = None,
                                    knownLegalProceedings: Option[String] = None,
                                    envisagedCommodityCode: Option[String] = None): Case => Case = { c =>
    c.copy(
      application = c.application.asInstanceOf[BTIApplication].copy(
        confidentialInformation = confidentialInformation,
        otherInformation = otherInformation,
        reissuedBTIReference = reissuedBTIReference,
        relatedBTIReference = relatedBTIReference,
        knownLegalProceedings = knownLegalProceedings,
        envisagedCommodityCode = envisagedCommodityCode
      )
    )
  }

  def withReference(ref: String): Case => Case = {
    _.copy(reference = ref)
  }

  def withStatus(status: CaseStatus): Case => Case = {
    _.copy(status = status)
  }

  def withoutAgent(): Case => Case = {
    c => c.copy(application = c.application.asInstanceOf[BTIApplication].copy(agent = None))
  }

  def withAgent(eori: String = "agent-eori",
                businessName: String = "agent-business",
                addressLine1: String = "agent-address1",
                addressLine2: String = "agent-address2",
                addressLine3: String = "agent-address3",
                postcode: String = "agent-postcode",
                country: String = "agent-country",
                letter: Option[Attachment] = None): Case => Case = { c =>
    val eoriDetails = EORIDetails(eori, businessName, addressLine1, addressLine2, addressLine3, postcode, country)
    val agentDetails = AgentDetails(eoriDetails, letter)
    c.copy(application = c.application.asInstanceOf[BTIApplication].copy(agent = Some(agentDetails)))
  }

  def withAttachment(attachment: Attachment): Case => Case = {
    c => c.copy(attachments = c.attachments :+ attachment)
  }

  def withContact(contact: Contact): Case => Case = {
    c => c.copy(application = c.application.asInstanceOf[BTIApplication].copy(contact = contact))
  }

  def withoutAttachments(): Case => Case = {
    _.copy(attachments = Seq.empty)
  }

  def withoutDecision(): Case => Case = {
    _.copy(decision = None)
  }

  def withDecision(bindingCommodityCode: String = "decision-commodity-code",
                   effectiveStartDate: Option[Instant] = Some(Instant.now()),
                   effectiveEndDate: Option[Instant] = Some(Instant.now()),
                   justification: String = "decision-justification",
                   goodsDescription: String = "decision-goods-description",
                   methodSearch: Option[String] = None,
                   methodExclusion: Option[String] = None,
                   methodCommercialDenomination: Option[String] = None,
                   appeal: Seq[Appeal] = Seq.empty,
                   cancellation: Option[Cancellation] = None
                  ): Case => Case = {
    _.copy(decision = Some(
      Decision(
        bindingCommodityCode,
        effectiveStartDate,
        effectiveEndDate,
        justification,
        goodsDescription,
        methodSearch,
        methodExclusion,
        methodCommercialDenomination,
        appeal,
        cancellation
      )))
  }

  def withCreatedDate(date: Instant): Case => Case = {
    _.copy(createdDate = date)
  }

}
