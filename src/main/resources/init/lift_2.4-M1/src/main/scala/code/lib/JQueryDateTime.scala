package code.lib
import net.liftweb.mapper._
import net.liftweb.common._
import net.liftweb.http._
import java.util.Date
import java.text.SimpleDateFormat
import scala.xml.{NodeSeq}

abstract class JQueryDateTime[T<:Mapper[T]](fieldOwner: T) extends MappedDateTime(fieldOwner) {
  val dateTimeFormatter = new SimpleDateFormat("d/M/yy k:mm")
  val dateFormatter = new SimpleDateFormat("d/M/yy")
  val timeFormatter = new SimpleDateFormat("k:mm")

  override def parse(s : String) : Box[Date] = try {
    Full(dateTimeFormatter.parse(s))
  } catch {
    case _ : Empty
  }
  override def format(d: Date): String = dateTimeFormatter.format(d)

  override def _toForm: Box[NodeSeq] = 
  S.fmapFunc({s: List[String] => this.setFromAny(s)}){funcName =>
  Full(appendFieldId(
    <span>
        <input type="text"
                     name={funcName}
                     id={name + "_date"}
                     value={is match {case null => "" case s => dateFormatter.format(s)}}/>
        <input type="text"
                     name={funcName}
                     id={name + "_time"}
                     value={is match {case null => "" case s => timeFormatter.format(s)}}/>
        <script type="text/javascript">
          $('#{ name + "_date" }').calendricalDate();
          $('#{ name + "_time" }').calendricalTime({"{isoTime: true}"});
        </script>
    </span>))
  }

    override def setFromAny(f: Any): Date = f match {
      case (s1 : String) :: (s2 : String) :: _ => 
        val date = if (s1.contains("/")) parse(s1 + " " + s2) else parse(s2 + " " + s1)
        this.set(date.openOr(this.is))
      case d => super.setFromAny(d)
    }
}
