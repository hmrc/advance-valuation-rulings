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

package util

import java.time.{Instant, LocalDateTime, ZoneOffset}
import java.util.UUID

import model.CaseStatus.{CaseStatus, _}
import model._
import utils.RandomGenerator

object EventData extends FixedTimeFixtures  {

  private def createEvent(caseRef: String, details: Details, date: Instant = fixedTime): Event =
    Event(
      id            = RandomGenerator.randomUUID(),
      details       = details,
      operator      = Operator(RandomGenerator.randomUUID(), Some("user name")),
      caseReference = caseRef,
      timestamp     = date
    )

  def createNoteEvent(caseReference: String, date: Instant = Instant.now()): Event =
    createEvent(
      caseRef = caseReference,
      details = Note("This is a random note"),
      date    = date
    )

  def createCaseStatusChangeEvent(
    caseReference: String,
    from: CaseStatus = DRAFT,
    to: CaseStatus   = NEW,
    date: Instant    = fixedTime
  ): Event =
    createEvent(
      caseRef = caseReference,
      details = createCaseStatusChangeEventDetails(from, to),
      date    = date
    )

  def createCaseStatusChangeEventDetails(from: CaseStatus, to: CaseStatus): Details =
    to match {
      case CaseStatus.COMPLETED => CompletedCaseStatusChange(from, Some("comment"), None)
      case CaseStatus.REFERRED  => ReferralCaseStatusChange(from, Some("comment"), None, "referredTo", Nil)
      case CaseStatus.REJECTED =>
        RejectCaseStatusChange(from, to, Some("comment"), None, RejectReason.NO_INFO_FROM_TRADER)
      case CaseStatus.CANCELLED =>
        CancellationCaseStatusChange(from, Some("comment"), None, CancelReason.INVALIDATED_OTHER)
      case _ => CaseStatusChange(from, to, Some("comment"), None)
    }

  def createExtendedUseStatusChangeEvent(caseReference: String): Event =
    createEvent(
      caseRef = caseReference,
      details = ExtendedUseStatusChange(from = true, to = false, comment = Some("comment"))
    )

  def createQueueChangeEvent(caseReference: String): Event =
    createEvent(
      caseRef = caseReference,
      details = QueueChange(from = Some("q1"), to = Some("q2"), comment = Some("comment"))
    )

  def createAssignmentChangeEvent(caseReference: String): Event = {
    val o1 = Operator(RandomGenerator.randomUUID(), Some("user 1"))
    val o2 = Operator(RandomGenerator.randomUUID(), Some("user 2"))

    createEvent(
      caseRef = caseReference,
      details = AssignmentChange(from = Some(o1), to = Some(o2), comment = Some("comment"))
    )
  }

  def anEvent(withModifier: (Event => Event)*): Event = {
    val example = createCaseStatusChangeEvent(UUID.randomUUID().toString)

    withModifier.foldLeft(example)((current: Event, modifier) => modifier.apply(current))
  }

  def withCaseReference(reference: String): Event => Event = _.copy(caseReference = reference)

  def withStatusChange(from: CaseStatus, to: CaseStatus): Event => Event =
    _.copy(details = createCaseStatusChangeEventDetails(from, to))

  def withTimestamp(date: String): Event => Event =
    _.copy(timestamp = LocalDateTime.parse(date).atOffset(ZoneOffset.UTC).toInstant)

}
