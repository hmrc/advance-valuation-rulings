/*
 * Copyright 2025 HM Revenue & Customs
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

package uk.gov.hmrc.advancevaluationrulings.config

import java.time.Clock

import play.api.{Configuration, Environment}
import play.api.inject.Binding
import uk.gov.hmrc.advancevaluationrulings.services.{DefaultDmsSubmissionService, DmsSubmissionService, SaveFileDmsSubmissionService}
import uk.gov.hmrc.crypto.{Decrypter, Encrypter}

import org.apache.fop.apps.FopFactory

class Module extends play.api.inject.Module {

  override def bindings(
    environment: Environment,
    configuration: Configuration
  ): collection.Seq[Binding[?]] = {

    val authTokenInitialiserBinding: Binding[InternalAuthTokenInitialiser] =
      if (configuration.get[Boolean]("internal-auth-token-initialiser.enabled")) {
        bind[InternalAuthTokenInitialiser].to[InternalAuthTokenInitialiserImpl].eagerly()
      } else {
        bind[InternalAuthTokenInitialiser].to[NoOpInternalAuthTokenInitialiser].eagerly()
      }

    val dmsSubmissionServiceBinding: Binding[DmsSubmissionService] =
      if (configuration.get[Boolean]("dms-submission.enabled")) {
        bind[DmsSubmissionService].to[DefaultDmsSubmissionService].eagerly()
      } else {
        bind[DmsSubmissionService].to[SaveFileDmsSubmissionService].eagerly()
      }

    Seq(
      bind[AppConfig].toSelf.eagerly(),
      bind[Clock].toInstance(Clock.systemUTC()),
      bind[FopFactory].toProvider[FopFactoryProvider].eagerly(),
      bind[Encrypter & Decrypter].toProvider[CryptoProvider],
      authTokenInitialiserBinding,
      dmsSubmissionServiceBinding
    )
  }
}
