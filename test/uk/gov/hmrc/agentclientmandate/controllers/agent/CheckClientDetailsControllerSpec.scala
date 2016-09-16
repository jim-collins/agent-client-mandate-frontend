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

package uk.gov.hmrc.agentclientmandate.controllers.agent

import java.util.UUID

import org.jsoup.Jsoup
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.agentclientmandate.builders.{AuthBuilder, SessionBuilder}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future


class CheckClientDetailsControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with BeforeAndAfterEach {

  "CheckClientDetailsControllerSpec" must {

    "not return NOT_FOUND at route " when {
      "GET /agent-client-details/agent-client-details" in {
        val result = route(FakeRequest(GET, "/agent-client-mandate/agent-client-details")).get
        status(result) mustNot be(NOT_FOUND)
      }

    }

    "return check client details view for AUTHORISED agent" when {

      "client requests(GET) for check client details view" in {
        checkClientDetailsAuthorisedAgent { result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))
          document.title() must be("Check your clients details")
          document.getElementById("header").text must be("Add a client Check your clients details")
          document.getElementById("your-email").text must be("Your email address")
          document.getElementById("registered-name").text must be("Registered name")
          document.getElementById("utr").text must be("Unique tax reference")
          document.getElementById("submit").text must be("Confirm and add client")
        }
      }

    }

    "redirect client details view for UNAUTHORISED agent" when {

      "agent requests(GET) for 'what is your email address' view" in {
        checkClientDetailsUnAuthorisedAgent { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).get must include("/gg/sign-in")
        }
      }

    }

  }

  val mockAuthConnector = mock[AuthConnector]

  object TestCheckClientDetailsController extends CheckClientDetailsController {
    override val authConnector = mockAuthConnector
  }

  override def beforeEach() = {
    reset(mockAuthConnector)
  }


  def checkClientDetailsAuthorisedAgent(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val hc: HeaderCarrier = HeaderCarrier()
    implicit val user = AuthBuilder.createRegisteredAgentAuthContext(userId, "name")
    AuthBuilder.mockAuthorisedAgent(userId, mockAuthConnector)
    val result = TestCheckClientDetailsController.checkClientDetails().apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def checkClientDetailsUnAuthorisedAgent(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val hc: HeaderCarrier = HeaderCarrier()
    implicit val user = AuthBuilder.createRegisteredAgentAuthContext(userId, "name")
    AuthBuilder.mockUnAuthorisedAgent(userId, mockAuthConnector)
    val result = TestCheckClientDetailsController.checkClientDetails().apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

}
