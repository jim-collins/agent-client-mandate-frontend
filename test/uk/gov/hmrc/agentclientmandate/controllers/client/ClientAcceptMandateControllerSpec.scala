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

package uk.gov.hmrc.agentclientmandate.controllers.client

import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.test.FakeRequest
import play.api.test.Helpers._

class ClientAcceptMandateControllerSpec extends PlaySpec with OneServerPerSuite {

  "ClientAcceptMandateController" must {

    "not return NOT_FOUND at route " when {

      "GET /agent-client-mandate/client-search-mandate" in {
        val result = route(FakeRequest(GET, "/agent-client-mandate/client-approve-mandate")).get
        status(result) mustNot be(NOT_FOUND)
      }

//      "POST /agent-client-mandate/client-search-mandate" in {
//        val result = route(FakeRequest(POST, "/agent-client-mandate/client-approve-mandate")).get
//        status(result) mustNot be(NOT_FOUND)
//      }

    }

  }

}
