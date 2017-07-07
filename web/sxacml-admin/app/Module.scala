import java.time.Clock

import com.google.inject.AbstractModule
import controllers.SxacmlMainTemplate
import ontoplay.controllers.MainTemplate
import play.api.{Configuration, Environment, Logger}
import services.{ApplicationTimer, AtomicCounter, Counter}

/**
  * This class is a Guice module that tells Guice how to bind several
  * different types. This Guice module is created when the Play
  * application starts.
  *
  * Play will automatically use any class called `Module` that is in
  * the root package. You can create modules in other locations by
  * adding `play.modules.enabled` settings to the `application.conf`
  * configuration file.
  */
class Module(environment: Environment, configuration: Configuration) extends AbstractModule {

  override def configure() = {
    // Use the system clock as the default implementation of Clock
    bind(classOf[Clock]).toInstance(Clock.systemDefaultZone)
    // Ask Guice to create an instance of ApplicationTimer when the
    // application starts.
    bind(classOf[ApplicationTimer]).asEagerSingleton()
    // Set AtomicCounter as the implementation for Counter.
    bind(classOf[Counter]).to(classOf[AtomicCounter])

    Logger.info("Running in folder: " + environment.rootPath)
    Logger.info("Testing file: " + environment.getFile("."))
    Logger.info("Config is null? : " + (configuration == null))
    Logger.info("Config: " + configuration.underlying.origin().url())
    install(new ontoplay.Module(new play.Environment(environment), new play.Configuration(configuration)));
    bind(classOf[MainTemplate]).to(classOf[SxacmlMainTemplate])

  }
}
