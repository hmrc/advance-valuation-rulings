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

package uk.gov.hmrc.advancevaluationrulings.services

import javax.inject.{Inject, Singleton}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

import uk.gov.hmrc.advancevaluationrulings.logging.RequestAwareLogger
import uk.gov.hmrc.advancevaluationrulings.models.{Application, IndividualApplicant, ValuationRulingsApplication}
import uk.gov.hmrc.advancevaluationrulings.models.common.{Envelope, SubmissionSuccess}
import uk.gov.hmrc.advancevaluationrulings.models.errors.{BaseError, SubmissionError}
import uk.gov.hmrc.advancevaluationrulings.repositories.ValuationRulingsRepository
import uk.gov.hmrc.http.HeaderCarrier

import cats.data.EitherT
import cats.implicits._

@Singleton
class ValuationRulingsService @Inject() (repository: ValuationRulingsRepository) {

  private val logger = new RequestAwareLogger(this.getClass)

  def submitApplication(
    application: ValuationRulingsApplication
  )(implicit
    ec: ExecutionContext,
    hc: HeaderCarrier
  ): EitherT[Future, BaseError, SubmissionSuccess] =
    Envelope.apply {
      repository
        .insert(application)
        .map(acknowledged => SubmissionSuccess(acknowledged).asRight)
        .recover {
          case NonFatal(ex) =>
            val eori = application.data.checkRegisteredDetails.eori
            logger.error(s"Failed to insert application with eori: $eori: ${ex.getMessage}")
            SubmissionError(s"Failed to insert application with eori: $eori").asLeft
        }
    }

  def submitApplication(
    application: Application
  )(implicit
    ec: ExecutionContext,
    hc: HeaderCarrier
  ): EitherT[Future, BaseError, SubmissionSuccess] =
    Envelope.apply {
      repository
        .insert(application)
        .map(acknowledged => SubmissionSuccess(acknowledged).asRight)
        .recover {
          case NonFatal(ex) =>

            val eori = application.applicant match {
              case IndividualApplicant(holder, contact) => holder.eori
              case _                                    => ???
            }

            logger.error(s"Failed to insert application with eori: $eori: ${ex.getMessage}")
            SubmissionError(s"Failed to insert application with eori: $eori").asLeft
        }
    }
}
