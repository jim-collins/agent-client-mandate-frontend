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

package uk.gov.hmrc.agentclientmandate.controllers.auth

import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import uk.gov.hmrc.agentclientmandate.builders.AuthBuilder
import uk.gov.hmrc.play.frontend.auth.connectors.domain.Accounts

class AgentRegimeSpec extends PlaySpec with OneServerPerSuite with MockitoSugar {

  "AgentRegime" must {

    "extend TaxRegime" when {

      "overriding isAuthorised - when loggedInUser has registered agent account - return true" in {
        when(accounts.org).thenReturn(None)
        val registeredAgent = AuthBuilder.createRegisteredAgentAuthContext("userId", "userName")
        when(accounts.agent).thenReturn(registeredAgent.principal.accounts.agent)
        AgentRegime.isAuthorised(accounts) must be(true)
      }

      "overriding isAuthorised - when loggedInUser doesn't have agent account - return false" in {
        when(accounts.org).thenReturn(None)
        when(accounts.agent).thenReturn(None)
        AgentRegime.isAuthorised(accounts) must be(false)
      }

      "overriding authenticationType" in {
        AgentRegime.authenticationType must be(AgentGovernmentGateway)
      }

      "overriding unauthorised page" in {
        AgentRegime.unauthorisedLandingPage must be(None)
      }

    }

  }

  val accounts = mock[Accounts](RETURNS_DEEP_STUBS)

}
