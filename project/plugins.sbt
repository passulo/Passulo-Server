// adds reStart and reStop
addSbtPlugin("io.spray" % "sbt-revolver" % "0.10.0")

// adds scalafmt
addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.5.2")

// Enable several package formats, especially docker.
// sbt> docker:publishLocal
// sbt> docker:publish
addSbtPlugin("com.github.sbt" % "sbt-native-packager" % "1.9.8")

// https://github.com/playframework/twirl#sbt-twirl
addSbtPlugin("com.typesafe.play" % "sbt-twirl" % "1.6.2")
