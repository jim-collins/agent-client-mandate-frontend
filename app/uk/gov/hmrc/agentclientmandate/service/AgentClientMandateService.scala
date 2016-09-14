/*
 * Copyright 2016 HM Revenue & Customs
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

package uk.gov.hmrc.agentclientmandate.service

import play.api.http.Status._
import uk.gov.hmrc.agentclientmandate.connectors.AgentClientMandateConnector
import uk.gov.hmrc.agentclientmandate.models._
import uk.gov.hmrc.agentclientmandate.viewModelsAndForms.AgentEmail
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


trait AgentClientMandateService {

  def dataCacheService: DataCacheService

  def agentClientMandateConnector: AgentClientMandateConnector

  def formId: String

  def createMandate(service: String)(implicit hc: HeaderCarrier, ac: AuthContext): Future[Option[ClientMandate]] = {
    dataCacheService.fetchAndGetFormData[AgentEmail](formId) flatMap {
      case Some(cachedEmail) =>
        //TODO: Change exception message
        val arn = ac.principal.accounts.agent.flatMap(_.agentBusinessUtr.map(_.utr)).
          getOrElse(throw new RuntimeException("No valid agent business UTR found!"))
        val agentName = ac.principal.name.getOrElse("")
        val email = cachedEmail.email
        val partyDto = PartyDto(id = arn, name = agentName, `type` = "Agent")
        val serviceDto = ServiceDto(service)
        val contactDto = ContactDetailsDto(email, "")
        val mandateDto = ClientMandateDto(partyDto, contactDto, serviceDto)
        agentClientMandateConnector.createMandate(mandateDto) map {
          response => response.status match {
            case OK => response.json.asOpt[ClientMandate]
            case status => None
          }
        }
      case None => Future.successful(None)
    }
  }

  def fetchClientMandate(mandateId: String)(implicit hc: HeaderCarrier, ac: AuthContext): Future[Option[ClientMandate]] = {
    agentClientMandateConnector.fetchMandate(mandateId) map {
      response => response.status match {
        case OK => response.json.asOpt[ClientMandate]
        case status => None
      }
    }
  }

}

object AgentClientMandateService extends AgentClientMandateService {
  val dataCacheService = DataCacheService
  val agentClientMandateConnector = AgentClientMandateConnector
  val formId = "agent-email"
}
