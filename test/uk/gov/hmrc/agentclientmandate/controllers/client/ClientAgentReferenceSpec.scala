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

package uk.gov.hmrc.agentclientmandate.controllers.client

import java.util.UUID

import org.jsoup.Jsoup
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.agentclientmandate.builders.{AuthBuilder, SessionBuilder}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future


class ClientAgentReferenceSpec extends PlaySpec with OneServerPerSuite with MockitoSugar {

  "ClientAgentReferenceSpec" must {

    "not return NOT_FOUND at route " when {

      "GET /agent-client-mandate/client-add-email" in {
        val result = route(FakeRequest(GET, "/agent-client-mandate/client-agent-reference")).get
        status(result) mustNot be(NOT_FOUND)
      }

    }

  }

  "redirect to login page for UNAUTHENTICATED client" when {

    "client requests(GET) for search mandate view" in {
      agentRefUnAuthenticatedClient { result =>
        status(result) must be(SEE_OTHER)
        redirectLocation(result).get must include("/gg/sign-in")
      }
    }

  }

  "redirect to unauthorised page for UNAUTHORISED client" when {

    "client requests for agent reference view" in {
      agentRefUnAuthenticatedClient { result =>
        status(result) must be(SEE_OTHER)
        redirectLocation(result).get must include("/gg/sign-in")
      }
    }

  }

  "return agent reference view for AUTHORISED client" when {

    "client requests for agent reference view" in {
      agentRefAuthorisedClient { result =>
        status(result) must be(OK)
        val document = Jsoup.parse(contentAsString(result))
        document.title() must be("What is the agent's reference?")
        document.getElementById("header").text() must include("What is the agent's reference?")
        document.getElementById("submit").text() must be("Continue")
      }
    }

  }

  "redirect to respective page " when {

    "valid form is submitted" in {
      continueWithAuthorisedClient { result =>
        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some("/agent-client-mandate/client-review-agent"))
      }
    }

  }

  val mockAuthConnector = mock[AuthConnector]

  object TestAgentReferenceController extends AgentReferenceController {
    val authConnector = mockAuthConnector
  }

  def agentRefUnAuthenticatedClient(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val hc: HeaderCarrier = HeaderCarrier()
    AuthBuilder.mockUnAuthenticatedClient(userId, mockAuthConnector)
    val result = TestAgentReferenceController.agentReference().apply(SessionBuilder.buildRequestWithSessionNoUser)
    test(result)
  }

  def agentRefAuthorisedClient(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val hc: HeaderCarrier = HeaderCarrier()
    implicit val user = AuthBuilder.createOrgAuthContext(userId, "name")
    AuthBuilder.mockAuthorisedClient(userId, mockAuthConnector)
    val result = TestAgentReferenceController.agentReference().apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def continueWithAuthorisedClient(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val hc: HeaderCarrier = HeaderCarrier()
    AuthBuilder.mockAuthorisedClient(userId, mockAuthConnector)
    val result = TestAgentReferenceController.continue().apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

}