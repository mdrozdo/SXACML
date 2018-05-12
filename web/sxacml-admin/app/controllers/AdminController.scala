package controllers

import javax.inject._

import ontoplay.models.ontologyModel.OntoClass
import ontoplay.models.ontologyReading.OntologyReader
import play.api.mvc._;


/**
  * This controller creates an `Action` to handle HTTP requests to the
  * application's home page.
  */
@Singleton
class AdminController @Inject()(val ontoReader: OntologyReader) extends Controller {


  /**
    * Create an Action to render an HTML page with a welcome message.
    * The configuration in the `routes` file means that this method
    * will be called when the application receives a `GET` request with
    * a path of `/`.
    */
  def admin = Action {
    val owlClass = getOwlClassFromName("urn:oasis:names:tc:xacml:1.0:subject-category:access-subject")
    Ok(views.html.admin())
  }

  def testRequest = Action {
    Ok(views.html.testRequest())
  }

  protected def getOwlClassFromName(className: String): OntoClass = ontoReader.getOwlClass(className)
}
