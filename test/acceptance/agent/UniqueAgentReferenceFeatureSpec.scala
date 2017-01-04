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

package acceptance.agent

import org.jsoup.Jsoup
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, FeatureSpec, GivenWhenThen}
import org.scalatestplus.play.OneServerPerSuite
import play.api.test.FakeRequest
import uk.gov.hmrc.agentclientmandate.viewModelsAndForms.ClientDisplayDetails
import uk.gov.hmrc.agentclientmandate.views


class UniqueAgentReferenceFeatureSpec extends FeatureSpec with OneServerPerSuite with MockitoSugar with BeforeAndAfterEach with GivenWhenThen{

  implicit val request = FakeRequest()
  implicit val messages : play.api.i18n.Messages = play.api.i18n.Messages.Implicits.applicationMessages

  feature("The user can view the reject client page") {

    info("as a user I want to view the correct page content")

    scenario("user has visited the page from ated") {

      Given("A user visits the page from ated")
      When("The user views the page from ated")

      val mandateId = "ABC123"
      val clientDisplayDetails = ClientDisplayDetails("test name", mandateId)
      val html = views.html.agent.uniqueAgentReference(clientDisplayDetails,  "ated")

      val document = Jsoup.parse(html.toString())
      Then("The title should match - Your unique agent reference for test name is ABC123")
      assert(document.title() === "Your agent reference number for test name is ABC123")
      And("The banner text is - Your unique agent reference for test name is ABC123")
      assert(document.getElementById("banner-text").text() === "Your agent reference number for test name is ABC123")
      And("The screen text is - What happens next?")
      assert(document.getElementById("what-happens-next").text() === "What happens next")

      And("The authorise-instruction - What happens next?")
      assert(document.getElementById("authorise-instruction").text() === "You need to give this agent reference number to your client so they can authorise you.")

      And("The client-instruction - should be correct for the relevant service")
      assert(document.getElementById("client-instruction").text() === "Your client will then need to:")
      assert(document.getElementById("client-instruction-1").text() === "register their company to use the new ATED service, they may need to create a new organisational Government Gateway account")
      assert(document.getElementById("client-instruction-2").text() === "enter the agent reference number you gave them")

      And("The email-instruction : Once they have done this you will receive an email notification.")
      assert(document.getElementById("email-instruction").text() === "Once they have done this you will receive an email notification. You have 28 days to sign in and accept their request.")

      And("The submit : View all my clients has the correct link")
      assert(document.getElementById("view-clients-form").attr("action") === "/mandate/agent/summary/ated")
      assert(document.getElementById("submit").text() === "View all my clients")

      And("Return to service : Has the correct link")
      assert(document.getElementById("calling-service").text() === "ATED service")
      assert(document.getElementById("calling-service").attr("href").contains("/ated/welcome") === true)

    }
  }
}
