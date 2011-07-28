import scala.util.parsing.combinator.syntactical._
import scala.util.parsing.combinator.token.StdTokens
import scala.util.parsing.combinator.lexical.StdLexical
import scala.util.parsing.input.CharArrayReader.EofCh

object LiftMenuParser extends StandardTokenParsers {
  override val lexical : StdLexical = new StdLexical {
    override def whitespaceChar = elem("space char", ch => ch <= ' ' && ch != '\n' && ch != EofCh)
  }
  lexical.reserved ++= List("package", "object", "class", "val", "List", "Menu", "i")
  lexical.delimiters ++= List(".","\n","{", "}", "(", ")", "=", ";", ",", "/", "**")

  def eos = ("\n" | ";")

  lazy val `program` = rep(eos) ~ `package` ~ rep1(eos) ~ rep1(`import`) ~ rep1(eos) ~ `object` ~ rep(eos) ^^ { 
    case _~p~_~i~_~o~_ => SProgram(p,i,o) 
  }

  lazy val `package` = "package" ~> ident ^^ { 
    case n => SPackage(n) 
  }

  lazy val `import` = "import" ~> rep1sep(ident, ".") ^^ {
    case l => SImport(l)
  }

  lazy val `object` = "object" ~> ident ~ `block` ^^ { 
    case n~m => SObject(n,m) 
  }

  lazy val `block` = "{" ~ rep(eos) ~> `menuDef` <~ rep(eos) ~ "}" ^^ { 
    case m => m 
  }

  lazy val `menuDef` = "val" ~> ident ~ "=" ~ "List" ~ "(" ~ repsep(`menuItem`, ",") <~ ")" ^^ {
    case id~_~_~_~items => new SMenu(id, items)
  }

  lazy val `menuItem` = rep("\n") ~ "Menu" ~ "." ~ "i" ~ "(" ~> stringLit ~ ")" ~ "/" ~ stringLit <~ "/" ~ "**" ~ rep("\n") ^^ {
    case link~_~_~loc => SMenuItem(link, loc)
  }

  def parse(s:String) = {
    val tokens = new lexical.Scanner(s)
    phrase(`program`)(tokens)
  }

  def apply(s : String) = parse(s) match {
    case Success(v,_) => Some(v)
    case e : NoSuccess => None
  }
}

sealed abstract class Expr {}
case class SProgram(pack : SPackage, imp : List[SImport], obj : SObject) {
  override def toString = pack + "\n\n" + imp.mkString("\n") + "\n\n" + obj
  def addMenuItem(name : String, loc : String) = SProgram(pack, imp, obj.addMenuItem(name, loc))
}
case class SPackage(name : String) extends Expr {
  override def toString = "package " + name
}
case class SImport(importPath : List[String]) extends Expr {
  override def toString = "import " + importPath.mkString(".")
}
case class SObject(name : String, menu : SMenu) extends Expr {
  override def toString = "object " + name + " {\n" + menu + "\n}"
  def addMenuItem(name : String, loc : String) = SObject(this.name, menu.addMenuItem(name, loc))
}
case class SMenu(name : String, items : List[SMenuItem]) extends Expr {
  override def toString = "  val " + name + " = List(\n" + items.mkString(",\n") + ")"
  def addMenuItem(name : String, loc : String) = SMenu(this.name, items :+ SMenuItem(name, loc))
}
case class SMenuItem(link : String, loc : String) extends Expr {
  override def toString = "    Menu.i(\""+link+"\") / \"" + loc + "\" / **"
}
