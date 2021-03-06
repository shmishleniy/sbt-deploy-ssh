# sbt-deploy-ssh

SBT Deploy Plugin to easily deploy your project.

## Notes. Bintray shutdown 01.05.2021
Please apply changes to `project/plugins.sbt`:
### janalyse resolver
remove:

`resolvers += "JAnalyse Repository" at "http://www.janalyse.fr/repository/"`

### new organization `io.github.shmishleniy`
replace:

`addSbtPlugin("com.github.shmishleniy" % "sbt-deploy-ssh" % "0.1.x")`

with

`addSbtPlugin("io.github.shmishleniy" % "sbt-deploy-ssh" % "0.1.x")`
## Quick Start

Read documentation here below or check [example project](https://github.com/shmishleniy/sbt-deploy-ssh/tree/master/example).

You will be able to deploy your project with `deploy-ssh` task.

Usage example: `deploySsh yourServerName1 yourServerName2 ...`

[SBT's documentation on how to use plugins](http://www.scala-sbt.org/1.x/docs/Using-Plugins.html)

## Table of Contents

 - [Installation](#installation)
 - [Configuration](#configuration)
  - [Configs](#configs)
  - [Locations](#locations)
  - [Artifacts](#artifacts)
 - [Execute scripts before/after deploy](#execute-scripts-beforeafter-deploy)
 - [Link to task](#link-to-task)
 - [Start deploy](#start-deploy)

## Installation

Add to your `project/plugins.sbt` file:

``` sbt
addSbtPlugin("io.github.shmishleniy" % "sbt-deploy-ssh" % "0.1.5")
```

Add import to your project build file

```sbt
import deployssh.DeploySSH._
```

Enable plugin in your project.
For example in your `build.sbt`

``` sbt
lazy val myProject = project.enablePlugins(DeploySSH)
```

## Configuration

### Configs

You can specify configs that will be used for deployment.

You can use `.conf` files or set configs directly in project settings.

Allowed config fields:

* `name` - your server name. **Should be unique** in all loaded configs. (Duplication will be overridden)
* `host` - ip adress or hostname of the server
* `user` - ssh username. If missing or empty will be used your current user (`user.name`)
* `password`- ssh password. If missing or empty will be used ssh key
* `passphrase`- passphrase for ssh key. Remove or leave empty for ssh key without passphrase
* `port` - ssh port. If missing or empty will be used `22`
* `sshDir` - directory with you ssh keys.
This directory should contain `identity`, `id_dsa`, `id_ecdsa`, `id_ed25519` or `id_rsa` (the first matched file in the folder will be used for auth).
By default `user.name/.ssh` directory. This field is not allowed to be empty in `.conf` file.
You should remove this field from config in `.conf` file to use default value.
* `sshKeyFile` - add additional private key file name that will be used for ssh connection.
This file name will be added to head of the default list [`identity`, `id_dsa`, `id_ecdsa`, `id_ed25519`, `id_rsa`].
This field is not allowed to be empty in `.conf` file. You should remove this field from config in `.conf` file to use default value.

**`name` and `host` fields are mandatory**

To set server configs in project settings use `ServerConfig` class and `deployConfigs` task key (see details below in `Locations` section)

``` scala
case class ServerConfig
  ( name: String
  , host: String
  , user: Option[String] = None
  , password: Option[String] = None
  , passphrase: Option[String] = None
  , port: Option[Int] = None
  , sshDir: Option[String] = None
  , sshKeyFile: Option[String] = None
  )
```

Example of the `.conf`
``` conf
servers = [
 #connect to the server via `22` port and ssh key that located in `user.name/.ssh/` directory, user is current `user.name`
 {
  name = "server_0"
  host = "127.0.0.1"
 },
 #connect to the server via `22` port and ssh key with name `id_a12` that located in `/tmp/.sshKeys/` directory, user is `ssh_test`
 {
  name = "server_1"
  host = "169.254.0.2"
  user = "ssh_test"
  sshDir = "/tmp/.sshKeys"
  sshKeyFile = "id_a12" #custom private key file name
 }
]
```

### Locations
There are four places where you can store your server config (All configs will be loaded and merged).

* External config file that located somewhere on your PC
* Config file located in your project directory
* Config file located in user home directory
* Set server configs directly in project settings

``` sbt
lazy val myProject = project.enablePlugins(DeploySSH).settings(
 //load build.conf from external path
 deployExternalConfigFiles ++= Seq("/home/myUser/Documents/build.conf"),
 //load build2.conf from `myProjectDir` and load build3.conf from `myProjectDir/project`
 deployResourceConfigFiles ++= Seq("build2.conf", "project/build3.conf"),
 //load build4.conf from user home directory (in example `/home/myUser/build4.conf`)
 deployHomeConfigFiles ++= Seq("build4.conf"),
 //configuration in project setttings
 deployConfigs ++= mySettings,
 deployConfigs ++= Seq(
  ServerConfig("server_6", "169.254.0.3"),
  ServerConfig("server_7", "169.254.0.4")
 )
)

lazy val mySettings = Seq(
 ServerConfig("server_5", "169.254.0.2")
)
```

### Artifacts
Set artifacts to deploy

``` sbt
lazy val myProject = project.enablePlugins(DeploySSH).settings(
 version := "1.1",
 deployConfigs ++= Seq(
  ServerConfig("server_5", "169.254.0.2")
 ),
 deployArtifacts ++= Seq(
  //`jar` file from `packageBin in Compile` task will be deployed to `/tmp/` directory
  ArtifactSSH((packageBin in Compile).value, "/tmp/"),
  //directory `stage` generated by `sbt-native-packager` will be deployed to `~/stage_1.1_release/` directory
  ArtifactSSH((stage in Universal).value), s"stage_${version.value}_release/")
 )
)
```

Deploy execution for this config:

`deploy-ssh server_5`

or

`deploySsh server_5`

### Execute scripts before/after deploy

Use `deploySshExecBefore` and `deploySshExecAfter` to execute any bash commands before and after deploy.

Any exception in `deploySshExecBefore` and `deploySshExecAfter` will abort deploy for all servers.

To skip deploy only for current server you should wrap exception to `SkipDeployException`.

For example stop and update and run your app, copy with scp needed application.conf depends on server name:

``` sbt
lazy val myProject = project.enablePlugins(DeploySSH).settings(
 deployConfigs ++= Seq(
  ServerConfig("server_5", "169.254.0.2")
 ),
 deploySshExecBefore ++= Seq(
  (ssh: SSH) => {
   ssh.execOnce("touch pid")
   val pid = ssh.execOnceAndTrim("cat pid")
   if ("".equals(pid)) {
    //skip deploy to current server
    throw new SkipDeployException(new RuntimeException("missing pid"))
   }
   ssh.execOnceAndTrim(s"kill $pid")
  }
 ),
 deploySshExecAfter ++= Seq(
  (ssh: SSH) => {
   ssh.scp { scp =>
     scp.send(file(s"./src/main/resources/application-${ssh.options.name.get}.conf"), s"/home/app/application.conf")))
   }
   ssh.execOnce("nohup ./myApp/run & echo $! > ~/pid")
   ssh.execOnce("touch pid")
   val pid = ssh.execOnceAndTrim("cat pid")
   if ("".equals(pid)) {
    //stop deploy to all servers
    throw new RuntimeException("missing pid. please check package")
   }
  }
 )
)
```

### Link to task

If you need execute deploy in your task you can use `deploySshTask` and `deploySshServersNames` to config args for `deploySsh`. Or cast `deploySsh` to task.

``` sbt
lazy val myProject = project.enablePlugins(DeploySSH).settings(
 deployConfigs ++= Seq(
  ServerConfig("server_5", "169.254.0.2"),
  ServerConfig("server_6", "169.254.0.3")
 ),
 deploySshServersNames ++= Seq("server_5", "server_6"),
 publishLocal := deploySshTask //or deploySsh.toTask(" server_5 server_6")
)
```

## Start deploy

After confuguration you will be able to:

### Start deploy procedure from sbt console with `deploySsh` input task:

`deploySsh yourServerName1 yourServerName2 ...`

### Start deploy procedure from sbt console with `deploySshTask` task:

You should set `deploySshServersNames` list of server names that will be deployed and execute `deploySshTask` from console or link it to other task.

[See example: "Link to task"](#link-to-task)
