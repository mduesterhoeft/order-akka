name := """order-akka"""

version := "1.0"

scalaVersion := "2.12.1"
val akkaVersion = "2.4.16"
val akkaHttpVersion = "10.0.1"

libraryDependencies ++= Seq(
  "ch.qos.logback"             % "logback-classic"                    % "1.1.7",
  "com.typesafe.akka"         %% "akka-slf4j"                         % akkaVersion,
  "com.typesafe.akka"         %% "akka-actor"                         % akkaVersion,
  "com.typesafe.akka"         %% "akka-http"                          % akkaHttpVersion,
  "com.typesafe.akka"         %% "akka-http-spray-json"               % akkaHttpVersion,
  "com.typesafe.akka"         %% "akka-persistence"                   % akkaVersion,
  "com.typesafe.akka"         %% "akka-persistence-cassandra"         % "0.20",
  "com.typesafe.akka"         %% "akka-cluster"                       % akkaVersion,
  "com.typesafe.akka"         %% "akka-cluster-tools"                 % akkaVersion,
  "com.typesafe.akka"         %% "akka-cluster-sharding"              % akkaVersion,
  "org.iq80.leveldb"           % "leveldb"                            % "0.7",
  "org.fusesource.leveldbjni"  % "leveldbjni-all"                     % "1.8",

  "com.lightbend.akka"        %% "akka-stream-alpakka-amqp"           % "0.5",

  "com.typesafe.akka"         %% "akka-testkit"                       % akkaVersion       % "test",
  "com.typesafe.akka"         %% "akka-http-testkit"                  % akkaHttpVersion   % "test",
  "org.scalatest"             %% "scalatest"                          % "3.0.1"           % "test"
)
