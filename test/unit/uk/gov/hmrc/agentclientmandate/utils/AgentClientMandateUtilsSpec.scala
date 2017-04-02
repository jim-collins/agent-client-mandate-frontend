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

package unit.uk.gov.hmrc.agentclientmandate.utils

import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import uk.gov.hmrc.agentclientmandate.utils.AgentClientMandateUtils
import uk.gov.hmrc.agentclientmandate.models.Status
import uk.gov.hmrc.agentclientmandate.models.Status.Status

class AgentClientMandateUtilsSpec extends PlaySpec with OneServerPerSuite {

  "AgentClientMandateUtils" must {
    "validateUTR" must {
      "given valid UTR return true" in {
        AgentClientMandateUtils.validateUTR(Some("1111111111")) must be(true)
        AgentClientMandateUtils.validateUTR(Some("1111111112")) must be(true)
        AgentClientMandateUtils.validateUTR(Some("8111111113")) must be(true)
        AgentClientMandateUtils.validateUTR(Some("6111111114")) must be(true)
        AgentClientMandateUtils.validateUTR(Some("4111111115")) must be(true)
        AgentClientMandateUtils.validateUTR(Some("2111111116")) must be(true)
        AgentClientMandateUtils.validateUTR(Some("2111111117")) must be(true)
        AgentClientMandateUtils.validateUTR(Some("9111111118")) must be(true)
        AgentClientMandateUtils.validateUTR(Some("7111111119")) must be(true)
        AgentClientMandateUtils.validateUTR(Some("5111111123")) must be(true)
        AgentClientMandateUtils.validateUTR(Some("3111111124")) must be(true)
      }
      "given invalid UTR return false" in {
        AgentClientMandateUtils.validateUTR(Some("2111111111")) must be(false)
        AgentClientMandateUtils.validateUTR(Some("211111111")) must be(false)
        AgentClientMandateUtils.validateUTR(Some("211111 111 ")) must be(false)
        AgentClientMandateUtils.validateUTR(Some("211111ab111 ")) must be(false)
      }
      "None as UTR return false" in {
        AgentClientMandateUtils.validateUTR(None) must be(false)
      }
    }

    "checkStatus" when {
      "a valid status is passed" in {
        AgentClientMandateUtils.checkStatus(Status.PendingActivation) must be ("Pending")
      }
    }
  }


    "getIsoCodeTupleList" must {
      "bring the correct country from the file" in {
        AgentClientMandateUtils.getIsoCodeTupleList must contain(("US", "USA :United States of America"))
        AgentClientMandateUtils.getIsoCodeTupleList must contain(("GB", "United Kingdom :UK, GB, Great Britain"))
        AgentClientMandateUtils.getIsoCodeTupleList must contain(("GB", "United Kingdom :UK, GB, Great Britain"))
      }
    }


}
