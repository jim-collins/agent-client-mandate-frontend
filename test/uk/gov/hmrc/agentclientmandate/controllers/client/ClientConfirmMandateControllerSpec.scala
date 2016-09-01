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

class ClientConfirmMandateControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar {

  "ClientConfirmMandateController" must {

    "not return NOT_FOUND at route " when {

      "GET /agent-client-mandate/client-accepted-mandate" in {
        val result = route(FakeRequest(GET, "/agent-client-mandate/client-accepted-mandate")).get
        status(result) mustNot be(NOT_FOUND)
      }

      "GET /agent-client-mandate/client-rejected-mandate" in {
        val result = route(FakeRequest(GET, "/agent-client-mandate/client-rejected-mandate")).get
        status(result) mustNot be(NOT_FOUND)
      }


      //      "POST /agent-client-mandate/client-search-mandate" in {
      //        val result = route(FakeRequest(POST, "/agent-client-mandate/client-approve-mandate")).get
      //        status(result) mustNot be(NOT_FOUND)
      //      }


    }

    "redirect to login page for UNAUTHENTICATED client" when {

      "client requests(GET) for search mandate view" in {
        approveUnAuthenticatedClient { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).get must include("/gg/sign-in")
        }
      }

    }

    "redirect to unauthorised page for UNAUTHORISED client" when {

      "client requests(GET) for search mandate view" in {
        approveUnAuthenticatedClient { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).get must include("/gg/sign-in")
        }
      }

    }

    "return confirm mandate view for AUTHORISED client" when {

      "client requests(GET) for search mandate view" in {
        approveAuthorisedClient { result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))
          document.title() must be("Mandate confirmation")
          document.getElementById("header").text() must be("Mandate confirmation")
        }
      }

    }

    "return confirm reject mandate view for AUTHORISED client" when {

      "client requests(GET) for search mandate view" in {
        rejectAuthorisedClient { result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))
          document.title() must be("Mandate reject confirmation")
          document.getElementById("header").text() must be("Mandate reject confirmation")
        }
      }

    }

  }

  val mockAuthConnector = mock[AuthConnector]

  object TestClientConfirmMandateController extends ClientConfirmMandateController {
    val authConnector = mockAuthConnector
  }

  def approveUnAuthenticatedClient(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val hc: HeaderCarrier = HeaderCarrier()
    AuthBuilder.mockUnAuthenticatedClient(userId, mockAuthConnector)
    val result = TestClientConfirmMandateController.accepted().apply(SessionBuilder.buildRequestWithSessionNoUser)
    test(result)
  }

  def approveAuthorisedClient(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val hc: HeaderCarrier = HeaderCarrier()
    implicit val user = AuthBuilder.createOrgAuthContext(userId, "name")
    AuthBuilder.mockAuthorisedClient(userId, mockAuthConnector)
    val result = TestClientConfirmMandateController.accepted().apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def rejectAuthorisedClient(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val hc: HeaderCarrier = HeaderCarrier()
    implicit val user = AuthBuilder.createOrgAuthContext(userId, "name")
    AuthBuilder.mockAuthorisedClient(userId, mockAuthConnector)
    val result = TestClientConfirmMandateController.accepted().apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }


}
