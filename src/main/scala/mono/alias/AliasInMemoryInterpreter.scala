package mono.alias

import cats.~>
import monix.eval.Task

import scala.collection.concurrent.TrieMap

class AliasInMemoryInterpreter extends (AliasOp ~> Task) {
  private val data = TrieMap.empty[String, Alias]

  override def apply[A](fa: AliasOp[A]): Task[A] = fa match {
    case GetAlias(id) ⇒
      data.get(id)
        .fold(Task.raiseError[A](new NoSuchElementException(s"Alias `$id` not found")))(a ⇒
          Task.now(a.asInstanceOf[A]))

    case FindAliases(pointers) ⇒
      Task.now(pointers
        .map(p ⇒ data.values.find(_.pointer == p))
        .collect {
          case Some(a) ⇒ a.pointer → a
        }.asInstanceOf[A])

    case TryPointTo(id, pointer, force) ⇒
      val oldAlias = data.values.find(_.pointer == pointer)
      if (oldAlias.isDefined && !force) {
        Task.now(oldAlias.asInstanceOf[A])
      } else {

        Alias.normalize(Some(id)).flatMap {
          case None ⇒
            Task.now(None)
          case Some(alias) ⇒
            val oldAlias = data.values.find(_.pointer == pointer)

            val a = data.getOrElseUpdate(alias, Alias(alias, pointer))
            if (a.pointer == pointer) {
              oldAlias.filter(_.id != alias).foreach(oa ⇒ data.remove(oa.id, oa))
              Task.now(Some(a))
            } else Task.now(None)

        }
      }.map(_.asInstanceOf[A])
  }
}
