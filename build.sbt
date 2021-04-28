sbtPlugin := true

name := "sbt-deploy-ssh"
organization := "io.github.shmishleniy"
organizationHomepage := Some(url("https://github.com/shmishleniy"))
version := "0.1.5"

licenses += ("MIT", url("https://opensource.org/licenses/MIT"))

import xerial.sbt.Sonatype._
sonatypeProjectHosting := Some(GitHubHosting("shmishleniy", "sbt-deploy-ssh", "shmishleniy@gmail.com"))

publishMavenStyle := true
credentials += Credentials(Path.userHome / ".sbt" / "sonatype_credentials")
pomIncludeRepository := { _ => false }
publishTo := sonatypePublishToBundle.value
sonatypeCredentialHost := "s01.oss.sonatype.org"
sonatypeRepository := "https://s01.oss.sonatype.org/service/local"

libraryDependencies ++= Seq(
  "com.typesafe" % "config" % "1.3.3",
  "fr.janalyse" %% "janalyse-ssh" % "0.10.4",
  "org.scalaz" %% "scalaz-core" % "7.2.27"
)

scalacOptions in Compile ++= Seq("-encoding", "UTF-8", "-deprecation", "-feature")
javacOptions in Compile ++= Seq("-encoding", "UTF-8")
