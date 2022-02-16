lazy val root = (project in file("."))
  .settings(
    name          := "scala-start",
    organization  := "my-organization",
    version       := "1.0.0",
    scalaVersion  := "2.13.8",
    scalacOptions := scalaCompilerOptions,
    libraryDependencies ++= akkaDependencies ++ databaseDependencies ++ jsonDependencies ++ testDependencies ++ loggingDependencies
  )
  .enablePlugins(JavaAppPackaging, DockerPlugin)

val akkaVersion     = "2.6.18"
val akkaHttpVersion = "10.2.8"

lazy val akkaDependencies = Seq(
  "com.typesafe.akka" %% "akka-actor-typed"         % akkaVersion,
  "com.typesafe.akka" %% "akka-stream"              % akkaVersion,
  "com.typesafe.akka" %% "akka-http"                % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion     % Test,
  "com.typesafe.akka" %% "akka-http-testkit"        % akkaHttpVersion % Test,
  "ch.megard"         %% "akka-http-cors"           % "1.1.3"
)
lazy val databaseDependencies = Seq(
  "org.postgresql"       % "postgresql"          % "42.3.3",
  "com.h2database"       % "h2"                  % "2.1.210",
  "com.typesafe.slick"  %% "slick"               % "3.3.3",
  "com.typesafe.slick"  %% "slick-hikaricp"      % "3.3.3",
  "com.github.tminglei" %% "slick-pg"            % "0.20.2",
  "com.github.tminglei" %% "slick-pg_circe-json" % "0.20.2"
)

val circeVersion = "0.14.1"

lazy val jsonDependencies = Seq(
  "io.circe"          %% "circe-core"      % circeVersion,
  "io.circe"          %% "circe-generic"   % circeVersion,
  "io.circe"          %% "circe-parser"    % circeVersion,
  "de.heikoseeberger" %% "akka-http-circe" % "1.39.2"
)

lazy val testDependencies = Seq(
  "org.scalatest" %% "scalatest"     % "3.2.11" % Test,
  "org.mockito"   %% "mockito-scala" % "1.17.0" % Test
)

lazy val log4JVersion = "2.17.1"

lazy val loggingDependencies = Seq(
  // scala-logging wraps SLF4J, which can use log4j2
  "com.typesafe.scala-logging" %% "scala-logging"    % "3.9.4",
  "org.apache.logging.log4j"    % "log4j-api"        % log4JVersion,
  "org.apache.logging.log4j"    % "log4j-core"       % log4JVersion,
  "org.apache.logging.log4j"    % "log4j-slf4j-impl" % log4JVersion % "runtime",
  "com.lmax"                    % "disruptor"        % "3.4.4"      % "runtime"
)

lazy val scalaCompilerOptions = Seq(
  "-target:11",
  "-deprecation", // Emit warning and location for usages of deprecated APIs.
  "-encoding",
  "utf-8",         // Specify character encoding used by source files.
  "-explaintypes", // Explain type errors in more detail.
  "-feature",      // Emit warning and location for usages of features that should be imported explicitly.
  "-unchecked",    // Enable additional warnings where generated code depends on assumptions.
  "-Xcheckinit",   // Wrap field accessors to throw an exception on uninitialized access.
//  "-Xfatal-warnings",              // Fail the compilation if there are any warnings.
  "-Xlint:adapted-args",           // Warn if an argument list is modified to match the receiver.
  "-Xlint:constant",               // Evaluation of a constant arithmetic expression results in an error.
  "-Xlint:delayedinit-select",     // Selecting member of DelayedInit.
  "-Xlint:doc-detached",           // A Scaladoc comment appears to be detached from its element.
  "-Xlint:inaccessible",           // Warn about inaccessible types in method signatures.
  "-Xlint:infer-any",              // Warn when a type argument is inferred to be `Any`.
  "-Xlint:missing-interpolator",   // A string literal appears to be missing an interpolator id.
  "-Xlint:nullary-unit",           // Warn when nullary methods return Unit.
  "-Xlint:option-implicit",        // Option.apply used implicit view.
  "-Xlint:package-object-classes", // Class or object defined in package object.
  "-Xlint:poly-implicit-overload", // Parameterized overloaded implicit methods are not visible as view bounds.
  "-Xlint:private-shadow",         // A private field (or class parameter) shadows a superclass field.
  "-Xlint:stars-align",            // Pattern sequence wildcard must align with sequence component.
  "-Xlint:type-parameter-shadow",  // A local type parameter shadows a type already in scope.
  "-Ywarn-dead-code",              // Warn when dead code is identified.
  "-Ywarn-extra-implicit",         // Warn when more than one implicit parameter section is defined.
  "-Ywarn-numeric-widen",          // Warn when numerics are widened.
  "-Ywarn-unused:implicits",       // Warn if an implicit parameter is unused.
  "-Ywarn-unused:imports",         // Warn if an import selector is not referenced.
  "-Ywarn-unused:locals",          // Warn if a local definition is unused.
  "-Ywarn-unused:params",          // Warn if a value parameter is unused.
  "-Ywarn-unused:patvars",         // Warn if a variable bound in a pattern is unused.
  "-Ywarn-unused:privates",        // Warn if a private member is unused.
  "-Ywarn-value-discard",          // Warn when non-Unit expression results are unused.
  "-Xsource:3"                     // use Scala 3 syntax
)
