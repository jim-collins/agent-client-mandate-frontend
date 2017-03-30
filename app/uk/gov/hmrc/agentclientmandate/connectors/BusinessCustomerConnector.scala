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

package uk.gov.hmrc.agentclientmandate.connectors


import play.api.Logger
import play.api.http.Status._
import play.api.i18n.Messages.Implicits._
import play.api.Play.current
import play.api.i18n.Messages
import play.api.libs.json.Json
import uk.gov.hmrc.agentclientmandate.config.WSHttp
import uk.gov.hmrc.agentclientmandate.models.UpdateRegistrationDetailsRequest
import uk.gov.hmrc.agentclientmandate.utils.AuthUtils
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http.{HeaderCarrier, _}
import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.Future

trait BusinessCustomerConnector extends ServicesConfig with RawResponseReads {

  def serviceUrl: String

  def baseUri: String

  def registerUri: String

  def updateRegistrationDetailsURI: String

  def knownFactsUri: String

  def http: HttpGet with HttpPost

  def updateRegistrationDetails(safeId: String, updateRegistrationDetails: UpdateRegistrationDetailsRequest)
                               (implicit hc: HeaderCarrier, ac: AuthContext): Future[HttpResponse] = {
    val authLink = AuthUtils.getAuthLink
    val postUrl = s"""$serviceUrl$authLink/$baseUri/$updateRegistrationDetailsURI/$safeId"""
    val jsonData = Json.toJson(updateRegistrationDetails)
    http.POST(postUrl, jsonData) map { response =>
      response.status match {
        case OK => response
        case NOT_FOUND =>
          Logger.warn(s"[BusinessCustomerConnector][updateRegistrationDetails] - Not Found Exception ${updateRegistrationDetails.organisation.map(_.organisationName)}")
          throw new InternalServerException(s"${Messages("bc.connector.error.not-found")}  Exception ${response.body}")
        case SERVICE_UNAVAILABLE =>
          Logger.warn(s"[BusinessCustomerConnector][updateRegistrationDetails] - Service Unavailable Exception ${updateRegistrationDetails.organisation.map(_.organisationName)}")
          throw new ServiceUnavailableException(s"${Messages("bc.connector.error.service-unavailable")}  Exception ${response.body}")
        case status =>
          Logger.warn(s"[BusinessCustomerConnector][updateRegistrationDetails] - $status Exception ${updateRegistrationDetails.organisation.map(_.organisationName)}")
          throw new InternalServerException(s"${Messages("bc.connector.error.unknown-response", status)}  Exception ${response.body}")
      }
    }
  }

}

object BusinessCustomerConnector extends BusinessCustomerConnector {
  // $COVERAGE-OFF$
  val serviceUrl = baseUrl("business-customer")
  val baseUri = "business-customer"
  val registerUri = "register"
  val knownFactsUri = "known-facts"
  val updateRegistrationDetailsURI = "update"
  val http: HttpGet with HttpPost = WSHttp
  // $COVERAGE-ON$
}
