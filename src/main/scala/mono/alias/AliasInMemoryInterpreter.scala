package mono.alias

import cats.~>
import monix.eval.Task

import scala.collection.concurrent.TrieMap

class AliasInMemoryInterpreter extends (AliasOp ~> Task) {
  private val data = TrieMap.empty[String, Alias]

  override def apply[A](fa: AliasOp[A]): Task[A] = fa match {
    case GetAlias(id) ⇒
      Task.now(data(id).asInstanceOf[A])

    case FindAlias(pointer) ⇒
      Task.now(data.values.find(_.pointer == pointer).asInstanceOf[A])

    case TryPointTo(id, pointer) ⇒
      Alias.normalize(Some(id)).flatMap {
        case None ⇒
          Task.now(None)
        case Some(alias) ⇒
          val oldAlias = data.values.find(_.pointer == pointer)

          val a = data.getOrElseUpdate(alias, Alias(alias, pointer))
          if (a.pointer == pointer) {
            oldAlias.foreach(oa ⇒ data.remove(oa.id, oa))
            Task.now(Some(a))
          } else Task.now(None)

      }.map(_.asInstanceOf[A])
  }
}
