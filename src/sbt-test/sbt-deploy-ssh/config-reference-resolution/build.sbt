import deployssh.DeploySSH._

enablePlugins(DeploySSH)

lazy val root = (project in file("."))
  .settings(
    version := "0.1",
    scalaVersion := "2.10.6",
    deployResourceConfigFiles ++= Seq("ssh-deploy.conf")
  )