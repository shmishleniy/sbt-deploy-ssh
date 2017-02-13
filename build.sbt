import bintray.Keys._

sbtPlugin := true

name := "sbt-deploy-ssh"
organization := "com.github.shmishleniy"
version := "0.1.3"

publishMavenStyle := false
bintrayPublishSettings
repository in bintray := "sbt-plugins"
licenses += ("MIT", url("https://opensource.org/licenses/MIT"))
bintrayOrganization in bintray := None

resolvers += "JAnalyse Repository" at "http://www.janalyse.fr/repository/"
libraryDependencies ++= Seq(
  "com.typesafe" % "config" % "1.2.1",
  "fr.janalyse" %% "janalyse-ssh" % "0.9.19",
  "org.scalaz" %% "scalaz-core" % "7.2.8"
)

scalacOptions in Compile ++= Seq("-encoding","UTF-8","-target:jvm-1.7","-deprecation","-feature")
javacOptions in Compile ++= Seq("-encoding","UTF-8","-source","1.7","-target","1.7")
