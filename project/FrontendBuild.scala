import sbt._

object FrontendBuild extends Build with MicroService {

  val appName = "agent-client-mandate-frontend"

  override lazy val appDependencies: Seq[ModuleID] = AppDependencies()
}

private object AppDependencies {

  import play.sbt.PlayImport._
  import play.core.PlayVersion


  private val playHealthVersion = "2.1.0"

  private val frontendBootstrapVersion = "7.19.0"
  private val govukTemplateVersion = "5.1.0"
  private val playUiVersion = "7.0.0"
  private val httpCachingClientVersion = "6.2.0"
  private val playAuthorisedFrontendVersion = "6.3.0"
  private val playPartialsVersion = "5.3.0"
  private val logbackJsonLoggerVersion = "3.1.0"
  private val playConfigVersion = "4.3.0"
  private val domainVersion = "4.1.0"

  private val hmrcTestVersion = "2.3.0"
  private val scalaTestPlusVersion = "1.5.1"
  private val pegdownVersion = "1.6.0"

  val compile = Seq(
    ws,
    "uk.gov.hmrc" %% "frontend-bootstrap" % frontendBootstrapVersion,
    "uk.gov.hmrc" %% "play-partials" % playPartialsVersion,
    "uk.gov.hmrc" %% "play-authorised-frontend" % playAuthorisedFrontendVersion,
    "uk.gov.hmrc" %% "domain" % domainVersion,
    "uk.gov.hmrc" %% "play-config" % playConfigVersion,
    "uk.gov.hmrc" %% "logback-json-logger" % logbackJsonLoggerVersion,
    "uk.gov.hmrc" %% "govuk-template" % govukTemplateVersion,
    "uk.gov.hmrc" %% "play-health" % playHealthVersion,
    "uk.gov.hmrc" %% "play-ui" % playUiVersion,
    "uk.gov.hmrc" %% "http-caching-client" % httpCachingClientVersion
  )

  trait TestDependencies {
    lazy val scope: String = "test"
    lazy val test: Seq[ModuleID] = ???
  }

  object Test {
    def apply() = new TestDependencies {
      override lazy val test = Seq(
        "uk.gov.hmrc" %% "hmrctest" % hmrcTestVersion % scope,
        "org.scalatestplus.play" %% "scalatestplus-play" % scalaTestPlusVersion % scope,
        "org.pegdown" % "pegdown" % pegdownVersion % scope,
        "org.jsoup" % "jsoup" % "1.9.2" % scope,
        "uk.gov.hmrc" %% "logback-json-logger" % logbackJsonLoggerVersion,
        "org.mockito" % "mockito-all" % "1.10.19" % scope,
        "com.typesafe.play" %% "play-test" % PlayVersion.current % scope
      )
    }.test
  }

  def apply() = compile ++ Test()
}


