package deployssh

import com.typesafe.config.{ConfigFactory, Config, ConfigParseOptions}
import fr.janalyse.ssh._
import sbt._

import scala.util.{Failure, Success, Properties, Try}

object DeploySSH extends AutoPlugin {

  object Keys {
    val deployExternalConfigFiles = SettingKey[Seq[String]]("deploy-external-config-files", "Deploy config file name located somewhere")
    val deployResourceConfigFiles = SettingKey[Seq[String]]("deploy-resource-config-files", "Deploy config file name located in project root directory")
    val deployHomeConfigFiles = SettingKey[Seq[String]]("deploy-home-config-files", "Deploy config file name located in `user home` directory")
    val deployConfigsLoaded = TaskKey[Seq[Config]]("deploy-configs-loaded", "Deploy config loaded from file")
    val deployConfigs = TaskKey[Seq[ServerConfig]]("deploy-configs", "Additional config")
    val deployServers = TaskKey[Map[String, ServerConfig]]("deploy-servers", "Servers configs")

    val deployArtifacts = TaskKey[Seq[ArtifactSSH]]("deploy-artifacts", "Artifacts that will be sent to the server")
    val deploySsh = InputKey[Unit]("deploy-ssh", "Deploy to the specified server. Usage: `deploy-ssh serverName1 serverName2`")

    val deploySshServersNames = TaskKey[Seq[String]]("deploy-ssh-servers-names", "Deploy to the specified servers when call `deploy-ssh-task`")
    val deploySshTask = TaskKey[Unit]("deploy-ssh-task", "Wrap `deploySsh`. Will be executed with `deploy-ssh-servers-names` args")

    val deploySshExecBefore = TaskKey[Seq[(SSH) => Any]]("deploy-ssh-exec-before", "Execute before deploy")
    val deploySshExecAfter = TaskKey[Seq[(SSH) => Any]]("deploy-ssh-exec-after", "Execute after deploy")
  }

  val autoImport = Keys

  case class ServerConfig(name: String,
                          host: String,
                          user: Option[String] = None,
                          password: Option[String] = None,
                          passphrase: Option[String] = None,
                          port: Option[Int] = None,
                          sshDir: Option[String] = None,
                          sshKeyFile: Option[String] = None)

  case class ArtifactSSH(path: File, remoteDir: String)

  class SkipDeployException(e: Exception) extends Exception(e)

  import autoImport._
  import scala.collection.JavaConversions._
  override lazy val projectSettings = defaultSetting ++ Seq(
    deployConfigsLoaded := {
      val log = sbt.Keys.streams.value.log
      def loadFromFile(path: String): Config = {
        Try {
          ConfigFactory.parseFile(file(path),
            ConfigParseOptions.defaults.setAllowMissing(false))
        } match {
          case Success(config) => config
          case Failure(e) =>
            log.error(s"Failed to load config=$path, error=$e")
            ConfigFactory.empty()
        }
      }

      val external = deployExternalConfigFiles.value.map(loadFromFile)
      val projectDir = (sbt.Keys.baseDirectory in ThisBuild).value
      val resource = deployResourceConfigFiles.value.map {
        path => loadFromFile((projectDir / path).getPath)
      }
      val home = deployHomeConfigFiles.value.map {
        path => loadFromFile((file(Properties.userHome) / path).getPath)
      }
      (external ++ resource ++ home).toSeq
    },
    deployServers := {
      val serverConfigs: List[DeploySSH.ServerConfig] =
        deployConfigsLoaded.value.foldLeft(List[ServerConfig]()) {
          (lst, config) => {
            val serverConfigs = for {
              server <- Try(config.getObjectList("servers").toList) getOrElse List()
              serverConfig = server.toConfig
              name = serverConfig.getString("name")
              host = serverConfig.getString("host")
              user = Try(Some(serverConfig.getString("user"))) getOrElse None
              password = Try(Some(serverConfig.getString("password"))) getOrElse None
              passphrase = Try(Some(serverConfig.getString("passphrase"))) getOrElse None
              port = Try(Some(serverConfig.getInt("port"))) getOrElse None
              sshDir = Try(Some(serverConfig.getString("sshDir"))) getOrElse None
              sshKeyFile = Try(Some(serverConfig.getString("sshKeyFile"))) getOrElse None
            } yield ServerConfig(name, host, user, password, passphrase, port, sshDir, sshKeyFile)
            lst ::: serverConfigs
          }
        }
      (serverConfigs ++ deployConfigs.value).foldLeft(Map.empty[String, ServerConfig]) {
        (map, el) =>
          map + (el.name -> el)
      }
    },
    deploySsh := {
      val log = sbt.Keys.streams.value.log
      val servers = {
        val args = sbt.Def.spaceDelimited().parsed
        if (args.size > 0 && "!!".equals(args.head))
          deploySshServersNames.value
        else args
      }
      val configs = deployServers.value
      if (configs.size > 0) log.info(s"Loaded ${configs.size} configs.")
      else log.warn(s"Loaded 0 configs. Please verify configuration.")
      log.debug(configs.mkString(", \r\n"))
      log.info(s"Deploy started. Servers=${servers.mkString(", ")}.")
      servers foreach { serverName =>
        log.info(s"Trying to deploy to server with name=$serverName.")
        configs get serverName match {
          case Some(serverConfig) =>
            log.info(s"Found config=${serverConfig.host}. Start deployment process.")
            try {
              deployToTheServer(serverConfig,
                deployArtifacts.value,
                deploySshExecBefore.value,
                deploySshExecAfter.value, log)
              log.info(s"Deploy done for server=$serverName.")
            } catch {
              case error: SkipDeployException =>
                log.error(s"Failed to deploy to the server=$serverName, error=${error.getMessage}. Skip deployment.\r\nStack=${error.getStackTraceString}")
            }
          case None =>
            log.error(s"Failed to find config for server name=$serverName. Skip deployment process.")
        }
      }
      log.info(s"Deploy done.")
    },
    deploySshTask := deploySsh.toTask(" !!").value
  )

