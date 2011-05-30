import sbt._

class MongoScalaThingyProject(info: ProjectInfo) extends DefaultProject(info)
{
    val scalajCollection = "org.scalaj" % "scalaj-collection_2.8.0" % "1.0"
    val casbahCore = "com.mongodb.casbah" %% "casbah-core" % "2.1.2"
    val liftJson = "net.liftweb" %% "lift-json" % "2.3"
    val scalap = "org.scala-lang" % "scalap" % "2.8.1"
    val commonsCodec = "commons-codec" % "commons-codec" % "1.4"

    val junitInterface = "com.novocode" % "junit-interface" % "0.7" % "test->default"
}