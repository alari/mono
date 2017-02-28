import com.typesafe.sbt.SbtScalariform.ScalariformKeys
import sbt.Keys._

import scalariform.formatter.preferences._

name := "monolith"

version := "1.0"

scalaVersion := "2.12.1"

val commonScalariform = scalariformSettings :+ (ScalariformKeys.preferences := ScalariformKeys.preferences.value
  .setPreference(AlignParameters, true)
  .setPreference(AlignSingleLineCaseStatements, true)
  .setPreference(DoubleIndentClassDeclaration, true)
  .setPreference(PreserveSpaceBeforeArguments, true)
  .setPreference(RewriteArrowSymbols, true))

commonScalariform

mainClass := Some("mono.MonoApp")

resolvers ++= Seq(
  "Typesafe Repo" at "http://repo.typesafe.com/typesafe/releases/",
  Resolver.sonatypeRepo("snapshots")
)

val monixV = "2.2.2"
val akkaV = "2.4.17"
val akkaHttpV = "10.0.3"
val circeV = "0.7.0"
val doobieV = "0.4.1"

val logback = "ch.qos.logback" % "logback-classic" % "1.1.7"
val scalatest = "org.scalatest" %% "scalatest" % "3.0.1" % Test

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-http" % akkaHttpV,
  "com.typesafe.akka" %% "akka-stream" % akkaV,
  "info.mukel" %% "telegrambot4s" % "2.1.0-SNAPSHOT",
  "org.typelevel" %% "cats" % "0.9.0"  ,
  "io.monix" %% "monix" % monixV,
  "io.monix" %% "monix-cats" % monixV,
  "org.tpolecat" %% "doobie-core-cats" % doobieV,
  "org.tpolecat" %% "doobie-postgres-cats" % doobieV,
  logback,
  scalatest
)

//libraryDependencies ++= Seq(
//  "io.circe" %% "circe-core",
//  "io.circe" %% "circe-generic",
//  "io.circe" %% "circe-parser"
//).map(_ % circeV)

enablePlugins(SbtTwirl)