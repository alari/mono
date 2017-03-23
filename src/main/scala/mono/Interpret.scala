package mono

import cats.data.Coproduct
import cats.~>
import monix.eval.Task
import mono.alias.{ AliasInMemoryInterpreter, AliasOp }
import mono.article.{ ArticleOp, ArticlesInMemoryInterpreter }
import mono.author.{ AuthorOp, AuthorsInMemoryInterpreter }
import mono.env.{ EnvConfigInterpreter, EnvOp }

object Interpret {

  type Op0[A] = Coproduct[ArticleOp, AuthorOp, A]

  type Op1[A] = Coproduct[EnvOp, Op0, A]

  type Op[A] = Coproduct[AliasOp, Op1, A]

  def inMemory: Op ~> Task = {
    val i0: Op0 ~> Task = new ArticlesInMemoryInterpreter or new AuthorsInMemoryInterpreter
    val i1: Op1 ~> Task = new EnvConfigInterpreter() or i0
    new AliasInMemoryInterpreter or i1
  }
}
