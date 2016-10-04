import com.google.inject.AbstractModule
import java.time.Clock
import javax.inject.{Inject, Provider}

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
  val ontologyName: String = "TAN.OWL"
  val file: String = "file:" + "configuration/uploads/" + ontologyName
  val fileName: String = "configuration/uploads/" + ontologyName
  val checkFile: String = "file:samples/TAN/TANCheckk.owl"
  val checkFileName: String = "./samples/TAN/TANCheckk.owl"
  val checkFilePath: String = "./samples/OrganizationCheck"
  val nameSpace: String = "http://www.tan.com#"
  val iriString: String = "http://www.tan.com"


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
        new JenaOwlReaderConfiguration().initialize(file, new JenaOwlReaderConfig().useLocalMapping(iriString, fileName))
        OntologyReader.getGlobalInstance
      })
    }

  }

}
