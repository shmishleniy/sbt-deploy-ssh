# sbt-deploy-ssh
SBT deploy plugin

Allows you to setup deploy configuration for your project. 

**autoplugin (sbt >= 0.13.5)**

[Please read sbt documentation before start to work with plugin](http://www.scala-sbt.org/0.13.5/docs/Getting-Started/Using-Plugins.html)

## Usage

### Installation

Add to your project/plugins.sbt file:
``` sbt
addSbtPlugin("com.github.shmishleniy" % "sbt-deploy-ssh" % "0.1")
```
Enable plugin in your project.
For example in your `build.sbt`
``` sbt
lazy val myProject = project.enablePlugins(DeploySSH)
```

### Configuration

#### Locations
There are four places where you can store your server config.

* Extrenal config file that located somewhere on your PC
* Config file located in your project directory
* Config file located in user home directory
* Set server configs directly in project settings
``` sbt 
lazy val myProject = project.enablePlugins(DeploySSH).settings(
  //load build.conf from external path
  deployExternalConfigFile ++= Seq("/home/myUser/Documents/build.conf"),
  //load build2.conf from `myProjectDir` and load build3.conf from `myProjectDir/projects`
  deployResourceConfigFile ++= Seq("build2.conf", "projects/build3.conf"),
  //load build4.conf from user home directory (in example `/home/myUser/build4.conf`)
  deployHomeConfigFile ++= Seq("build4.conf"),
  //configuration in project setttings
  deployConfigs ++= Seq(
    ServerConfig("server_5", "127.0.0.1")
  )
)
```

#### Configs
`name` and `host` are two mandatory fields.
...
All specified configs will be loaded before deploy.

Server name should be unique.
