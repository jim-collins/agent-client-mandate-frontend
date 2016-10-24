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

import org.joda.time.DateTime
import org.jsoup.Jsoup
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.agentclientmandate.controllers.agent.AgentSummaryController
import uk.gov.hmrc.agentclientmandate.models.{MandateStatus, Service, Status, Subscription, _}
import uk.gov.hmrc.agentclientmandate.service.{AgentClientMandateService, Mandates}
import uk.gov.hmrc.domain.Generator
import uk.gov.hmrc.play.frontend.auth.connectors.{AuthConnector, DelegationConnector}
import uk.gov.hmrc.play.http.HeaderCarrier
import unit.uk.gov.hmrc.agentclientmandate.builders.{AuthBuilder, SessionBuilder}

import scala.concurrent.Future

class AgentSummaryControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with BeforeAndAfterEach {


  "AgentClientSummaryController" must {
    "not return NOT_FOUND at route " when {
      "Get /mandate/agent/summary/:service" in {
        val result = route(FakeRequest(GET, s"/mandate/agent/summary/$service")).get
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
          document.getElementById("add-client-link").text() must be("Add a new client")
          document.getElementById("yourClients-name").text() must be("Name")
          document.getElementById("yourClients-action").text() must be("Action")
          document.getElementById("remove-client").text() must be("Remove test client")
          document.getElementById("client-name-0").text() must be("test client")
          document.getElementById("pending-client-data-0").child(0).text() must be("test client1")
          document.getElementById("pending-client-data-0").child(1).text() must be("Reject")
          document.getElementById("accept-1").text() must be("Accept")
          document.getElementById("pending-client-data-1").child(0).text() must be("test client2")
          document.getElementById("pending-client-data-2").child(0).text() must be("test client3")
          document.getElementById("pending-client-data-3").child(0).text() must be("test client4")
          document.getElementById("pending-client-data-1").child(2).text() must be("Pending")
          document.getElementById("sidebar.agentname").text() must be("ABC Ltd.")
        }
      }
    }

    "redirect to delegated service specific page" when {
      "agent selects and begins delegation on a particular client" in {
        val userId = s"user-${UUID.randomUUID}"
        implicit val hc: HeaderCarrier = HeaderCarrier()
        implicit val user = AuthBuilder.createRegisteredAgentAuthContext(userId, "name")
        AuthBuilder.mockAuthorisedAgent(userId, mockAuthConnector)
        when(mockDelegationConnector.startDelegation(Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(()))
        val result = TestAgentSummaryController.doDelegation(service, atedUtr.utr, "Client-Name").apply(SessionBuilder.buildRequestWithSession(userId))
        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some("http://localhost:9916/ated/account-summary"))
      }
    }

  }

  val mockAuthConnector = mock[AuthConnector]
  val mockAgentClientMandateService = mock[AgentClientMandateService]
  val mockDelegationConnector = mock[DelegationConnector]

  object TestAgentSummaryController extends AgentSummaryController {
    override val authConnector = mockAuthConnector
    override val agentClientMandateService = mockAgentClientMandateService
    override val delegationConnector = mockDelegationConnector
  }

  override def beforeEach() = {
    reset(mockAuthConnector)
    reset(mockAgentClientMandateService)
    reset(mockDelegationConnector)
  }

  val registeredAddressDetails = RegisteredAddressDetails("123 Fake Street", "Somewhere", None, None, None, "GB")
  val agentDetails = AgentDetails("ABC Ltd.", registeredAddressDetails)

  val mandateId = "12345678"
  val time1 = DateTime.now()
  val service = "ATED"
  val atedUtr = new Generator().nextAtedUtr

  val clientParty = Party("12345678", "test client", PartyType.Individual, ContactDetails("a.a@a.com", None))
  val clientParty1 = Party("12345679", "test client1", PartyType.Individual, ContactDetails("aa.aa@a.com", None))
  val clientParty2 = Party("12345671", "test client2", PartyType.Individual, ContactDetails("aa.aa@a.com", None))
  val clientParty3 = Party("12345671", "test client3", PartyType.Individual, ContactDetails("aa.aa@a.com", None))
  val clientParty4 = Party("12345671", "test client4", PartyType.Individual, ContactDetails("aa.aa@a.com", None))

  val mandateNew: Mandate = Mandate(id = mandateId, createdBy = User("credId", "agentName", Some("agentCode")), None, None, agentParty = Party("JARN123456", "agency name", PartyType.Organisation, ContactDetails("agent@agent.com", None)), clientParty = Some(clientParty1), currentStatus = MandateStatus(Status.New, time1, "credId"), statusHistory = Nil, Subscription(None, Service("ated", "ATED")))

  val mandateActive: Mandate = Mandate(id = mandateId, createdBy = User("credId", "agentName", Some("agentCode")), None, None, agentParty = Party("JARN123457", "agency name", PartyType.Organisation, ContactDetails("agent@agent.com", None)), clientParty = Some(clientParty), currentStatus = MandateStatus(Status.Active, time1, "credId"), statusHistory = Seq(MandateStatus(Status.New, time1, "credId")), Subscription(None, Service("ated", "ATED")))

  val mandateApproved: Mandate = Mandate(id = mandateId, createdBy = User("credId", "agentName", Some("agentCode")), None, None, agentParty = Party("JARN123457", "agency name", PartyType.Organisation, ContactDetails("agent@agent.com", None)), clientParty = Some(clientParty3), currentStatus = MandateStatus(Status.Approved, time1, "credId"), statusHistory = Seq(MandateStatus(Status.New, time1, "credId")), Subscription(None, Service("ated", "ATED")))

  val mandatePendingCancellation: Mandate = Mandate(id = mandateId, createdBy = User("credId", "agentName", Some("agentCode")), None, None, agentParty = Party("JARN123458", "agency name", PartyType.Organisation, ContactDetails("agent@agent.com", None)), clientParty = Some(clientParty4), currentStatus = MandateStatus(Status.PendingCancellation, time1, "credId"), statusHistory = Seq(MandateStatus(Status.New, time1, "credId")), Subscription(None, Service("ated", "ATED")))

  val mandatePendingActivation: Mandate = Mandate(id = mandateId, createdBy = User("credId", "agentName", Some("agentCode")), None, None, agentParty = Party("JARN123451", "agency name", PartyType.Organisation, ContactDetails("agent@agent.com", None)), clientParty = Some(clientParty2), currentStatus = MandateStatus(Status.PendingActivation, time1, "credId"), statusHistory = Seq(MandateStatus(Status.New, time1, "credId")), Subscription(None, Service("ated", "ATED")))

  def viewAuthorisedAgent(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val hc: HeaderCarrier = HeaderCarrier()
    implicit val user = AuthBuilder.createRegisteredAgentAuthContext(userId, "name")
    AuthBuilder.mockAuthorisedAgent(userId, mockAuthConnector)

    when(mockAgentClientMandateService.fetchAllClientMandates(Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())) thenReturn {
      Future.successful(Some(Mandates(activeMandates = Seq(mandateActive), pendingMandates = Seq(mandateNew, mandatePendingActivation, mandateApproved, mandatePendingCancellation))))
    }
    when(mockAgentClientMandateService.fetchAgentDetails()(Matchers.any(), Matchers.any())) thenReturn Future.successful(agentDetails)

    val result = TestAgentSummaryController.view(service).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

}
