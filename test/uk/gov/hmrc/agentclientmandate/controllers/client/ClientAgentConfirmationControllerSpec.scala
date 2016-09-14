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
import uk.gov.hmrc.agentclientmandate.builders.{SessionBuilder, AuthBuilder}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future

class ClientAgentConfirmationControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar {

  "ClientSearchMandateController" must {

    "not return NOT_FOUND at route " when {

      "GET /agent-client-mandate/client-agent-confirmation" in {
        val result = route(FakeRequest(GET, "/agent-client-mandate/client-agent-confirmation")).get
        status(result) mustNot be(NOT_FOUND)
      }

    }

    "redirect to login page for UNAUTHENTICATED client" when {

      "client requests(GET) for agent confirm view" in {
        agentConfirmUnAuthenticatedClient { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).get must include("/gg/sign-in")
        }
      }

    }

    "redirect to unauthorised page for UNAUTHORISED client" when {

      "client requests(GET) for agent confirm view" in {
        agentConfirmUnAuthorisedClient { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).get must include("/gg/sign-in")
        }
      }

    }

    "return search mandate view for AUTHORISED client" when {

      "client requests(GET) for agent confirm view" in {
        agentConfirmAuthorisedClient { result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))
          document.title() must be("What happens next?")
          document.getElementById("notification").text() must be("Your agent will receive an email notification.")
          document.getElementById("heading").text() must be("What happens next?")
          document.getElementById("list").text() must include("Your agent has 28 days to accept the request your request by accessing the ATED service.")
          document.getElementById("finish_btn").text() must be("Finish and sign out")
        }
      }

    }

  }

  val mockAuthConnector = mock[AuthConnector]

  object TestClientAgentConfirmationController extends ClientAgentConfirmationController {
    val authConnector = mockAuthConnector
  }

  def agentConfirmUnAuthenticatedClient(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val hc: HeaderCarrier = HeaderCarrier()
    AuthBuilder.mockUnAuthenticatedClient(userId, mockAuthConnector)
    val result = TestClientAgentConfirmationController.clientAgentConfirmation().apply(SessionBuilder.buildRequestWithSessionNoUser)
    test(result)
  }



  def agentConfirmUnAuthorisedClient(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val hc: HeaderCarrier = HeaderCarrier()
    implicit val user = AuthBuilder.createInvalidAuthContext(userId, "name")
    AuthBuilder.mockUnAuthorisedClient(userId, mockAuthConnector)
    val result = TestClientAgentConfirmationController.clientAgentConfirmation().apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def agentConfirmAuthorisedClient(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val hc: HeaderCarrier = HeaderCarrier()
    implicit val user = AuthBuilder.createOrgAuthContext(userId, "name")
    AuthBuilder.mockAuthorisedClient(userId, mockAuthConnector)
    val result = TestClientAgentConfirmationController.clientAgentConfirmation().apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }


}
