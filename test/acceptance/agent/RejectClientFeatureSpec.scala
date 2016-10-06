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

package acceptance.agent

import java.util.UUID

import org.joda.time.DateTime
import org.jsoup.Jsoup
import org.mockito.Matchers
import org.scalatest.{BeforeAndAfterEach, FeatureSpec, GivenWhenThen}
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.OneServerPerSuite
import play.api.mvc.Result
import play.api.test.Helpers._
import uk.gov.hmrc.agentclientmandate.builders.{AuthBuilder, SessionBuilder}
import uk.gov.hmrc.agentclientmandate.controllers.agent.RejectClientController
import uk.gov.hmrc.agentclientmandate.models._
import uk.gov.hmrc.agentclientmandate.service.AgentClientMandateService
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future

class RejectClientFeatureSpec extends FeatureSpec with OneServerPerSuite with MockitoSugar with BeforeAndAfterEach with GivenWhenThen{

  feature("The user can view the reject client page") {

    info("as a user I want to view the correct page content")

    scenario("user has visited the page") {

      org.mockito.Mockito.when(mockAgentClientMandateService.fetchClientMandate(Matchers.any())(Matchers.any(), Matchers.any())) thenReturn Future.successful(Some(mandate))
      viewWithAuthorisedAgent { result =>
        Given("A user visits the page")
        When("The user views the page")
        val document = Jsoup.parse(contentAsString(result))
        Then("The title should match - Are you sure you want to reject the request from ACME Limited?")
        assert(document.title() === "Are you sure you want to reject the request from ACME Limited?")
        And("The pre-header text is - Manage your ATED service")
        assert(document.getElementById("pre-heading").text() === "Manage your ATED service")
        And("The header text is - Are you sure you want to reject the request from ACME Limited?")
        assert(document.getElementById("heading").text() === "Are you sure you want to reject the request from ACME Limited?")
      }
    }
  }

  def viewWithAuthorisedAgent(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val hc: HeaderCarrier = HeaderCarrier()
    implicit val user = AuthBuilder.createOrgAuthContext(userId, "name")
    AuthBuilder.mockAuthorisedAgent(userId, mockAuthConnector)
    val result = TestRejectClientController.view("ATED", "1").apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  object TestRejectClientController extends RejectClientController {
    override val authConnector = mockAuthConnector
    override val acmService = mockAgentClientMandateService
  }

  val mockAgentClientMandateService = mock[AgentClientMandateService]
  val mockAuthConnector = mock[AuthConnector]

  val mandate = Mandate(id = "1", createdBy = User("credId", "agentName", Some("agentCode")), None, None, agentParty = Party("JARN123456", "agency name", PartyType.Organisation, ContactDetails("agent@agent.com", None)), clientParty = Some(Party("JARN123456", "ACME Limited", PartyType.Organisation, ContactDetails("client@client.com", None))), currentStatus = MandateStatus(Status.New, DateTime.now(), "credId"), statusHistory = Nil, Subscription(None, Service("ated", "ATED")))
}
