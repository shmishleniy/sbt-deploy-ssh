ThisBuild / scalaVersion := "2.12.8"
ThisBuild / fork := true
ThisBuild / cancelable in Global := true
ThisBuild / scalacOptions in Compile ++= Vector(
  "-target:jvm-1.8",
  "-feature",
  "-unchecked",
  "-deprecation",
  "-language:_",
  "-encoding", "UTF-8",
  "-Xfatal-warnings",
  "-Ywarn-unused-import",
)

import deployssh.DeploySSH.{ServerConfig, ArtifactSSH}
import fr.janalyse.ssh.SSH

lazy val example = project.in(file(".")).settings(
  libraryDependencies += "com.typesafe" % "config" % "1.3.3",
  mainClass in (Compile, run) := Some("example.App"),
    deployConfigs ++= Seq(
    ServerConfig(name="server1", host="localhost", user=None),
  ),
  deployArtifacts ++= Seq(
    ArtifactSSH((packageBin in Universal).value, "/tmp/example")
  ),
  deploySshExecBefore ++= Seq(
    (ssh: SSH) => ssh.shell{ shell =>
      shell.execute("cd /tmp/example")
      shell.execute("touch pid")
      val pid = shell.execute("cat pid")
      if (pid != "") {
        shell.execute(s"kill ${pid}; sleep 5; kill -9 ${pid}")
      } else ()
      shell.execute("rm pid")
    }
  ),
  deploySshExecAfter ++= Seq(
    (ssh: SSH) => {
      ssh.scp { scp =>
        scp.send(file(s"./deploy/${ssh.options.name.get}.conf"), "/tmp/example/app.conf")
      }
      ssh.shell{ shell =>
        val name = (packageName in Universal).value
        val script = (executableScriptName in Universal).value
        shell.execute("cd /tmp/example")
        shell.execute(s"unzip -q -o ${name}.zip")
        shell.execute(s"rm ${name}.zip")
        shell.execute(s"nohup ./${name}/bin/${script} -Dconfig.file=/tmp/example/app.conf &")
        shell.execute("echo $! > pid")
        shell.execute("touch pid")
        val pid = shell.execute("cat pid")
        val (_, status) = shell.executeWithStatus("echo $?")
        if (status != 0 || pid == "") {
         throw new RuntimeException(s"status=${status}, pid=${pid}. please check package")
        }
      }
    }
  ),
).enablePlugins(JavaAppPackaging, DeploySSH)
