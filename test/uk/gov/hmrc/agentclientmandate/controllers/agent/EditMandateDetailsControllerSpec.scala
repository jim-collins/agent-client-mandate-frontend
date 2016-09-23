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
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.agentclientmandate.builders.{SessionBuilder, AuthBuilder}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future

class EditMandateDetailsControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with BeforeAndAfterEach {

  "EditMandateControllerSpec" must {

    "not return NOT_FOUND at route " when {
      "GET /mandate/agent/agent-edit-client" in {
        val result = route(FakeRequest(GET, "/mandate/agent/agent-edit-client")).get
        status(result) mustNot be(NOT_FOUND)
      }
    }

    "return 'edit mandate' view for AUTHORISED agent" when {

      "client requests(GET) for edit mandate view" in {
        viewWithAuthorisedAgent { result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))
          document.title() must be("Edit ACME Limited")
          document.getElementById("header").text() must include("Manage your ATED service Edit ACME Limited")
          document.getElementById("sub-heading").text() must be("Unique agent reference GG123456")
          document.getElementById("displayName_field").text() must include("Display name")
          document.getElementById("displayName_hint").text() must include("This does not change the official company name.")
          document.getElementById("utr_field").text() must include("Unique tax reference")
          document.getElementById("utr_hint").text() must include("A UTR number is made up of 10 or 13 digits, If it is 13 digits only enter the last 10.")
          document.getElementById("utr-help-question").text() must be("Where to find their UTR")
          document.getElementById("submit").text() must be("Save changes")
        }
      }

    }

  }

  val mockAuthConnector = mock[AuthConnector]
  val service = "ATED"

  object TestEditMandateController extends EditMandateController {
    override val authConnector = mockAuthConnector
  }

  def viewWithAuthorisedAgent(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val hc: HeaderCarrier = HeaderCarrier()
    implicit val user = AuthBuilder.createOrgAuthContext(userId, "name")
    AuthBuilder.mockAuthorisedAgent(userId, mockAuthConnector)
    val result = TestEditMandateController.view.apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }



}
