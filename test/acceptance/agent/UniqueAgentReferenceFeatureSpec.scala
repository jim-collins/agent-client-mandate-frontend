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

import org.jsoup.Jsoup
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, FeatureSpec, GivenWhenThen}
import org.scalatestplus.play.OneServerPerSuite
import play.api.test.FakeRequest
import uk.gov.hmrc.agentclientmandate.viewModelsAndForms.ClientDisplayDetails
import uk.gov.hmrc.agentclientmandate.views

class UniqueAgentReferenceFeatureSpec extends FeatureSpec with OneServerPerSuite with MockitoSugar with BeforeAndAfterEach with GivenWhenThen{

  feature("The user can view the reject client page") {

    info("as a user I want to view the correct page content")

    scenario("user has visited the page") {

      Given("A user visits the page")
      When("The user views the page")
      implicit val request = FakeRequest()

      val mandateId = "ABC123"
      val clientDisplayDetails = ClientDisplayDetails("test name", mandateId)
      val html = views.html.agent.uniqueAgentReference(clientDisplayDetails,  "ATED")

      val document = Jsoup.parse(html.toString())
      Then("The title should match - Your unique agent reference for {0} is {1}")
      assert(document.title() === "Your unique agent reference for {0} is {1}")
      And("The banner text is - Your unique agent reference for test name is ABC123")
      assert(document.getElementById("banner-text").text() === "Your unique agent reference for test name is ABC123")
      And("The screen text is - What happens next?")
      assert(document.getElementById("what-happens-next").text() === "What happens next?")

      And("The authorise-instruction - What happens next?")
      assert(document.getElementById("authorise-instruction").text() === "You need to give this agent reference to your client so they can authorise you.")

      And("The client-instruction")
      assert(document.getElementById("client-instruction").text() === "Your client will then need to:")
      assert(document.getElementById("client-instruction-1").text() === "Register their company to use the new ATED service, they may need to create a new organisational Government Gateway account.")
      assert(document.getElementById("client-instruction-2").text() === "Enter the agent reference you gave them.")

      And("The email-instruction : Once they have done this you will receive an email notification.")
      assert(document.getElementById("email-instruction").text() === "Once they have done this you will receive an email notification.")

      And("The request-expire : You have 28 days to sign in and accept their request.")
      assert(document.getElementById("request-expire").text() === "You have 28 days to sign in and accept their request.")

      And("The admin-instruction : If you have a number of agents working on ATED ..")
      assert(document.getElementById("admin-instruction").text() === "If you have a number of agents working on ATED within your organisation you may want to filter your clients. To do this you need to add administrators to your account in Government Gateway.")

      And("The submit : View all my clients")
      assert(document.getElementById("submit").text() === "View all my clients")

      assert(document.getElementById("ated-service").attr("href") === "#")

      assert(document.getElementById("admin").attr("href") === "#")



    }
  }
}
