import sbt._
import Keys._
import Project.Initialize

import java.util.Properties
import java.io.FileInputStream

import scala.collection.JavaConversions.asScalaSet

object MyBuild extends Build {
  import com.zentrope.ScalatePlugin._

  val templateDirectory = "target/templates-prepared"

  lazy val root = Project("root", file(".")) settings(
    sbtPlugin := true,
    name := "Scails",
    organization := "com.codequirks",
    version := "0.1.0",

    cleanFiles <<= (cleanFiles, baseDirectory) { 
      (files, basedir) => 
        files ++ Seq(new File(basedir, "/" + templateDirectory))
    },

    libraryDependencies ++= Seq(
      "org.fusesource.scalate" % "scalate-core" % "1.4.1" % "compile",
      "ch.qos.logback" %  "logback-classic" % "0.9.28" % "compile"
    ),

    sourceGenerators in Compile :== Nil,
    sourceGenerators in Compile <+= (streams, sourceManaged in Compile, scalateLoggingConfig in Compile) map { 
      (out, outputDir, logConfig) => 
        processDirectory(Seq[String](), file("src/main/resources"))
        generateScalateSource(out, outputDir, Seq(file(templateDirectory)), logConfig) 
    }
  )

  def processDirectory(basePath : Seq[String], dir : File) {
    file(templateDirectory).mkdirs
    dir.listFiles.filter(_.getName == "template.properties").foreach(copyTemplates(basePath, _))
    dir.listFiles.filter(_.isDirectory).foreach(f => processDirectory(basePath :+ f.getName, f))
  }
  
  def copyTemplates(basePath : Seq[String], propsFile : File) {
    val props = new Properties
    val templatePrefix = basePath.mkString("","_","_")
    props.load(new FileInputStream(propsFile))
    props.keySet.foreach { keyO =>
      val key = keyO.toString 
      if(isTemplate(key)) {
        val template = file(propsFile.getParent+"/"+key)
        val templateTarget = key.replace("/", "_")
        val target = file(templateDirectory+"/"+templatePrefix+templateTarget)
        IO.copyFile(template, target)
      }
    }
  }
  
  def isTemplate(key : String) = {
    key.endsWith(".scaml") || 
    key.endsWith(".jade") || 
    key.endsWith(".ssp") ||
    key.endsWith(".mousetache")
  }

}