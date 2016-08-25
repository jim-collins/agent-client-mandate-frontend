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

package uk.gov.hmrc.agentclientmandate.builders

import org.mockito.Matchers
import org.mockito.Mockito._
import uk.gov.hmrc.domain._
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.frontend.auth.connectors.domain.{OrgAccount, PayeAccount, _}

import scala.concurrent.Future

object AuthBuilder {

  val nino = new Generator().nextNino

  def createOrgUserAuthority(userId: String): Authority = {
    Authority(
      uri = userId,
      accounts = Accounts(org = Some(OrgAccount(org = Org("123"), link = "/org/123"))),
      loggedInAt = None,
      previouslyLoggedInAt = None,
      credentialStrength = CredentialStrength.Weak,
      confidenceLevel = ConfidenceLevel.L50,
      userDetailsLink = Some("/user-details/1234567890"),
      enrolments = Some("/auth/oid/1234567890/enrolments")
    )
  }

  def createInvalidAuthority(userId: String): Authority = {
    Authority(
      uri = userId,
      accounts = Accounts(paye = Some(PayeAccount(link = s"/paye/${nino.nino}", nino = nino))),
      loggedInAt = None,
      previouslyLoggedInAt = None,
      credentialStrength = CredentialStrength.Weak,
      confidenceLevel = ConfidenceLevel.L50,
      userDetailsLink = None,
      enrolments = None
    )
  }


  def mockAuthorisedClient(userId: String, mockAuthConnector: AuthConnector) {
    when(mockAuthConnector.currentAuthority(Matchers.any())) thenReturn {
      Future.successful(Some(createOrgUserAuthority(userId)))
    }
  }

  def mockUnAuthorisedClient(userId: String, mockAuthConnector: AuthConnector) {
    when(mockAuthConnector.currentAuthority(Matchers.any())) thenReturn {
      Future.successful(Some(createInvalidAuthority(userId)))
    }
  }

  def mockUnAuthenticatedClient(userId: String, mockAuthConnector: AuthConnector) {
    when(mockAuthConnector.currentAuthority(Matchers.any())).thenReturn(Future.successful(None))
  }

}
