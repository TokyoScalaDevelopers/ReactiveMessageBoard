name := "ReactiveBlog"

version := "1.0-SNAPSHOT"

libraryDependencies ++= Seq(
  jdbc,
  anorm,
  cache,
  "net.databinder.dispatch" %% "dispatch-core" % "0.11.0"
)     

play.Project.playScalaSettings
