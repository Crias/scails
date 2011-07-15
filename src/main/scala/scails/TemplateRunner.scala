import sbt._
import java.io.FileWriter
import java.util.Properties
import scala.collection.JavaConversions.asScalaSet
import org.fusesource.scalate.util._
import org.fusesource.scalate.TemplateEngine

object TemplateRunner {
  val engine = new TemplateEngine()

  def runTemplate(directory : String, templateArguments : Map[String,Any]) {
    readProps(directory+"template.properties").foreach { entry => 
      val (key,value) = (entry.getKey().toString, entry.getValue()) 
      val templateResource = directory.replace("/","_") + key.replace("/","_")
      if(value == null || "" == value.toString)
        IO.transfer(resourceStream(directory + key), file(key))
      else
        IO.write(file(value.toString), engine.layout(templateResource, templateArguments))
    }
    readProps(directory+"directory.properties").foreach { entry => 
      file(entry.getKey.toString).mkdirs
    }
  }
  
  def readProps(name:String) = {
    val templates = new Properties()
    templates.load(resourceStream(name))
    templates.entrySet
  }

  def resourceStream(name : String) = this.getClass().getClassLoader.getResourceAsStream(name)
}