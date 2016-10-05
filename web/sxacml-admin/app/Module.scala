import com.google.inject.AbstractModule
import java.time.Clock
import javax.inject.{Inject, Provider, Singleton}

import ontoplay.OntologyHelper
import ontoplay.jobs.{JenaOwlReaderConfiguration, OwlApiReaderConfiguration}
import ontoplay.models.ontologyReading.OntologyReader
import ontoplay.models.ontologyReading.jena.JenaOwlReaderConfig
import ontoplay.models.ontologyReading.owlApi.OwlApiReader
import play.api.{Environment, Play}
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
  val fileName: String = "test1.owl"
  val folderPath: String = "onto";
  val checkFileName: String = "temp.owl"
  val checkFolderPath: String = "onto"
  val ontologyNamespace: String = "http://drozdowicz.net/sxacml/test1"


  override def configure() = {
    // Use the system clock as the default implementation of Clock
    bind(classOf[Clock]).toInstance(Clock.systemDefaultZone)
    // Ask Guice to create an instance of ApplicationTimer when the
    // application starts.
    bind(classOf[ApplicationTimer]).asEagerSingleton()
    // Set AtomicCounter as the implementation for Counter.
    bind(classOf[Counter]).to(classOf[AtomicCounter])

    bind(classOf[OntologyReader]).toProvider(classOf[JenaReaderProvider]).in(classOf[Singleton])
    class JenaReaderProvider @Inject()(val env: Environment) extends Provider[OntologyReader]{
      override def get() = Option(OntologyReader.getGlobalInstance).getOrElse({
        new JenaOwlReaderConfiguration().initialize(ontologyNamespace, new JenaOwlReaderConfig().useLocalMapping(ontologyNamespace, folderPath))
        OntologyReader.getGlobalInstance
      })
    }

    bind(classOf[OntologyHelper]).toProvider(classOf[OntologyHelperProvider]).in(classOf[Singleton])
    class OntologyHelperProvider @Inject()(val env: Environment, val ontoReader : OntologyReader) extends Provider[OntologyHelper]{
      override def get() = new OntologyHelper(fileName, folderPath, checkFileName, checkFolderPath, ontologyNamespace, ontoReader)
    }
  }

}
