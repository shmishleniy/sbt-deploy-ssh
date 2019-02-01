package example

import com.typesafe.config.ConfigFactory

object Example extends App {
  val config = ConfigFactory.load()
  val name = config.getString("name")
  val t = System.currentTimeMillis
  println(s"name=${name}, t=${t}")
}
