scalacOptions in Compile ++= Seq("-deprecation", "-feature")

addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.3.14")

addSbtPlugin("io.github.shmishleniy" % "sbt-deploy-ssh" % "0.1.5")