  val defaultSetting = Seq(
    deployExternalConfigFiles := Seq(),
    deployResourceConfigFiles := Seq(),
    deployHomeConfigFiles := Seq(),
    deploySshExecBefore := Seq(),
    deploySshExecAfter := Seq(),
    deployConfigs := Seq(),
    deployArtifacts := Seq(),
    deploySshServersNames := Seq()
  )

  private[this] def deployToTheServer(serverConfig: ServerConfig,
                                      artifacts: Seq[ArtifactSSH],
                                      execBefore: Seq[(SSH) => Any],
                                      execAfter: Seq[(SSH) => Any],
                                      log: Logger): Unit = {
    import java.io.File.separator
    import java.nio.file.Paths

    val sshDir = serverConfig.sshDir.map(Paths.get(_)).getOrElse(Paths.get(Properties.userHome + separator + ".ssh"))
    val keyFilenames = (serverConfig.sshKeyFile.toList ++ SSHOptions.defaultPrivKeyFilenames).distinct
    val identities = keyFilenames.map(f => sshDir.resolve(f)).map(p => SSHIdentity(p.toString))

    implicit val ssh = SSH(
      SSHOptions(
        serverConfig.host,
        serverConfig.user getOrElse Properties.userName,
        serverConfig.password,
        serverConfig.passphrase,
        Some(serverConfig.name),
        port = serverConfig.port.getOrElse(22),
        identities = identities
      )
    )
    val sftp = ssh.newSftp
    log.info("Exec before deploy")
    execBefore.foreach(_(ssh))
    val deployInfo = artifacts.foldLeft(Seq.empty[(File, File, File)]) {
      (files, artifact) =>
        val workDir = getWorkDirectory(artifact.path)
        files ++ getFilesFromDirectory(artifact.path)
          .map((_, workDir, file(artifact.remoteDir)))
    }
    var counter = 0
    deployInfo.foreach { case (file, workDir, remoteDir) =>
      print(s"\rSend ${counter+=1;counter} of ${deployInfo.size}\r")
      deployFile(ssh, sftp, file, workDir, remoteDir, log)
    }
    log.info(s"Sent ${deployInfo.size} file(s)")
    log.info("Exec after deploy")
    execAfter.foreach(_(ssh))
    sftp.close()
    ssh.close()
  }

  private[this] def deployFile(ssh: SSH, transferProtocol: TransfertOperations,
                               fileToDeploy: File, workDirectory: File,
                               remoteDir: File, log: Logger): Unit = {
    val destDirectory =
      getDestinationFile(workDirectory, fileToDeploy.getParentFile, remoteDir)
    val destFile = getDestinationFile(workDirectory, fileToDeploy, remoteDir)
    log.debug(s"Send $fileToDeploy to $destDirectory")
    destDirectory.foreach(ssh.mkdir)
    destFile.foreach(transferProtocol.send(fileToDeploy, _))
  }

  private[this] def getDestinationFile(pathToFolderWithFile: File, localFile: File,
                                       remoteDir: File): Option[String] = {
    Some((localFile.relativeTo(pathToFolderWithFile) match {
      case Some(relativePath) => new File(remoteDir, relativePath.getPath).getPath
      case _ => remoteDir.getPath
    }).replaceAll(java.util.regex.Matcher.quoteReplacement("\\"), "/"))
  }

  private[this] def getWorkDirectory(path: File): File = {
    if (path.isDirectory) path
    else path.getParentFile
  }

  private[this] def getFilesFromDirectory(directory: File): Seq[File] = {
    if (directory.isDirectory)
      directory.listFiles.filter(_.isFile) ++
        directory.listFiles.filter(_.isDirectory).flatMap(getFilesFromDirectory)
    else Array(directory)
  }
}
