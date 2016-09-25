import com.google.inject.AbstractModule
import java.time.Clock
import javax.inject.{Inject, Provider}

import ontoplay.OntologyHelper
import ontoplay.jobs.{JenaOwlReaderConfiguration, OwlApiReaderConfiguration}
import ontoplay.models.ontologyReading.OntologyReader
import ontoplay.models.ontologyReading.jena.JenaOwlReaderConfig
import ontoplay.models.ontologyReading.owlApi.OwlApiReader
import play.api.Environment
import services.{ApplicationTimer, AtomicCounter, Counter}

/**
 * This class is a Guice module that tells Guice how to bind several
 * different types. This Guice module is created when the Play
 * application starts.

 * Play will automatically use any class called `Module` that is in
 * the root package. You can create modules in other locations by
 * adding `play.modules.enabled` settings to the `application.conf`
 * configuration file.
 */
class Module extends AbstractModule {

  override def configure() = {
    // Use the system clock as the default implementation of Clock
    bind(classOf[Clock]).toInstance(Clock.systemDefaultZone)
    // Ask Guice to create an instance of ApplicationTimer when the
    // application starts.
    bind(classOf[ApplicationTimer]).asEagerSingleton()
    // Set AtomicCounter as the implementation for Counter.
    bind(classOf[Counter]).to(classOf[AtomicCounter])

    bind(classOf[OntologyReader]).toProvider(classOf[JenaReaderProvider])
    class JenaReaderProvider @Inject()(val env: Environment) extends Provider[OntologyReader]{
      override def get(): OntologyReader = Option(OntologyReader.getGlobalInstance).getOrElse({
        //TODO: Change to use local constants
        new JenaOwlReaderConfiguration().initialize(OntologyHelper.file, new JenaOwlReaderConfig().useLocalMapping(OntologyHelper.iriString, OntologyHelper.fileName))
        OntologyReader.getGlobalInstance
      })
    }

  }

}
