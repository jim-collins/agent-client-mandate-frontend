resolvers += Resolver.url("hmrc-sbt-plugin-releases", url("https://dl.bintray.com/hmrc/sbt-plugin-releases"))(Resolver.ivyStylePatterns)

resolvers += "Typesafe Releases" at "http://repo.typesafe.com/typesafe/releases/"

resolvers += Resolver.url("scoverage-bintray", url("https://dl.bintray.com/sksamuel/sbt-plugins/"))(Resolver.ivyStylePatterns)

addSbtPlugin("uk.gov.hmrc" % "sbt-auto-build" % "1.4.0")

addSbtPlugin("uk.gov.hmrc" % "sbt-git-versioning" % "0.9.0")

addSbtPlugin("uk.gov.hmrc" % "sbt-distributables" % "1.0.0")

addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.5.8")

addSbtPlugin("org.scoverage" %% "sbt-scoverage" % "1.3.3")

addSbtPlugin("org.scalastyle" %% "scalastyle-sbt-plugin" % "0.8.0")
