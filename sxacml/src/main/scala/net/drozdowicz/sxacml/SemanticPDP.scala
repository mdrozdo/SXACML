package scala.net.drozdowicz.sxacml

import java.io.{File, IOException}
import java.util
import java.util.Properties

import net.drozdowicz.sxacml.OwlAttributeModule
import org.geotools.xacml.geoxacml.config.GeoXACML
import org.wso2.balana.cond.{FunctionFactory, FunctionFactoryProxy, StandardFunctionFactory}
import org.wso2.balana.finder.{AttributeFinder, AttributeFinderModule, ResourceFinder, ResourceFinderModule}
import org.wso2.balana.{Balana, PDP, PDPConfig}
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
    val owlStore = new OwlAttributeStore(ontologyFolderPath, rootOntologyId)
    val owlAttributeModule = createAttributeModule(owlStore)

    initializeFunctions(pdpConfig, owlStore)
    pdpConfig.getPolicyFinder.setModules(Set(new FileBasedPolicyFinderModule()))
    pdpConfig.getPolicyFinder.init()
    val attributeFinder = initializeAttributeFinder(pdpConfig, owlAttributeModule)
    val resourceFinder = initializeResourceFinders(pdpConfig)



    return new PDP(new PDPConfig(attributeFinder, pdpConfig.getPolicyFinder, resourceFinder, true))
  }

  private def initializeAttributeFinder(pdpConfig: PDPConfig, owlAttributeModule: OwlAttributeModule): AttributeFinder = {
    val attributeFinder = pdpConfig.getAttributeFinder
    val finderModules = attributeFinder.getModules.filter(m => m.getClass() != classOf[OwlAttributeModule]);
    finderModules.add(owlAttributeModule)
    attributeFinder.setModules(finderModules)
    attributeFinder
  }

  private def createAttributeModule(owlAttributeStore: OwlAttributeStore): OwlAttributeModule = {
    val attributeModule = new OwlAttributeModule

    attributeModule.init(owlAttributeStore)
    attributeModule
  }

  private def initializeResourceFinders(pdpConfig: PDPConfig): ResourceFinder = {
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

  private def initializeFunctions(pdpConfig: PDPConfig, owlAttributeStore: OwlAttributeStore) = {

    //val factoryProxy = FunctionFactory.getInstance()
    val factoryProxy = StandardFunctionFactory.getNewFactoryProxy
    val factory = factoryProxy.getGeneralFactory()
    factory.addFunction(new BoolTextCompare)
    factory.addFunction(new PointFromString)
    factory.addFunction(new SparqlPathSelect(owlAttributeStore))
    factory.addFunction(new SparqlSelect(owlAttributeStore))

    GeoXACML.initialize(factoryProxy);

    FunctionFactory.setDefaultFactory(factoryProxy)
  }

}
