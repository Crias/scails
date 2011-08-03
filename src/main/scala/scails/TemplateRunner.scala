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
      val key = translateProp(entry.getKey, templateArguments)
      val value = translateProp(entry.getValue, templateArguments)
      val templateResource = directory.replace("/","_") + key.replace("/","_")
      val target = if(value == null || value == "") key else value

      if(!isTemplate(key)) 
        IO.transfer(resourceStream(directory + key), file(target))
      else
        IO.write(file(value.toString), engine.layout(templateResource, templateArguments))
    }

    readProps(directory+"directory.properties").foreach { entry => 
      file(entry.getKey.toString).mkdirs
    }
  }

  def readProps(name:String) = {
    val resource = resourceStream(name)
    val templates = new Properties()
    if(resource != null) templates.load(resource)
    templates.entrySet
  }

  def translateProp(entry : Any, templateArguments : Map[String,Any]) = {
    templateArguments.foldLeft(entry.toString) { case (current, (key,value)) => current.replace("${"+key+"}", value.toString) }
  }
  
  def isTemplate(file : String) = 
    file.endsWith(".scaml") || 
    file.endsWith(".jade") || 
    file.endsWith(".ssp") || 
    file.endsWith(".moustache") ||
    file.endsWith(".md") ||
    file.endsWith(".markdown")

  def resourceStream(name : String) = this.getClass().getClassLoader.getResourceAsStream(name)
}