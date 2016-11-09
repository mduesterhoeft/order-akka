name := """order-akka"""

version := "1.0"

scalaVersion := "2.11.7"
val akkaVersion = "2.4.11"

libraryDependencies ++= Seq(
  "com.typesafe.akka"         %% "akka-actor"                         % akkaVersion,
  "com.typesafe.akka"         %% "akka-http-core"                     % akkaVersion,
  "com.typesafe.akka"         %% "akka-http-experimental"             % akkaVersion,
  "com.typesafe.akka"         %% "akka-http-spray-json-experimental"  % akkaVersion,
  "com.typesafe.akka"         %% "akka-persistence"                   % akkaVersion,
  "org.iq80.leveldb"           % "leveldb"                            % "0.7",
  "org.fusesource.leveldbjni"  % "leveldbjni-all"                     % "1.8",

  "io.scalac"                 %% "reactive-rabbit"                    % "1.1.2",

  "com.typesafe.akka"         %% "akka-testkit"                       % akkaVersion   % "test",
  "org.scalatest"              % "scalatest_2.11"                     % "3.0.0"       % "test"
)
