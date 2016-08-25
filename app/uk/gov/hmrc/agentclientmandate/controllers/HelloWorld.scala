package uk.gov.hmrc.agentclientmandate.controllers

import uk.gov.hmrc.play.frontend.controller.FrontendController
import play.api.mvc._
import scala.concurrent.Future


object HelloWorld extends HelloWorld

trait HelloWorld extends FrontendController {
  val helloWorld = Action.async { implicit request =>
		Future.successful(Ok(uk.gov.hmrc.agentclientmandate.views.html.helloworld.hello_world()))
  }
}
