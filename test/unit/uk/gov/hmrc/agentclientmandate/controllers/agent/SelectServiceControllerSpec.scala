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
import uk.gov.hmrc.agentclientmandate.controllers.agent.SelectServiceController
import uk.gov.hmrc.agentclientmandate.utils.{FeatureSwitch, MandateFeatureSwitches}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.http.HeaderCarrier
import unit.uk.gov.hmrc.agentclientmandate.builders.{AuthBuilder, SessionBuilder}

import scala.concurrent.Future

class SelectServiceControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with BeforeAndAfterEach {

  "SelectServiceController" must {

    "not return NOT_FOUND at route " when {

      "GET /mandate/agent/service" in {
        val result = route(FakeRequest(GET, s"/mandate/agent/service")).get
        status(result) mustNot be(NOT_FOUND)
      }

      "POST /mandate/agent/service" in {
        val result = route(FakeRequest(POST, s"/mandate/agent/service")).get
        status(result) mustNot be(NOT_FOUND)
      }

    }

    "redirect to login page for UNAUTHENTICATED agent" when {

      "agent requests(GET) for 'select service question' view" in {
        viewWithUnAuthenticatedAgent { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).get must include("/gg/sign-in")
        }
      }

    }

    "redirect to unauthorised page for UNAUTHORISED agent" when {

      "agent requests(GET) for 'select service question' view" in {
        viewWithUnAuthorisedAgent { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).get must include("/gg/sign-in")
        }
      }

    }

    "return 'select service question' view for AUTHORISED agent" when {

      "agent requests(GET) for 'select service question' view and single service feature is disabled" in {
        FeatureSwitch.disable(MandateFeatureSwitches.singleService)
        viewWithAuthorisedAgent { result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))
          document.title() must be("Select a service")
          document.getElementById("header").text() must include("Select a service")
          document.getElementById("pre-header").text() must be("Add a client")
          document.getElementById("service_legend").text() must be("Select a service")
          document.getElementById("submit").text() must be("Submit")
        }
      }

    }

    "redirect to 'summary page for ated' view for AUTHORISED agent" when {

      "agent requests(GET) for 'select service question' view and single service feature is enabled" in {
        viewWithAuthorisedAgent { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some("/mandate/agent/summary/ated"))
        }
      }

    }

    "redirect to 'agent summary page for service' Page" when {
      "valid form is submitted" in {
        val fakeRequest = FakeRequest().withFormUrlEncodedBody("service" -> "ated")
        submitWithAuthorisedAgent(fakeRequest) { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some("/mandate/agent/summary/ated"))
        }
      }
    }

    "returns BAD_REQUEST" when {
      "invalid form is submitted" in {
        val fakeRequest = FakeRequest().withFormUrlEncodedBody("service" -> "")
        submitWithAuthorisedAgent(fakeRequest) { result =>
          status(result) must be(BAD_REQUEST)
          val document = Jsoup.parse(contentAsString(result))
          document.getElementsByClass("error-list").text() must include("There is a problem with the select service question")
          document.getElementsByClass("error-notification").text() must include("You must select one service")
        }
      }
    }

  }

  val mockAuthConnector = mock[AuthConnector]

  object TestSelectServiceController extends SelectServiceController {
    override val authConnector = mockAuthConnector
  }

  override def beforeEach(): Unit = {
    reset(mockAuthConnector)
    FeatureSwitch.enable(MandateFeatureSwitches.singleService)
  }

  def viewWithUnAuthenticatedAgent(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val hc: HeaderCarrier = HeaderCarrier()
    AuthBuilder.mockUnAuthenticatedClient(userId, mockAuthConnector)
    val result = TestSelectServiceController.view().apply(SessionBuilder.buildRequestWithSessionNoUser)
    test(result)
  }

  def viewWithUnAuthorisedAgent(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val hc: HeaderCarrier = HeaderCarrier()
    implicit val user = AuthBuilder.createInvalidAuthContext(userId, "name")
    AuthBuilder.mockUnAuthorisedAgent(userId, mockAuthConnector)
    val result = TestSelectServiceController.view().apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def viewWithAuthorisedAgent(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val hc: HeaderCarrier = HeaderCarrier()
    implicit val user = AuthBuilder.createOrgAuthContext(userId, "name")
    AuthBuilder.mockAuthorisedAgent(userId, mockAuthConnector)
    val result = TestSelectServiceController.view().apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def submitWithAuthorisedAgent(request: FakeRequest[AnyContentAsFormUrlEncoded])(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val hc: HeaderCarrier = HeaderCarrier()
    implicit val user = AuthBuilder.createRegisteredAgentAuthContext(userId, "name")
    AuthBuilder.mockAuthorisedAgent(userId, mockAuthConnector)
    val result = TestSelectServiceController.submit().apply(SessionBuilder.updateRequestFormWithSession(request, userId))
    test(result)
  }

}
