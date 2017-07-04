package controllers

import javax.inject._

import play.api.mvc._

@Singleton
class AuthorizationController @Inject()() extends Controller {

  def index = Action { Ok(views.xml.authorization()) }

  def evaluateRequest = Action(parse.xml) { request =>
    Ok(views.xml.pdpResponse(request.body.toString())) }
}
