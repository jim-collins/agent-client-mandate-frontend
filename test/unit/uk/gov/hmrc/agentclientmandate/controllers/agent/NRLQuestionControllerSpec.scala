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

package unit.uk.gov.hmrc.agentclientmandate.controllers.agent

import java.util.UUID

import org.jsoup.Jsoup
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.mvc.{AnyContentAsFormUrlEncoded, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.agentclientmandate.builders.{AuthBuilder, SessionBuilder}
import uk.gov.hmrc.agentclientmandate.controllers.agent.NRLQuestionController
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future

class NRLQuestionControllerSpec extends PlaySpec with OneServerPerSuite with BeforeAndAfterEach with MockitoSugar {

  "NRLQuestionController" must {

    "not return NOT_FOUND at route " when {

      "GET /mandate/agent/overseas-client-question/:service" in {
        val result = route(FakeRequest(GET, s"/mandate/agent/nrl-question/$service")).get
        status(result) mustNot be(NOT_FOUND)
      }

      "POST /mandate/agent/overseas-client-question/:service" in {
        val result = route(FakeRequest(POST, s"/mandate/agent/nrl-question/$service")).get
        status(result) mustNot be(NOT_FOUND)
      }

    }

    "redirect to login page for UNAUTHENTICATED agent" when {
      "agent requests(GET) for 'nrl question' view" in {
        viewWithUnAuthenticatedAgent { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).get must include("/gg/sign-in")
        }
      }
    }

    "redirect to unauthorised page for UNAUTHORISED agent" when {
      "agent requests(GET) for 'nrl question' view" in {
        viewWithUnAuthorisedAgent { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).get must include("/gg/sign-in")
        }
      }
    }

    "return 'nrl question' view for AUTHORISED agent" when {
      "agent requests(GET) for 'nrl question' view" in {
        viewWithAuthorisedAgent { result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))
          document.title() must be("Is your client a non-resident landlord?")
          document.getElementById("header").text() must include("Is your client a non-resident landlord?")
          document.getElementById("pre-header").text() must be("Add a client")
          document.getElementById("paysSA_legend").text() must be("Is your client a non-resident landlord?")
          document.getElementById("submit").text() must be("Submit")
        }
      }
    }

    "redirect agent to 'client permission' page" when {
      "valid form is submitted and YES is selected as client pays self-assessment" in {
        val fakeRequest = FakeRequest().withFormUrlEncodedBody("paysSA" -> "true")
        submitWithAuthorisedAgent(fakeRequest) { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(s"/mandate/agent/details/$service"))
        }
      }
    }

    "redirect agent to 'enter client non-uk details' page in business-customer-frontend application" when {
      "valid form is submitted and NO is selected as client pays self-assessment" in {
        val fakeRequest = FakeRequest().withFormUrlEncodedBody("paysSA" -> "false")
        submitWithAuthorisedAgent(fakeRequest) { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some("http://localhost:9923/business-customer/registration/non-uk/nrl/ated"))
        }
      }
    }

    "returns BAD_REQUEST" when {
      "invalid form is submitted" in {
        val fakeRequest = FakeRequest().withFormUrlEncodedBody("paysSA" -> "")
        submitWithAuthorisedAgent(fakeRequest) { result =>
          status(result) must be(BAD_REQUEST)
          val document = Jsoup.parse(contentAsString(result))
          document.getElementsByClass("error-list").text() must include("There is a problem with the non-resident landlord question.")
          document.getElementsByClass("error-notification").text() must include("You must answer non-resident landlord question")
        }
      }
    }

  }

  val mockAuthConnector = mock[AuthConnector]
  val service = "ATED"

  object TestNRLQuestionController extends NRLQuestionController {
    override val authConnector = mockAuthConnector
  }

  override def beforeEach(): Unit = {
    reset(mockAuthConnector)
  }

  def viewWithUnAuthenticatedAgent(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val hc: HeaderCarrier = HeaderCarrier()
    AuthBuilder.mockUnAuthenticatedClient(userId, mockAuthConnector)
    val result = TestNRLQuestionController.view(service).apply(SessionBuilder.buildRequestWithSessionNoUser)
    test(result)
  }

  def viewWithUnAuthorisedAgent(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val hc: HeaderCarrier = HeaderCarrier()
    implicit val user = AuthBuilder.createInvalidAuthContext(userId, "name")
    AuthBuilder.mockUnAuthorisedAgent(userId, mockAuthConnector)
    val result = TestNRLQuestionController.view(service).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def viewWithAuthorisedAgent(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val hc: HeaderCarrier = HeaderCarrier()
    implicit val user = AuthBuilder.createOrgAuthContext(userId, "name")
    AuthBuilder.mockAuthorisedAgent(userId, mockAuthConnector)
    val result = TestNRLQuestionController.view(service).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def submitWithAuthorisedAgent(request: FakeRequest[AnyContentAsFormUrlEncoded])(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val hc: HeaderCarrier = HeaderCarrier()
    implicit val user = AuthBuilder.createRegisteredAgentAuthContext(userId, "name")
    AuthBuilder.mockAuthorisedAgent(userId, mockAuthConnector)
    val result = TestNRLQuestionController.submit(service).apply(SessionBuilder.updateRequestFormWithSession(request, userId))
    test(result)
  }

}
