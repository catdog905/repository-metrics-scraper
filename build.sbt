ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.14"

lazy val root = (project in file("."))
  .settings(
    name := "repository-metrics-scraper",
    libraryDependencies ++= Seq(
      "com.lihaoyi" %% "requests" % "0.8.0",
      "com.softwaremill.sttp.client4" %% "core" % "4.0.0-M14",
      "com.lihaoyi" %% "ujson" % "3.3.1",
      "io.circe" %% "circe-core" % "0.14.7",
      "io.circe" %% "circe-generic" % "0.14.7",
      "io.circe" %% "circe-parser" % "0.14.7",

      "org.typelevel" %% "cats-core" % "2.10.0",

      // Start with this one
      "org.tpolecat" %% "doobie-core"      % "1.0.0-RC4",

      // And add any of these as needed
      "org.tpolecat" %% "doobie-h2"        % "1.0.0-RC4",          // H2 driver 1.4.200 + type mappings.
      "org.tpolecat" %% "doobie-hikari"    % "1.0.0-RC4",          // HikariCP transactor.
      "org.tpolecat" %% "doobie-postgres"  % "1.0.0-RC4",          // Postgres driver 42.6.0 + type mappings.

      // https://mvnrepository.com/artifact/com.github.mauricioaniche/ck
      "com.github.mauricioaniche" % "ck" % "0.7.0"

    )
  )
