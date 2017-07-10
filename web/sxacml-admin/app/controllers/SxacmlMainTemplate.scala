package controllers

import ontoplay.controllers.MainTemplate
import play.twirl.api.Html
import views.html.admin_main

/**
  * Created by drozd on 16.06.2017.
  */
class SxacmlMainTemplate extends MainTemplate {

  override def getRenderFunction: (String) => (Html) => (Html) => Html =
    (title: String) => (otherHeader: Html) => (content: Html) => admin_main.render(title, otherHeader, content)

}
