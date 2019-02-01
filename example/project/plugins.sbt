scalacOptions in Compile ++= Seq("-deprecation", "-feature")

addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.3.14")

resolvers += "JAnalyse Repository" at "http://www.janalyse.fr/repository/"
addSbtPlugin("com.github.shmishleniy" % "sbt-deploy-ssh" % "0.1.4")
