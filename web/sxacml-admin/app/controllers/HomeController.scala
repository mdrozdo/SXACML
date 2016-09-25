package controllers

import javax.inject._

import ontoplay.OntologyHelper
import play.api._
import play.api.mvc._
import ontoplay.controllers.OntologyController
import ontoplay.models.ontologyModel.OntoClass
import ontoplay.models.ontologyReading.OntologyReader;


/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class HomeController @Inject()(val ontoReader: OntologyReader) extends Controller{



  /**
   * Create an Action to render an HTML page with a welcome message.
   * The configuration in the `routes` file means that this method
   * will be called when the application receives a `GET` request with
   * a path of `/`.
   */
  def index = Action {
    val owlClass = getOwlClassFromName("Organization")
    Ok(views.html.index(owlClass))
  }

  protected def getOwlClassFromName(className: String): OntoClass = ontoReader.getOwlClass(OntologyHelper.nameSpace + className)
}
