sbtPlugin := true

name := "sbt-deploy-ssh"
organization := "com.github.shmishleniy"
version := "0.1.4"

publishMavenStyle := false
bintrayRepository in bintray := "sbt-plugins"
licenses += ("MIT", url("https://opensource.org/licenses/MIT"))
bintrayOrganization in bintray := None

resolvers += "JAnalyse Repository" at "http://www.janalyse.fr/repository/"
libraryDependencies ++= Seq(
  "com.typesafe" % "config" % "1.2.1",
  "fr.janalyse" %% "janalyse-ssh" % "0.10.1",
  "org.scalaz" %% "scalaz-core" % "7.2.8"
)

crossSbtVersions := Vector("0.13.15", "1.0.2")

scalacOptions in Compile ++= Seq("-encoding","UTF-8","-deprecation","-feature")
javacOptions in Compile ++= Seq("-encoding","UTF-8")
