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
val akkaV = "2.5.0-RC1"
val akkaHttpV = "10.0.5"
val circeV = "0.7.0"
val doobieV = "0.4.1"

val logback = "ch.qos.logback" % "logback-classic" % "1.2.2"
val shapeless = "com.chuusai" %% "shapeless" % "2.3.2"
val scalatest = "org.scalatest" %% "scalatest" % "3.0.1" % Test
val icu =  "com.ibm.icu" % "icu4j" % "58.2"
val akkaStreamTest = "com.typesafe.akka" %% "akka-stream-testkit" % akkaV % Test
val cats = "org.typelevel" %% "cats" % "0.9.0"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-http" % akkaHttpV,
  "com.typesafe.akka" %% "akka-stream" % akkaV,
  "info.mukel" %% "telegrambot4s" % "2.1.0-SNAPSHOT",
  cats,
  "io.monix" %% "monix" % monixV,
  "io.monix" %% "monix-cats" % monixV,
  "org.tpolecat" %% "doobie-core-cats" % doobieV,
  "org.tpolecat" %% "doobie-postgres-cats" % doobieV,
  icu,
  shapeless,
  logback,
  scalatest,
  akkaStreamTest
)

//libraryDependencies ++= Seq(
//  "io.circe" %% "circe-core",
//  "io.circe" %% "circe-generic",
//  "io.circe" %% "circe-parser"
//).map(_ % circeV)

enablePlugins(SbtTwirl)