import bintray.Keys._

name := "sbt-deploy-ssh"

sbtPlugin := true

organization := "com.github.shmishleniy"

version := "0.1"

scalacOptions ++= Seq(
  "-feature",
  "-unchecked",
  "-deprecation",
  "-language:_",
  "-encoding", "UTF-8"
)

publishMavenStyle := false

bintrayPublishSettings

bintrayOrganization in bintray := None

resolvers += "JAnalyse Repository" at "http://www.janalyse.fr/repository/"

libraryDependencies ++= Dependencies.tools