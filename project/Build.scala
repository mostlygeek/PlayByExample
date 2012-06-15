import sbt._
import Keys._
import PlayProject._

object ApplicationBuild extends Build {

    val appName         = "PlayByExample"
    val appVersion      = "1.0-SNAPSHOT"

    val appDependencies = Seq(
        "se.radley" %% "play-plugins-salat" % "1.0.4"
      , "org.clapper" %% "markwrap" % "0.5.4"     // markwrap, for markdown support
    )

    val main = PlayProject(appName, appVersion, appDependencies, mainLang = SCALA).settings(
        routesImport += "se.radley.plugin.salat.Binders._"
      , templatesImport += "org.bson.types.ObjectId"
    )
}
