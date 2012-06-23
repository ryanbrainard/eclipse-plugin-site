import sbt._
import Keys._
import PlayProject._

object ApplicationBuild extends Build {

    val appName         = "eclipse-plugin-site"
    val appVersion      = "1.0-SNAPSHOT"

    val appDependencies = Seq(
      "redis.clients" % "jedis" % "2.1.0",
      "org.codehaus.jackson" % "jackson-core-asl" % "[1.9.1,1.9.2)",
      "org.codehaus.jackson" % "jackson-mapper-asl" % "[1.9.1,1.9.2)"
      
    )

    val main = PlayProject(appName, appVersion, appDependencies, mainLang = JAVA).settings(
      // Add your own project settings here      
    )

}
