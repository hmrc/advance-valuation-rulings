package uk.gov.hmrc.advancevaluationrulings.config

import com.google.inject.AbstractModule

class Module extends AbstractModule {

  override def configure(): Unit =
    bind(classOf[AppConfig]).asEagerSingleton()
}
