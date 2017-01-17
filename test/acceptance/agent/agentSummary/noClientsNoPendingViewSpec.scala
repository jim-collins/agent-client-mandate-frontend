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

class noClientsNoPendingViewSpec extends FeatureSpec with OneServerPerSuite with MockitoSugar with BeforeAndAfterEach with GivenWhenThen{

  val registeredAddressDetails = RegisteredAddressDetails("123 Fake Street", "Somewhere", None, None, None, "GB")
  val agentDetails = AgentDetails("ABC Ltd.", registeredAddressDetails)

  val mandateId = "12345678"
  val time1 = DateTime.now()
  val service = "ATED"
  val atedUtr = new Generator().nextAtedUtr

  implicit val request = FakeRequest()
  implicit val messages : play.api.i18n.Messages = play.api.i18n.Messages.Implicits.applicationMessages

  feature("The agent can view the agent summary page but they have no clients and no pending clients") {

    info("as an agent I want to view the correct page content")

    scenario("agent has visited the page but has no clients or pending clients") {

      Given("An agent visits the page and has no mandates")
      When("The agent views the empty page")

      val html = views.html.agent.agentSummary.noClientsNoPending("ATED", agentDetails)

      val document = Jsoup.parse(html.toString())
      Then("The title should match - Your ATED clients")
      assert(document.title() === "Your ATED clients")

      And("The Pre Header should be the agents name - ABC Ltd.")
      assert(document.getElementById("pre-header").text() === "ABC Ltd.")

      And("The Add Client Button - should exist")
      assert(document.getElementById("add-client-btn").text() === "Add a new client")

      And("The Add Client Link - should not exist")
      assert(document.getElementById("add-client-link") === null)

      And("The sign out link should return to ATED")
      assert(document.getElementById("logOutNavHref").attr("href") === ("http://localhost:9916/ated/logout"))
    }
  }
}