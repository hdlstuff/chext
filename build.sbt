// See README.md for license details.

ThisBuild / scalaVersion := "2.13.18"
ThisBuild / version := "0.2.3"
ThisBuild / organization := "hdlstuff"

val chiselVersion = "7.6.0"

lazy val root = (project in file("."))
  .settings(
    name := "hdlstuff_chext",
    libraryDependencies ++= Seq(
      "org.chipsalliance" %% "chisel" % chiselVersion,
      "hdlstuff" %% "hdlstuff_hdlinfo" % "0.1.0",
      "com.lihaoyi" %% "sourcecode" % "0.4.4"
    ),
    scalacOptions ++= Seq(
      "-language:reflectiveCalls",
      "-deprecation",
      "-feature",
      "-Xcheckinit",
      "-Ymacro-annotations",
      "-g:vars",
      "-g:line",
      "-g:source"
    ),
    addCompilerPlugin(
      "org.chipsalliance" % "chisel-plugin" % chiselVersion cross CrossVersion.full
    )
  )
