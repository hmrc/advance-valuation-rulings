/*
 * Copyright 2019 HM Revenue & Customs
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

package uk.gov.hmrc.bindingtariffclassification.service

import java.time.Instant
import java.util.UUID

import javax.inject._
import uk.gov.hmrc.bindingtariffclassification.config.AppConfig
import uk.gov.hmrc.bindingtariffclassification.model.ApplicationType.ApplicationType
import uk.gov.hmrc.bindingtariffclassification.model._
import uk.gov.hmrc.bindingtariffclassification.repository.{CaseRepository, SequenceRepository}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class CaseService @Inject()(appConfig: AppConfig,
                            caseRepository: CaseRepository,
                            sequenceRepository: SequenceRepository,
                            eventService: EventService) {

  def insert(c: Case): Future[Case] = {
    caseRepository.insert(c)
  }

  def addInitialSampleStatusIfExists(c: Case): Future[Unit] = {
    if (c.sample.status.nonEmpty) {
      val details = SampleStatusChange(None, c.sample.status, None)
      eventService.insert(Event(UUID.randomUUID().toString, details, Operator("-1",Some(c.application.contact.name)), c.reference, Instant.now()))
    }
    Future.successful((): Unit)
  }

  def nextCaseReference(applicationType: ApplicationType): Future[String] = {
    sequenceRepository.incrementAndGetByName("case").map {
      _.value + appConfig.caseReferenceStart + (applicationType match {
        case ApplicationType.BTI => appConfig.btiReferenceOffset
        case ApplicationType.LIABILITY_ORDER => appConfig.liabilityReferenceOffset
      })
    }.map(_.toString)
  }

  def update(c: Case, upsert: Boolean): Future[Option[Case]] = {
    caseRepository.update(c, upsert)
  }

  def getByReference(reference: String): Future[Option[Case]] = {
    caseRepository.getByReference(reference)
  }

  def get(search: CaseSearch, pagination: Pagination): Future[Paged[Case]] = {
    caseRepository.get(search, pagination)
  }

  def deleteAll(): Future[Unit] = {
    caseRepository.deleteAll()
  }
}
