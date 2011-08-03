import scala.util.parsing.combinator.syntactical._
import scala.util.parsing.combinator.token.StdTokens
import scala.util.parsing.combinator.lexical.StdLexical
import scala.util.parsing.input.CharArrayReader.EofCh

object LiftMenuParser extends StandardTokenParsers {
  override val lexical : StdLexical = new StdLexical {
    override def whitespaceChar = elem("space char", ch => ch <= ' ' && ch != '\n' && ch != EofCh)
  }
  lexical.reserved ++= List("package", "import", "object", "class", "val", "List", "BaseMetaMapper", "Menu", "i")
  lexical.delimiters ++= List("[", "]", ".","\n","{", "}", "(", ")", "=", ";", ",", "/", "**")

  def eos = ("\n" | ";")

  lazy val `program` = rep(eos) ~ `package` ~ rep1(eos) ~ rep1(`import`) ~ rep1(eos) ~ `object` ~ rep(eos) ^^ { 
    case _~p~_~i~_~o~_ => SProgram(p,i,o) 
  }

  lazy val `package` = rep("\n") ~ "package" ~> rep1sep(ident, ".") ^^ { 
    case n => SPackage(n) 
  }

  lazy val `import` = rep("\n") ~ "import" ~> rep1sep(ident, ".") ^^ {
    case l => SImport(l)
  }

  lazy val `object` = "object" ~> ident ~ `block` ^^ { 
    case n~b => SObject(n,b._1,b._2) 
  }

  lazy val `block` = "{" ~ rep(eos) ~> (`menuThenTypes` | `typesThenMenu`) <~ rep(eos) ~ "}" ^^ { 
    case m => m 
  }

  lazy val `menuThenTypes` = `menuDef` ~ `typeListDef` ^^ { case m~t => (m,t) }
  lazy val `typesThenMenu` = `typeListDef` ~ `menuDef` ^^ { case t~m => (m,t) }

  lazy val `menuDef` = rep("\n") ~ "val" ~> ident ~ "=" ~ "List" ~ "(" ~ repsep(`menuItem`, ",") <~ ")" ^^ {
    case id~_~_~_~items => new SMenu(id, items)
  }

  lazy val `menuItem` = rep("\n") ~ "Menu" ~ "." ~ "i" ~ "(" ~> stringLit ~ ")" ~ "/" ~ stringLit <~ "/" ~ "**" ~ rep("\n") ^^ {
    case link~_~_~loc => SMenuItem(link, loc)
  }

  lazy val `typeListDef` = rep("\n") ~ "val" ~> ident ~ "=" ~ "List"  ~ "[" ~ "BaseMetaMapper" ~ "]" ~ "(" ~ repsep(`typeListItem`, ",") <~ ")" ^^ {
    case id~_~_~_~_~_~_~items => new SList(id, items)
  }

  lazy val `typeListItem` = rep("\n") ~> ident <~ rep("\n") ^^ {
    case id => SListItem(id)
  }

  def parse(s:String) = {
    val tokens = new lexical.Scanner(s)
    phrase(`program`)(tokens)
  }

  def apply(s : String) = parse(s) match {
    case Success(v,_) => Some(v)
    case e : NoSuccess => {println(e); None }
  }
}

sealed abstract class Expr {}
case class SProgram(pack : SPackage, imp : List[SImport], obj : SObject) {
  override def toString = pack + "\n\n" + imp.mkString("\n") + "\n\n" + obj
  def addScaffold(properName : String) = SProgram(pack, imp, obj.addScaffold(properName))
}
case class SPackage(packagePath : List[String]) extends Expr {
  override def toString = "package " + packagePath.mkString(".")
}
case class SImport(importPath : List[String]) extends Expr {
  override def toString = "import " + importPath.mkString(".")
}
case class SObject(name : String, menu : SMenu, typeList : SList) extends Expr {
  override def toString = "object " + name + " {\n" + menu +  "\n" + typeList + "\n}"
  def addScaffold(properName : String) = SObject(this.name, menu.addMenuItem(properName+"s", properName.toLowerCase+"s"), typeList.addItem(properName))
}
case class SMenu(name : String, menuItems : List[SMenuItem]) extends Expr {
  override def toString = "  val " + name + " = List(\n" + menuItems.mkString(",\n") + ")"
  def addMenuItem(name : String, loc : String) = SMenu(this.name, menuItems :+ SMenuItem(name, loc))
}
case class SMenuItem(link : String, loc : String) extends Expr {
  override def toString = "    Menu.i(\""+link+"\") / \"" + loc + "\" / **"
}
case class SList(name : String, items : List[SListItem]) extends Expr {
  override def toString = "  val " + name + " = List[BaseMetaMapper](\n" + items.mkString(",\n") + ")"
  def addItem(identifier : String) = SList(this.name, items :+ SListItem(identifier))
}
case class SListItem(identifier : String) extends Expr {
  override def toString = "    " + identifier
}
