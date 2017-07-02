package mono.core.alias

import com.ibm.icu.text.{ Normalizer2, Transliterator }
import monix.eval.Task

import scala.util.Try

case class Alias(
  id:      String,
  pointer: AliasPointer
)

object Alias {
  private val norm = (n: String) ⇒ Normalizer2.getNFDInstance.normalize(n).replaceAll("\\s+", "-").replaceAll("[^-_a-zA-Z0-9]", "").toLowerCase
  private val translit = (n: String) ⇒ Transliterator.getInstance("Any-Latin; NFD").transform(n)

  def normalize(alias: Option[String]): Task[Option[String]] =
    Task.fromTry(Try(alias.map(translit andThen norm).filter(_.length > 3).map(_.take(54))))

}