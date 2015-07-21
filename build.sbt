import com.typesafe.tools.mima.plugin.MimaKeys.previousArtifact

name := "socrata-utils"

previousArtifact <<= scalaBinaryVersion { sv => Some("com.socrata" % ("socrata-utils_" + sv) % "0.7.1") }

crossScalaVersions := Seq("2.8.1", "2.9.2")

libraryDependencies ++= Seq(
  "com.rojoma"        %% "simple-arm"   % "[1.1.10,2.0.0)",
  "com.yammer.metrics" % "metrics-core" % "2.0.3",
  "joda-time"          % "joda-time"    % "2.6",
  "org.joda"           % "joda-convert" % "1.2",
  "org.slf4j"          % "slf4j-simple" % "1.7.5" % "test"
)

libraryDependencies <++= (scalaVersion) {
  case "2.8.1" =>
    Seq("org.scalacheck" % "scalacheck_2.8.1" % "1.8" % "test",
        "org.scalatest" % "scalatest_2.8.1" % "1.8" % "test")
  case _ =>
    Seq("org.scalacheck" %% "scalacheck" % "1.12.4" % "test")
}

testOptions in Test += Tests.Setup { loader =>
  loader.loadClass("org.slf4j.LoggerFactory").getMethod("getILoggerFactory").invoke(null)
}

// TODO: enable code coverage build failures
scoverage.ScoverageSbtPlugin.ScoverageKeys.coverageFailOnMinimum := false
// TODO: enable scalastyle build failures
com.socrata.sbtplugins.StylePlugin.StyleKeys.styleFailOnError in Compile := false

scalacOptions <++= (scalaVersion) map {
  case s if s.startsWith("2.8.") => Nil
  case s if s.startsWith("2.9.") => Nil
  case s if s.startsWith("2.10.") =>
    Seq(// I would prefer to turn these on per-scope as appropriate
        // but can't do it while keeping 2.[8,9] compatibility.
      "-language:higherKinds", "-language:implicitConversions")
}

sourceGenerators in Compile <+= (sourceManaged in Compile, scalaVersion in Compile) map { (baseDir, scalaVersion) =>
  import java.io._
  import com.rojoma.simplearm.util._
  val targetDir = baseDir / "com" / "socrata" / "util-impl"
  val targetFile = targetDir / "ErrorImpl.scala"
  targetDir.mkdirs()
  for {
    stream <- managed(new FileOutputStream(targetFile))
    writer <- managed(new OutputStreamWriter(stream, "UTF-8"))
    printer <- managed(new PrintWriter(writer))
  } {
    printer.println("package com.socrata.`util-impl`")
    printer.println()
    printer.println("object ErrorImpl {")
    printer.println("  @inline def error(message: String): Nothing = {")
    scalaVersion match {
      case s if s.startsWith("2.8.") => printer.println("    Predef.error(message)")
      case s if s.startsWith("2.9.") => printer.println("    sys.error(message)")
      case s if s.startsWith("2.10.") => printer.println("    sys.error(message)")
    }
    printer.println("  }")
    printer.println("}")
  }
  Seq(targetFile)
}

// bllllleargh -- 2.8's doc process blows up thanks to SI-4284
publishArtifact in (Compile, packageDoc) <<= scalaVersion { sv =>
  !sv.startsWith("2.8.")
}
