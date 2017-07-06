package controllers

import javax.inject._

import ontoplay.OntoplayConfig
import play.api.Configuration
import play.api.mvc._

import scala.net.drozdowicz.sxacml.SemanticPDP

@Singleton
class AuthorizationController @Inject()(config: Configuration, ontoplayConfig: OntoplayConfig) extends Controller {

  def index = Action {
    Ok(views.xml.authorization())
  }

  def evaluateRequest = Action(parse.xml) { request =>
    val policyLocation = config.getString("policyLocation").get
    val ontologyPath = ontoplayConfig.getUploadsPath
    val ontologyUri = ontoplayConfig.getOntologyNamespace

    try {
      val pdp = new SemanticPDP(policyLocation, ontologyPath, ontologyUri)

      val response = pdp.evaluate(request.body.toString)

      Ok(views.xml.pdpResponse(scala.xml.XML.loadString(response)))

    } catch {
      case e: Exception =>
        e.printStackTrace();
        InternalServerError(e.toString)
    }
  }
}
