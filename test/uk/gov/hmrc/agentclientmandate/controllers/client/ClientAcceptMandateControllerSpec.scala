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
import play.api.mvc.{AnyContentAsFormUrlEncoded, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.agentclientmandate.builders.{AuthBuilder, SessionBuilder}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future

class ClientAcceptMandateControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar {

  "ClientAcceptMandateController" must {

    "not return NOT_FOUND at route " when {

      "GET /agent-client-mandate/client-search-mandate" in {
        val result = route(FakeRequest(GET, "/agent-client-mandate/client-approve-mandate")).get
        status(result) mustNot be(NOT_FOUND)
      }

      "POST /agent-client-mandate/client-search-mandate" in {
        val result = route(FakeRequest(POST, "/agent-client-mandate/client-approve-mandate")).get
        status(result) mustNot be(NOT_FOUND)
      }

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

    "return search mandate view for AUTHORISED client" when {

      "client requests(GET) for search mandate view" in {
        approveAuthorisedClient { result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))
          document.title() must be("Approve your mandate")
          document.getElementById("header").text() must be("Approve your mandate")
          document.getElementById("approved_legend").text() must be("Do you approve your mandate?")
          document.getElementById("submit").text() must be("Submit")
        }
      }

    }

    "returns OK" when {
      "valid form is submitted" in {
        val fakeRequest = FakeRequest().withFormUrlEncodedBody("approved" -> "true")
        submitMandateAuthorisedClient(fakeRequest) { result =>
          status(result) must be(OK)
        }
      }
    }

    "returns BAD_REQUEST" when {
      "invalid form is submitted" in {
        val fakeRequest = FakeRequest().withFormUrlEncodedBody("approved" -> "")
        submitMandateAuthorisedClient(fakeRequest) { result =>
          status(result) must be(BAD_REQUEST)
          val document = Jsoup.parse(contentAsString(result))
          document.getElementsByClass("error-list").text() must include("There is a problem with the mandate question")
          document.getElementsByClass("error-notification").text() must include("You must answer mandate number question")
        }
      }
    }

  }

  val mockAuthConnector = mock[AuthConnector]

  object TestClientAcceptMandateController extends ClientAcceptMandateController {
    val authConnector = mockAuthConnector
  }

  def approveUnAuthenticatedClient(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val hc: HeaderCarrier = HeaderCarrier()
    AuthBuilder.mockUnAuthenticatedClient(userId, mockAuthConnector)
    val result = TestClientAcceptMandateController.approve().apply(SessionBuilder.buildRequestWithSessionNoUser)
    test(result)
  }

  def approveUnAuthorisedClient(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val hc: HeaderCarrier = HeaderCarrier()
    implicit val user = AuthBuilder.createInvalidAuthContext(userId, "name")
    AuthBuilder.mockUnAuthorisedClient(userId, mockAuthConnector)
    val result = TestClientAcceptMandateController.approve().apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def approveAuthorisedClient(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val hc: HeaderCarrier = HeaderCarrier()
    implicit val user = AuthBuilder.createOrgAuthContext(userId, "name")
    AuthBuilder.mockAuthorisedClient(userId, mockAuthConnector)
    val result = TestClientAcceptMandateController.approve().apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def submitMandateAuthorisedClient(request: FakeRequest[AnyContentAsFormUrlEncoded])(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val hc: HeaderCarrier = HeaderCarrier()
    implicit val user = AuthBuilder.createOrgAuthContext(userId, "name")
    AuthBuilder.mockAuthorisedClient(userId, mockAuthConnector)
    val result = TestClientAcceptMandateController.submit().apply(SessionBuilder.updateRequestFormWithSession(request, userId))
    test(result)
  }

}
