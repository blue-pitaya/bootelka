import org.scalajs.linker.interface.ESVersion
import org.scalajs.linker.interface.OutputPatterns
import org.scalajs.linker.interface.ModuleSplitStyle

ThisBuild / scalaVersion := "2.13.8"
ThisBuild / version := "0.1.0"
ThisBuild / organization := "com.example"
ThisBuild / organizationName := "example"
Global / scalacOptions += "-Ymacro-annotations"
Global / scalacOptions += "-Wunused:imports"

val CatsVersion = "2.8.0"
val CatsEffectVersion = "3.3.14"
val Http4sVersion = "0.23.23"
val CirceVersion = "0.14.5"
val MunitVersion = "0.7.29"
val LogbackVersion = "1.4.11"
val MunitCatsEffectVersion = "1.0.7"
val MonocleVersion = "3.1.0"
val LaminarVersion = "16.0.0"
val WaypointVersion = "7.0.0"
val LaminarDraggingVersion = "1.1"
val LaminextVersion = "0.16.2"
val ScalatestVersion = "3.2.13"
val Http4sDomVersion = "0.2.9"
val Password4jVersion = "1.7.1"

lazy val core = crossProject(JSPlatform, JVMPlatform)
  .withoutSuffixFor(JVMPlatform)
  .crossType(CrossType.Pure)
  .in(file("core"))
  .settings(
    name := "core",
    libraryDependencies ++=
      Seq(
        "org.typelevel" %%% "cats-core" % CatsVersion,
        "org.typelevel" %%% "cats-effect" % CatsEffectVersion,
        "io.circe" %%% "circe-generic" % CirceVersion,
        "io.circe" %%% "circe-parser" % CirceVersion,
        "dev.optics" %%% "monocle-core" % MonocleVersion,
        "dev.optics" %%% "monocle-macro" % MonocleVersion,
        "org.scalameta" %% "munit" % MunitVersion % Test
      )
  )
  .jsSettings(
    scalaJSLinkerConfig ~=
      (_.withModuleKind(ModuleKind.ESModule)
        .withESFeatures(_.withESVersion(ESVersion.ES2021))
        .withSourceMap(false)),
    scalaJSUseMainModuleInitializer := false
  )

lazy val api = (project in file("api"))
  .dependsOn(core.jvm)
  .settings(
    name := "api",
    libraryDependencies ++=
      Seq(
        "org.http4s" %% "http4s-ember-server" % Http4sVersion,
        "org.http4s" %% "http4s-ember-client" % Http4sVersion,
        "org.http4s" %% "http4s-dsl" % Http4sVersion,
        "org.http4s" %% "http4s-circe" % Http4sVersion,
        "io.circe" %% "circe-generic" % CirceVersion,
        "org.scalameta" %% "munit" % MunitVersion % Test,
        "org.typelevel" %% "munit-cats-effect-3" % MunitCatsEffectVersion %
          Test,
        "ch.qos.logback" % "logback-classic" % LogbackVersion % Runtime,
        "dev.optics" %%% "monocle-core" % MonocleVersion,
        "dev.optics" %%% "monocle-macro" % MonocleVersion,
        "com.password4j" % "password4j" % Password4jVersion
      ),
    addCompilerPlugin(
      "org.typelevel" %% "kind-projector" % "0.13.2" cross CrossVersion.full
    )
  )
  .enablePlugins(JavaAppPackaging)
  .enablePlugins(DockerPlugin)
  .settings(
    dockerBaseImage := "openjdk",
    dockerExposedPorts := Seq(8080),
    dockerUpdateLatest := true,
    Docker / packageName := "appname-api",
    Docker / version := "latest"
  )

// Used for inserting environment variables before compiling fronted to JS
lazy val macros = (project in file("macros")).settings(
  libraryDependencies += "org.scala-lang" % "scala-reflect" % scalaVersion.value
)

lazy val web = (project in file("web"))
  .dependsOn(core.js)
  .dependsOn(macros)
  .settings(
    name := "web",
    libraryDependencies ++=
      Seq(
        "com.raquo" %%% "laminar" % LaminarVersion,
        "com.raquo" %%% "waypoint" % WaypointVersion,
        // TODO: change to MUnit
        "org.scalatest" %%% "scalatest" % ScalatestVersion % Test,
        "org.typelevel" %%% "cats-core" % CatsVersion,
        "org.typelevel" %%% "cats-effect" % CatsEffectVersion,
        "dev.bluepitaya" %%% "laminar-dragging" % LaminarDraggingVersion,
        "org.http4s" %%% "http4s-circe" % Http4sVersion,
        "io.circe" %%% "circe-generic" % CirceVersion,
        "io.laminext" %%% "fetch-circe" % LaminextVersion,
        "io.laminext" %%% "fetch" % LaminextVersion,
        "dev.optics" %%% "monocle-core" % MonocleVersion,
        "dev.optics" %%% "monocle-macro" % MonocleVersion,
        "org.http4s" %%% "http4s-client" % Http4sVersion,
        "org.http4s" %%% "http4s-dom" % Http4sDomVersion,
        "org.http4s" %%% "http4s-dsl" % Http4sVersion
      ),
    scalaJSUseMainModuleInitializer := true,
    // Development: copmilte to JS
    Compile / fastLinkJS / scalaJSLinkerConfig ~= {
      _.withModuleKind(ModuleKind.ESModule)
        .withOutputPatterns(OutputPatterns.fromJSFile("%s.js"))
        .withESFeatures(_.withESVersion(ESVersion.ES2021))
    },
    Compile / fastLinkJS / scalaJSLinkerOutputDirectory :=
      baseDirectory.value / "ui/sccode/",
    // Production: copmilte to JS -> minify + optimize
    Compile / fullLinkJS / scalaJSLinkerConfig ~= {
      _.withModuleKind(ModuleKind.CommonJSModule)
        .withOutputPatterns(OutputPatterns.fromJSFile("%s.js"))
    },
    Compile / fullLinkJS / scalaJSLinkerOutputDirectory :=
      baseDirectory.value / "ui/sccode/"
  )
  .enablePlugins(ScalaJSPlugin)
