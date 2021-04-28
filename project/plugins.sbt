libraryDependencies += "org.slf4j" % "slf4j-nop" % "1.7.25"
libraryDependencies += "org.eclipse.jgit" % "org.eclipse.jgit" % "5.2.1.201812262042-r"

scalacOptions in Compile ++= Seq("-deprecation", "-feature")

addSbtPlugin("com.github.sbt" % "sbt-pgp" % "2.1.2")
addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "3.9.6")