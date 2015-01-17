import sbt._

object Version {
  val typesafeConfig = "1.2.1"
  val janalyse = "0.9.14"
}

object Library {
  val typesafeConfig = "com.typesafe" % "config" % Version.typesafeConfig
  val janalyse = "fr.janalyse"   %% "janalyse-ssh" % Version.janalyse
}

object Dependencies {
  import Library._

  val tools = Seq(
    typesafeConfig,
    janalyse
  )
}