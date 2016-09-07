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

package uk.gov.hmrc.agentclientmandate.utils

import play.api.{Logger, Play}
import play.twirl.api.Html

object AgentDynamicTemplateLoader extends DynamicTemplateLoader {
  val packageName = "views.html.agents."
  def templateName(service: String): String = s"${packageName}_${service}_agent"
}

object ClientDynamicTemplateLoader extends DynamicTemplateLoader {
  val packageName = "views.html.clients."
  def templateName(service: String): String = s"${packageName}_${service}_client"
}

trait DynamicTemplateLoader {

  import reflect.runtime.universe._
  def packageName: String
  val currentMirror = runtimeMirror(Play.current.classloader)

  def templateName(service: String): String

  def moduleMirror(service: String) = currentMirror.reflectModule(currentMirror.staticModule(templateName(service)))
  def methodSymbol(service: String) = moduleMirror(service).symbol.typeSignature.decl(TermName("apply")).asMethod

  def instanceMirror(service: String) = currentMirror.reflect(moduleMirror(service).instance)
  def methodMirror(service: String) = instanceMirror(service).reflectMethod(methodSymbol(service))

  def returnOptTemplate(service: String): Option[Html] = try {
    Some(methodMirror(service).apply().asInstanceOf[Html])
  } catch {
    case ex: ScalaReflectionException =>
      Logger.info(s"$service template ${templateName(service)} not found.")
      None
  }

}
