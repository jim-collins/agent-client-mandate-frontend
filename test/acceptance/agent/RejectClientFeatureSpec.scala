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
import uk.gov.hmrc.agentclientmandate.viewModelsAndForms.YesNoQuestionForm._
import uk.gov.hmrc.agentclientmandate.views

class RejectClientFeatureSpec extends FeatureSpec with OneServerPerSuite with MockitoSugar with BeforeAndAfterEach with GivenWhenThen{

  feature("The user can view the reject client page") {

    info("as a user I want to view the correct page content")

    scenario("user has visited the page") {

      Given("A user visits the page")
      When("The user views the page")
      implicit val request = FakeRequest()
      val html = views.html.agent.rejectClient("ATED", yesNoQuestionForm, "ACME Limited", "")
      val document = Jsoup.parse(html.toString())
      Then("The title should match - Are you sure you want to reject the request from ACME Limited?")
      assert(document.title() === "Are you sure you want to reject the request from ACME Limited?")
      And("The pre-header text is - Manage your ATED service")
      assert(document.getElementById("pre-heading").text() === "Manage your ATED service")
      And("The header text is - Are you sure you want to reject the request from ACME Limited?")
      assert(document.getElementById("heading").text() === "Are you sure you want to reject the request from ACME Limited?")

    }
  }
}
