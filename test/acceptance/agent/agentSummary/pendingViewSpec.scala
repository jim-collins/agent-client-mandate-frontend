/*
 * Copyright 2017 HM Revenue & Customs
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

package acceptance.agent.agentSummary

import org.joda.time.DateTime
import org.jsoup.Jsoup
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, FeatureSpec, GivenWhenThen}
import org.scalatestplus.play.OneServerPerSuite
import play.api.test.FakeRequest
import uk.gov.hmrc.agentclientmandate.models._
import uk.gov.hmrc.agentclientmandate.service.Mandates
import uk.gov.hmrc.agentclientmandate.views
import uk.gov.hmrc.domain.Generator

class pendingViewSpec extends FeatureSpec with OneServerPerSuite with MockitoSugar with BeforeAndAfterEach with GivenWhenThen{

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

  val mandateNew: Mandate = Mandate(id = mandateId, createdBy = User("credId", "agentName", Some("agentCode")), None, None, agentParty = Party("JARN123456", "agency name", PartyType.Organisation, ContactDetails("agent@agent.com", None)), clientParty = None, currentStatus = MandateStatus(Status.New, time1, "credId"), statusHistory = Nil, Subscription(None, Service("ated", "ATED")), clientDisplayName = "client display name 1")
  val mandateActive: Mandate = Mandate(id = mandateId, createdBy = User("credId", "agentName", Some("agentCode")), None, None, agentParty = Party("JARN123457", "agency name", PartyType.Organisation, ContactDetails("agent@agent.com", None)), clientParty = Some(clientParty), currentStatus = MandateStatus(Status.Active, time1, "credId"), statusHistory = Seq(MandateStatus(Status.New, time1, "credId")), Subscription(None, Service("ated", "ATED")), clientDisplayName = "client display name 2")
  val mandateApproved: Mandate = Mandate(id = mandateId, createdBy = User("credId", "agentName", Some("agentCode")), None, None, agentParty = Party("JARN123457", "agency name", PartyType.Organisation, ContactDetails("agent@agent.com", None)), clientParty = Some(clientParty3), currentStatus = MandateStatus(Status.Approved, time1, "credId"), statusHistory = Seq(MandateStatus(Status.New, time1, "credId")), Subscription(None, Service("ated", "ATED")), clientDisplayName = "client display name 3")
  val mandatePendingCancellation: Mandate = Mandate(id = mandateId, createdBy = User("credId", "agentName", Some("agentCode")), None, None, agentParty = Party("JARN123458", "agency name", PartyType.Organisation, ContactDetails("agent@agent.com", None)), clientParty = Some(clientParty4), currentStatus = MandateStatus(Status.PendingCancellation, time1, "credId"), statusHistory = Seq(MandateStatus(Status.New, time1, "credId")), Subscription(None, Service("ated", "ATED")), clientDisplayName = "client display name 4")
  val mandatePendingActivation: Mandate = Mandate(id = mandateId, createdBy = User("credId", "agentName", Some("agentCode")), None, None, agentParty = Party("JARN123451", "agency name", PartyType.Organisation, ContactDetails("agent@agent.com", None)), clientParty = Some(clientParty2), currentStatus = MandateStatus(Status.PendingActivation, time1, "credId"), statusHistory = Seq(MandateStatus(Status.New, time1, "credId")), Subscription(None, Service("ated", "ATED")), clientDisplayName = "client display name 5")

  implicit val request = FakeRequest()
  implicit val messages : play.api.i18n.Messages = play.api.i18n.Messages.Implicits.applicationMessages

  feature("The agent can view the agent summary page when they only have pending clients") {

    info("as an agent I want to view the correct page content")

    scenario("agent has visited the page and has no clients but has pending clients") {

      Given("An agent visits the page and has no clients but has pending clients")
      When("The agent views the mandates")
      implicit val request = FakeRequest()

      val pendingMandates = Seq(mandateNew, mandatePendingActivation, mandateApproved, mandatePendingCancellation)

      val html = views.html.agent.agentSummary.pending("ATED", Mandates(Nil, pendingMandates), agentDetails, "")

      val document = Jsoup.parse(html.toString())
      Then("The title should match - Your ATED clients")
      assert(document.title() === "Your ATED clients")
      
      And("The Pending Clients tab - should not exist")
      assert(document.getElementById("pending-clients").text === "Requests (4)")

      And("The Pending Clients table - should have a name and action")
      assert(document.getElementById("client-name").text === "Name")
      assert(document.getElementById("client-action").text === "Action")

      And("The Pending Clients table - has the correct data and Accept link")

      assert(document.getElementById("pending-client-data-0").child(0).text() === "client display name 1")
      assert(document.getElementById("pending-client-data-0").child(2).text() === "Awaiting client")
      assert(document.getElementById("reject-client-link-0") === null)
      assert(document.getElementById("pending-client-data-1").child(0).text() === "client display name 5")
      assert(document.getElementById("pending-client-data-1").child(2).text() === "Pending")
      assert(document.getElementById("reject-client-link-1") === null)
      assert(document.getElementById("pending-client-data-2").child(0).text() === "client display name 3")
      assert(document.getElementById("pending-client-data-2").child(2).text() === "Accept client display name 3")
      assert(document.getElementById("reject-client-link-2").text() === "Reject client display name 3")
      assert(document.getElementById("pending-client-data-3").child(0).text() === "client display name 4")
      assert(document.getElementById("pending-client-data-3").child(2).text() === "Pending")
      assert(document.getElementById("reject-client-link-3") === null)


      And("The Add Client Button - should not exist")
      assert(document.getElementById("add-client-btn") === null)
      And("The Add Client Link - should exist")
      assert(document.getElementById("add-client-link").text() === "Add a new client")
    }
  }
}
