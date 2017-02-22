/*
 * Copyright 2017 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.agentclientmandate.controllers.client

import play.api.i18n.Messages
import play.api.libs.json.Format
import play.api.mvc.{AnyContent, Request}
import uk.gov.hmrc.agentclientmandate.config.FrontendAuthConnector
import uk.gov.hmrc.agentclientmandate.controllers.auth.ClientRegime
import uk.gov.hmrc.agentclientmandate.service.{DataCacheService, EmailService}
import uk.gov.hmrc.agentclientmandate.utils.MandateConstants
import uk.gov.hmrc.agentclientmandate.viewModelsAndForms.{YesNoQuestionForm, ClientCache}
import uk.gov.hmrc.agentclientmandate.viewModelsAndForms.ClientEmailForm._
import uk.gov.hmrc.agentclientmandate.views
import uk.gov.hmrc.play.frontend.auth.{AuthContext, Actions}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.frontend.controller.FrontendController
import play.api.i18n.Messages.Implicits._
import play.api.Play.current
import uk.gov.hmrc.play.http.HeaderCarrier
import scala.concurrent.Future

object CollectEmailController extends CollectEmailController {
  val authConnector: AuthConnector = FrontendAuthConnector
  val dataCacheService: DataCacheService = DataCacheService
  val emailService: EmailService = EmailService
}

trait CollectEmailController extends FrontendController with Actions with MandateConstants {

  def dataCacheService: DataCacheService

  def emailService: EmailService

  val backLinkId = "CollectEmailController:BackLink"
  def edit(service: String) = AuthorisedFor(ClientRegime(Some(service)), GGConfidence).async {
    implicit authContext => implicit request =>
      dataCacheService.cacheFormData[String](backLinkId, "").flatMap { cache =>
        showView(service, Some("edit"))
      }
  }

  def view(service: String, backLinkUrl: Option[String]) = AuthorisedFor(ClientRegime(Some(service)), GGConfidence).async {
    implicit authContext => implicit request =>
      dataCacheService.cacheFormData[String](backLinkId, backLinkUrl.getOrElse("")).flatMap { cache =>
        showView(service, None)
      }
  }

  private def showView(service: String, mode: Option[String])(implicit ac: AuthContext, request: Request[AnyContent]) = {

    for {
      cachedData <- dataCacheService.fetchAndGetFormData[ClientCache](clientFormId)
      backLink <- getBackLink(service, mode)
    } yield {
      val filledForm = cachedData.flatMap(_.email) match {
        case Some(x) => clientEmailForm.fill(x)
        case None => clientEmailForm
      }
      Ok(views.html.client.collectEmail(service, filledForm, None, backLink))
    }
  }

  def submit(service: String, mode: Option[String]) = AuthorisedFor(ClientRegime(Some(service)), GGConfidence).async {
    implicit authContext => implicit request =>
      clientEmailForm.bindFromRequest.fold(
        formWithError =>
          getBackLink(service, mode).map{
            backLink =>
              BadRequest(views.html.client.collectEmail(service, formWithError, mode, backLink))
          },
        data => {
          emailService.validate(data.email) flatMap { isValidEmail =>
            if (isValidEmail) {
              dataCacheService.fetchAndGetFormData[ClientCache](clientFormId) flatMap {
                case Some(x) => dataCacheService.cacheFormData[ClientCache](clientFormId, x.copy(email = Some(data))) flatMap { cachedData =>
                  Future.successful(redirect(service, mode))
                }
                case None => dataCacheService.cacheFormData[ClientCache](clientFormId, ClientCache(email = Some(data))) flatMap { cachedData =>
                  Future.successful(redirect(service, mode))
                }
              }
            } else {
              val errorMsg = Messages("client.collect-email.error.email.invalid-by-email-service")
              val errorForm = clientEmailForm.withError(key = "client-collect-email-form", message = errorMsg).fill(data)
              getBackLink(service, mode).map{
                backLink =>
                  BadRequest(views.html.client.collectEmail(service, errorForm, mode, backLink))
              }
            }
          }
        }
      )
  }

  private def redirect(service: String, mode: Option[String]) = {
    mode match {
      case Some("edit") => Redirect(routes.ReviewMandateController.view(service))
      case _ => Redirect(routes.SearchMandateController.view(service))
    }
  }

  private def getBackLink(service: String, mode: Option[String])(implicit hc: HeaderCarrier, ac: AuthContext, request: Request[AnyContent]) :Future[Option[String]]= {
    for {
      backLink <- dataCacheService.fetchAndGetFormData[String](backLinkId)
    } yield {
      (backLink, mode) match {
        case (Some(backLinkUrl), _) => backLink
        case (_, Some("edit")) => Some(routes.ReviewMandateController.view(service).url)
        case _ => None
      }
    }

  }
}
