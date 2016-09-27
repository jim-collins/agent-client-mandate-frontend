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
import uk.gov.hmrc.agentclientmandate.utils.MandateConstants
import uk.gov.hmrc.agentclientmandate.viewModelsAndForms.{AgentEmail, ClientCache}
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


trait AgentClientMandateService extends MandateConstants {

  def dataCacheService: DataCacheService

  def agentClientMandateConnector: AgentClientMandateConnector

  def createMandate(service: String)(implicit hc: HeaderCarrier, ac: AuthContext): Future[String] = {
    dataCacheService.fetchAndGetFormData[AgentEmail](agentEmailFormId) flatMap {
      case Some(cachedEmail) =>
        val mandateDto = CreateMandateDto(cachedEmail.email, service)
        agentClientMandateConnector.createMandate(mandateDto) flatMap {
          response => response.status match {
            case CREATED =>
              val mandateId = (response.json \ "mandateId").as[String]
              dataCacheService.clearCache() flatMap { clearCacheResponse =>
                dataCacheService.cacheFormData[String](agentRefCacheId, mandateId) flatMap { cachingResponse =>
                  Future.successful(mandateId)
                }
              }
            case status => throw new RuntimeException("Mandate not created")
          }
        }
      case None => throw new RuntimeException("Email not found in cache")
    }
  }

  def fetchClientMandate(mandateId: String)(implicit hc: HeaderCarrier, ac: AuthContext): Future[Option[Mandate]] = {
    agentClientMandateConnector.fetchMandate(mandateId) map {
      response => response.status match {
        case OK => response.json.asOpt[Mandate]
        case status => None
      }
    }
  }

  def approveMandate(mandate: Mandate)(implicit hc: HeaderCarrier, ac: AuthContext): Future[Option[Mandate]] = {
     agentClientMandateConnector.approveMandate(mandate) flatMap {
       response => response.status match {
         case OK =>
           val mandate = response.json.asOpt[Mandate]
           dataCacheService.clearCache() flatMap { clearCacheRep =>
             dataCacheService.cacheFormData[ClientCache](clientFormId, ClientCache(mandate = mandate)) flatMap { cacheResp =>
               Future.successful(mandate)
             }
           }
         case status => Future.successful(None)
       }
     }
  }

}

object AgentClientMandateService extends AgentClientMandateService {
  val dataCacheService = DataCacheService
  val agentClientMandateConnector = AgentClientMandateConnector
}
