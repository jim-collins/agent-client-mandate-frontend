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
import play.api.test.FakeRequest
import play.api.mvc.Result
import play.api.test.Helpers._
import uk.gov.hmrc.agentclientmandate.builders.{AuthBuilder, SessionBuilder}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future

class AgentClientSummaryControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with BeforeAndAfterEach{


  "AgentClientSummaryController" must{
    "not return NOT_FOUND at route " when{
      "Get /mandate/agent/agent-client-summary" in{
        val result = route(FakeRequest(GET, "/mandate/agent/agent-client-summary")).get
        status(result) mustNot be(NOT_FOUND)
      }

    }

    "return check client details view for agent" when {

      "client requests(GET) for check client details view" in {
        viewAuthorisedAgent { result =>

          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))
          document.title() must be("Your ATED clients")
          document.getElementById("header").text must be("Your ATED clients")
          document.getElementById("add-client-reveal").text() must be("How to add a client")
          document.getElementById("happens-next-text").text() must be("Your client will then need to:")
          document.getElementById("happens-next-point1").text() must be("Register their company to use the new ATED service, they may need to create a new organisational Government Gateway account.")
          document.getElementById("happens-next-point2").text() must be("Enter the agent reference you gave them.")
          document.getElementById("happens-next-point3").text() must be("Let you know once they have completed this process.")
          document.getElementById("happens-next-notification").text() must be("You will have 28 days to sign in and accept the agent request. You will not receive an email notification.")
        }
        }
      }

    }

  val mockAuthConnector = mock[AuthConnector]

  object TestAgentClientSummaryController extends AgentClientSummaryController {
    override val authConnector = mockAuthConnector
  }

  override def beforeEach() = {
    reset(mockAuthConnector)
  }




def viewAuthorisedAgent(test: Future[Result] => Any) {
  val userId = s"user-${UUID.randomUUID}"
  implicit val hc: HeaderCarrier = HeaderCarrier()
  implicit val user = AuthBuilder.createRegisteredAgentAuthContext(userId, "name")
  AuthBuilder.mockAuthorisedAgent(userId, mockAuthConnector)
  val result = TestAgentClientSummaryController.view().apply(SessionBuilder.buildRequestWithSession(userId))
  test(result)
}
}
