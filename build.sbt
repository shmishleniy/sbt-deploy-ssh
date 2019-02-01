sbtPlugin := true

name := "sbt-deploy-ssh"
organization := "com.github.shmishleniy"
version := org.eclipse.jgit.api.Git.open(file(".")).describe().call()

publishMavenStyle := false
bintrayRepository in bintray := "sbt-plugins"
licenses += ("MIT", url("https://opensource.org/licenses/MIT"))
bintrayOrganization in bintray := None

resolvers += "JAnalyse Repository" at "http://www.janalyse.fr/repository/"
libraryDependencies ++= Seq(
  "com.typesafe" % "config" % "1.3.3",
  "fr.janalyse" %% "janalyse-ssh" % "0.10.3",
  "org.scalaz" %% "scalaz-core" % "7.2.27"
)

scalacOptions in Compile ++= Seq("-encoding", "UTF-8", "-deprecation", "-feature")
javacOptions in Compile ++= Seq("-encoding", "UTF-8")
