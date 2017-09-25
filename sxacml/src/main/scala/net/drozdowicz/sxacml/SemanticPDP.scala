package scala.net.drozdowicz.sxacml

import java.io.{IOException, File}
import java.util
import java.util.Properties

import net.drozdowicz.sxacml.OwlAttributeModule
import org.wso2.balana.finder.{ResourceFinderModule, AttributeFinderModule, AttributeFinder, ResourceFinder}
import org.wso2.balana.{PDPConfig, PDP, Balana}
import org.wso2.balana.finder.impl.FileBasedPolicyFinderModule
import collection.JavaConversions._

/**
 * Created by michal on 2015-05-02.
 */
class SemanticPDP(policyLocation: String, ontologyFolderPath: String, rootOntologyId: String) {

  val balana = initBalana()
  val pdp = createPdp()

  def evaluate(request: String): String = {
    pdp.evaluate(request)
  }

  private def initBalana(): Balana = {
    System.setProperty(FileBasedPolicyFinderModule.POLICY_DIR_PROPERTY, policyLocation)
    Balana.getInstance
  }

  private def createPdp(): PDP = {
    val pdpConfig = balana.getPdpConfig
    pdpConfig.getPolicyFinder.setModules(Set(new FileBasedPolicyFinderModule()))
    pdpConfig.getPolicyFinder.init()
    val attributeFinder = initializeAttributeFinder(pdpConfig)
    val resourceFinder = initializeResourceFinders(pdpConfig)

    return new PDP(new PDPConfig(attributeFinder, pdpConfig.getPolicyFinder, resourceFinder, true))
  }

  def initializeAttributeFinder(pdpConfig: PDPConfig): AttributeFinder = {
    val attributeFinder = pdpConfig.getAttributeFinder
    val finderModules = attributeFinder.getModules.filter(m => m.getClass() != classOf[OwlAttributeModule]);
    finderModules.add(createAttributeModule)
    attributeFinder.setModules(finderModules)
    attributeFinder
  }

  private def createAttributeModule: AttributeFinderModule = {
    val attributeModule = new OwlAttributeModule
    val properties = new Properties
    properties.putAll(Map(
      "ontologyFolderPath" -> ontologyFolderPath,
      "rootOntologyId" -> rootOntologyId
    ))
    attributeModule.init(properties)
    attributeModule
  }

  def initializeResourceFinders(pdpConfig: PDPConfig): ResourceFinder = {
    val resourceFinder = pdpConfig.getResourceFinder
    val finderModules = resourceFinder.getModules.filter(m =>
      m.getClass() != classOf[OwlResourceClassFinderModule]
        && m.getClass() != classOf[OwlResourceHierarchyFinderModule]);
    finderModules.add({
      new OwlResourceClassFinderModule(ontologyFolderPath, rootOntologyId)
    })
    finderModules.add({
      new OwlResourceHierarchyFinderModule(ontologyFolderPath, rootOntologyId)
    })
    resourceFinder.setModules(finderModules)
    resourceFinder
  }

  }
