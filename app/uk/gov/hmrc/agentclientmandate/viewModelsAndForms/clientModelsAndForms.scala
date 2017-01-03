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

package uk.gov.hmrc.agentclientmandate.viewModelsAndForms

import play.api.data.{Form, FormError}
import play.api.data.Forms._
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._
import play.api.Play.current
import play.api.libs.json.Json
import uk.gov.hmrc.agentclientmandate.models.Mandate

import scala.annotation.tailrec

case class ClientEmail(email: String, confirmEmail: String)

object ClientEmail {
  implicit val formats = Json.format[ClientEmail]
}

object ClientEmailForm {

  val emailLength = 241

  val clientEmailForm =
    Form(
      mapping(
        "email" -> text
          .verifying(Messages("client.collect-email.error.email"), email => email.nonEmpty)
          .verifying(Messages("client.collect-email.error.email.length"), x => x.isEmpty || (x.nonEmpty && x.length <= emailLength)),
        "confirmEmail" -> text
          .verifying(Messages("client.collect-email.error.confirmEmail"), email => email.nonEmpty)
          .verifying(Messages("client.collect-email.error.confirmEmail.length"), x => x.isEmpty || (x.nonEmpty && x.length <= emailLength))
      )
      (ClientEmail.apply)(ClientEmail.unapply)
    )

  def validateConfirmEmail(emailForm: Form[ClientEmail]): Form[ClientEmail] = {
    def validate = {
      val email = emailForm.data.get("email").map(_.trim)
      val confirmEmail = emailForm.data.get("confirmEmail").map(_.trim)
      (email, confirmEmail) match {
        case (Some(e1), Some(e2)) if e1 == e2 => Seq()
        case (Some(e1), Some(e2)) => Seq(Some(FormError("confirmEmail", Messages("client.collect-email.error.confirm-email.not-equal"))))
        case _ => Seq()
      }
    }
    addErrorsToForm(emailForm, validate.flatten)
  }

  private def addErrorsToForm[A](form: Form[A], formErrors: Seq[FormError]): Form[A] = {
    @tailrec
    def y(f: Form[A], fe: Seq[FormError]): Form[A] = {
      if (fe.isEmpty) f
      else y(f.withError(fe.head), fe.tail)
    }
    y(form, formErrors)
  }

}

case class MandateReference(mandateRef: String)

object MandateReference {
  implicit val formats = Json.format[MandateReference]
}

object MandateReferenceForm {

  val mandateRefLength = 8

  //private val validFormat = "^[\\d|A-Z]{8}$"

  val mandateRefForm =
    Form(
      mapping(
        "mandateRef" -> text.transform[String](a => a.trim.replaceAll("\\s+", ""), a => a.trim.replaceAll("\\s+", "").toUpperCase)
          .verifying(Messages("client.search-mandate.error.mandateRef"), x => x.nonEmpty)
          .verifying(Messages("client.search-mandate.error.mandateRef.length"), x => x.isEmpty || (x.nonEmpty && x.length <= mandateRefLength))
      )
      (MandateReference.apply)(MandateReference.unapply)
    )
}

case class ClientCache(
                        email: Option[ClientEmail] = None,
                        mandate: Option[Mandate] = None
                      )

object ClientCache {
  implicit val formats = Json.format[ClientCache]
}

case class Declaration(agree: Option[Boolean])

object Declaration {
  implicit val formats = Json.format[Declaration]
}

object DeclarationForm {

  val declarationForm = Form(mapping(
    "agree" -> optional(boolean).verifying(Messages("mandate.checkbox-i-agree.confirm.error"), confirm => confirm.isDefined)
  )(Declaration.apply)(Declaration.unapply))

}
