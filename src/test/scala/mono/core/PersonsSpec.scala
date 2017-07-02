package mono.core

import cats.~>
import monix.eval.Task
import mono.core.person._

import scala.util.Random

class PersonsSpec extends OpsSpec[PersonOp] {

  private val persons = implicitly[PersonOps[Command]]

  buildSpec("in memory persons interpreter")(new PersonsInMemoryInterpreter)
  buildSpec("doobie persons interpreter", PersonsDoobieInterpreter.init)(new PersonsDoobieInterpreter(xa))

  override def buildSpec(name: String, init: Init = null)(implicit interpret: Command ~> Task) =
    name should {
      val t1 = Random.nextLong()
      val t2 = Random.nextLong()
      val t3 = Random.nextLong()

      initSpec(init)

      "create person by telegram" in {
        val p = persons.ensureTelegram(t1, s"telegram $t1").unsafeValue

        p.telegramId shouldBe t1

        persons.ensureTelegram(t1, s"telegram $t1 changed").unsafeValue shouldBe p
      }

      "get person by id" in {
        val p = persons.ensureTelegram(t2, s"telegram $t2").unsafeValue

        persons.findByTelegramId(t2).unsafeValue shouldBe Some(p)

        persons.getById(p.id).unsafeValue shouldBe p

        persons.getByIds(Set(p.id)).unsafeValue shouldBe Map[Int, Person](p.id → p)
      }

      "get several persons by ids" in {
        val p1 = persons.ensureTelegram(t1, s"telegram $t1").unsafeValue
        val p2 = persons.ensureTelegram(t2, s"telegram $t2").unsafeValue
        val p3 = persons.ensureTelegram(t3, s"telegram $t3").unsafeValue

        p1.id should not be p2.id
        p1.id should not be p3.id
        p2.id should not be p3.id

        persons.getByIds(Set(p1.id, p2.id, p3.id)).unsafeValue shouldBe Map[Int, Person](p1.id → p1, p2.id → p2, p3.id → p3)
      }
    }

}
