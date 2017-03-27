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

import play.api.Logger
import play.api.i18n.Messages
import play.api.libs.json.Json
import play.api.mvc.{AnyContent, Request}
import uk.gov.hmrc.agentclientmandate.controllers.auth.ClientRegime
import uk.gov.hmrc.agentclientmandate.models.ClientDetails
import uk.gov.hmrc.agentclientmandate.service.{AgentClientMandateService, DataCacheService, EmailService}
import uk.gov.hmrc.agentclientmandate.utils.MandateConstants
import uk.gov.hmrc.agentclientmandate.viewModelsAndForms.ClientEmailForm._
import uk.gov.hmrc.agentclientmandate.views
import uk.gov.hmrc.play.frontend.auth.{Actions, AuthContext}
import uk.gov.hmrc.play.frontend.controller.FrontendController
import uk.gov.hmrc.play.http.{BadRequestException, HeaderCarrier}
import play.api.i18n.Messages.Implicits._
import play.api.Play.current
import uk.gov.hmrc.agentclientmandate.config.FrontendAppConfig._
import uk.gov.hmrc.agentclientmandate.config.FrontendAuthConnector
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector

import scala.concurrent.Future

object EditEmailController extends EditEmailController {
  // $COVERAGE-OFF$
  val authConnector: AuthConnector = FrontendAuthConnector
  val mandateService: AgentClientMandateService = AgentClientMandateService
  val dataCacheService: DataCacheService = DataCacheService
  val emailService: EmailService = EmailService
  // $COVERAGE-ON$
}

trait EditEmailController extends FrontendController with Actions with MandateConstants {

  def emailService: EmailService
  def dataCacheService: DataCacheService
  def mandateService: AgentClientMandateService


  def getClientMandateDetails(clientId: String, service: String, returnUrl: String) = AuthorisedFor(ClientRegime(Some(service)), GGConfidence).async {
    implicit authContext => implicit request => {

      // $COVERAGE-OFF$
      if (Option(returnUrl).isEmpty) throw new BadRequestException("The return url is a mandatory parameter")
      // $COVERAGE-ON$

      mandateService.fetchClientMandateByClient(clientId, service).map {
        case Some(mandate) => mandate.currentStatus.status match {
          case uk.gov.hmrc.agentclientmandate.models.Status.Active =>
            val clientDetails = ClientDetails(mandate.agentParty.name, mandateFrontendHost + routes.RemoveAgentController.view(service, mandate.id, returnUrl).url, mandate.clientParty.get.contactDetails.email, mandateFrontendHost + routes.EditEmailController.view(mandate.id, service, returnUrl).url)
            Ok(Json.toJson(clientDetails))
          case _ => NotFound
        }
        case _ => NotFound
      }
    }
  }


  def view(mandateId: String, service: String, returnUrl: String) = AuthorisedFor(ClientRegime(Some(service)), GGConfidence).async {
    implicit authContext => implicit request =>
      saveBackLink(returnUrl).flatMap { cache =>
        for {
          _ <- dataCacheService.cacheFormData("MANDATE_ID", mandateId)
        } yield {
          Ok(views.html.client.editEmail(service, clientEmailForm, Some(returnUrl)))
        }
      }
  }

  def submit(service: String) = AuthorisedFor(ClientRegime(Some(service)), GGConfidence).async {
    implicit authContext => implicit request =>
      clientEmailForm.bindFromRequest.fold(
        formWithError =>
          getBackLink().map{
            backLink =>
              BadRequest(views.html.client.editEmail(service, formWithError, backLink))
          },
        data => {
          emailService.validate(data.email) flatMap { isValidEmail =>
            if (isValidEmail) {
              for {
                cachedMandateId <- dataCacheService.fetchAndGetFormData[String]("MANDATE_ID")
              } yield {
                mandateService.updateClientEmail(data.email, cachedMandateId.get)
              }
              getBackLink().map {
                backLink =>
                  Redirect(backLink.get)
              }
            } else {
              val errorMsg = Messages("client.edit-email.error.email.invalid-by-email-service")
              val errorForm = clientEmailForm.withError(key = "client-edit-email-form", message = errorMsg).fill(data)
              getBackLink().map{
                backLink =>
                  BadRequest(views.html.client.editEmail(service, errorForm, backLink))
              }
            }
          }
        }
      )
  }

  val backLinkId = "EditEmailController:BackLink"
  private def saveBackLink(redirectUrl: String)(implicit hc: uk.gov.hmrc.play.http.HeaderCarrier) = {
    dataCacheService.cacheFormData[String](backLinkId, redirectUrl)
  }

  private def getBackLink()(implicit hc: HeaderCarrier, ac: AuthContext, request: Request[AnyContent]) :Future[Option[String]]= {
    dataCacheService.fetchAndGetFormData[String](backLinkId).map(backLink =>
      backLink match {
        case Some(x) if (!x.trim.isEmpty) => backLink
        case _ => None
      }
    )
  }

}
