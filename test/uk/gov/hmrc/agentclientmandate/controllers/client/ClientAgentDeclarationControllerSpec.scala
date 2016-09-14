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
import uk.gov.hmrc.agentclientmandate.config.FrontendAuthConnector
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future

class ClientAgentDeclarationControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar {

  "ClientDeclarationControllerSpec" must {


    "use correct authConnector" in {
      ClientAgentDeclarationController.authConnector must be(FrontendAuthConnector)
    }

    "not return NOT_FOUND at route " when {
      "GET /agent-client-mandate/client-review-agent" in {
        val result = route(FakeRequest(GET, "/agent-client-mandate/client-review-agent")).get
        status(result) mustNot be(NOT_FOUND)
      }
    }




    "redirect to login page for UNAUTHENTICATED client" when {

      "client requests(GET) for search mandate view" in {
        clientAgentDeclarationUnAuthenticatedClient { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).get must include("/gg/sign-in")
        }
      }

    }

    "return agent declaration view for AUTHORISED client" when {

      "client requests(GET) for search declaration view" in {
        clientAgentDeclaration { result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))
          document.title() must be("Declaration and consent")
          document.getElementById("header").text() must include("Declaration and consent")
          document.getElementById("pre-heading").text() must include("Appoint an agent")
          document.getElementById("declare-title").text() must be("I declare that:")
          document.getElementById("agent-name").text() must be("the nominated agent [Agent Name] has agreed to act on my behalf in respect of ATED")
          document.getElementById("dec-info").text() must be("that the information I have provided is correct and complete")
          document.getElementById("confirm_btn").text() must be("Continue")
        }
      }
    }

  }

  val mockAuthConnector = mock[AuthConnector]

  object TestClientAgentDeclarationController extends ClientAgentDeclarationController {
    val authConnector = mockAuthConnector
  }

  def clientAgentDeclarationUnAuthenticatedClient(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val hc: HeaderCarrier = HeaderCarrier()
    AuthBuilder.mockUnAuthenticatedClient(userId, mockAuthConnector)
    val result = TestClientAgentDeclarationController.clientAgentDeclaration().apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }


  def clientAgentDeclarationAuthenticatedClient(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val hc: HeaderCarrier = HeaderCarrier()
    AuthBuilder.mockUnAuthenticatedClient(userId, mockAuthConnector)
    val result = TestClientAgentDeclarationController.clientAgentDeclaration().apply(SessionBuilder.buildRequestWithSessionNoUser)
    test(result)
  }


  def clientAgentDeclarationAuthorisedClient(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val hc: HeaderCarrier = HeaderCarrier()
    implicit val user = AuthBuilder.createOrgAuthContext(userId, "name")
    AuthBuilder.mockAuthorisedClient(userId, mockAuthConnector)
    val result = TestClientAgentDeclarationController.clientAgentDeclaration().apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }


  def clientAgentDeclaration(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val hc: HeaderCarrier = HeaderCarrier()
    implicit val user = AuthBuilder.createOrgAuthContext(userId, "name")
    AuthBuilder.mockAuthorisedClient(userId, mockAuthConnector)
    val result = TestClientAgentDeclarationController.clientAgentDeclaration().apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

}