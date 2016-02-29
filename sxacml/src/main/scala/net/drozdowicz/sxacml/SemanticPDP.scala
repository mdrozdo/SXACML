package scala.net.drozdowicz.sxacml

import java.io.{IOException, File}
import java.util
import java.util.Properties

import net.drozdowicz.sxacml.OwlAttributeModule
import org.wso2.balana.finder.{AttributeFinderModule, AttributeFinder}
import org.wso2.balana.{PDPConfig, PDP, Balana}
import org.wso2.balana.finder.impl.FileBasedPolicyFinderModule
import collection.JavaConversions._

/**
 * Created by michal on 2015-05-02.
 */
class SemanticPDP(policyLocation: String/*, ontologyPath: String*/) {

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
    val attributeFinder = pdpConfig.getAttributeFinder
    val finderModules = attributeFinder.getModules.filter(m=>m.getClass() != classOf[OwlAttributeModule]);

    val attributeModule = new OwlAttributeModule
    attributeModule.init(new Properties)
    finderModules.add(attributeModule)
    attributeFinder.setModules(finderModules)
    return new PDP(new PDPConfig(attributeFinder, pdpConfig.getPolicyFinder, null, true))
  }


}
