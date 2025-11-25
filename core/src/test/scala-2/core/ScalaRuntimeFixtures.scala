package io.github.scalats.core

import scala.util.control.NonFatal

import scala.reflect.runtime.{ universe => runtimeUniverse }

import io.github.scalats.core.Internals.ListSet
import io.github.scalats.scala._

object ScalaRuntimeFixtures {
  import runtimeUniverse.{ Type, Tree }

  lazy val results = new ScalaParserResults(
    ns = List("ScalaRuntimeFixtures"),
    valueClassNs = List("ScalaRuntimeFixtures"),
    nonEmptySelectedListInvariant = false
  )

  def objectClass(nme: String): String =
    s"${ScalaRuntimeFixtures.getClass.getName stripSuffix "$"}.$nme"

  private implicit def cl: ClassLoader = getClass.getClassLoader

  private[core] val scalaParser = new ScalaParser[runtimeUniverse.type](
    universe = runtimeUniverse,
    compiled = Set.empty,
    logger = Logger(org.slf4j.LoggerFactory getLogger "ScalaParserSpec")
  )

  def parseTypes(
      types: List[(Type, Tree)],
      symtab: Map[String, ListSet[(Type, Tree)]] = Map.empty
    ): List[(String, ListSet[TypeDef])] = scalaParser
    .parseTypes(
      types,
      symtab,
      ListSet.empty,
      _ => true
    )
    .parsed
    .toList

  @inline def parseType(
      tpe: (Type, Tree),
      symtab: ScalaParser.StringMap[(Type, Tree)],
      examined: ListSet[ScalaParser.TypeFullId],
      acceptsType: scalaParser.universe.Symbol => Boolean
    ) = scalaParser.parseType(tpe, symtab, examined, acceptsType)

  def fullName(sym: runtimeUniverse.Symbol): String = sym.fullName

  // ---

  implicit val mirror: reflect.runtime.universe.JavaMirror =
    runtimeUniverse.runtimeMirror(getClass.getClassLoader)

  import runtimeUniverse._

  private lazy val tb = {
    import scala.tools.reflect._

    runtimeUniverse.rootMirror.mkToolBox()
  }

  @inline private def retry[T](n: Int)(f: => T): T =
    try {
      f
    } catch {
      case NonFatal(_) if (n > 0) =>
        retry(n - 1)(f)

      case NonFatal(cause) =>
        throw cause
    }

  @inline private def typecheck(tree: Tree) =
    retry(5)(tb.typecheck(tree))

  @inline def EmptyTree = runtimeUniverse.EmptyTree

  // ---

  case class TestClass1(name: String)

  val TestClass1Type = typeOf[TestClass1]

  lazy val TestClass1Tree: Tree =
    typecheck(q"case class TestClass1(name: String)")

  val TestClass1CompanionType = typeOf[TestClass1.type]

  lazy val TestClass1CompanionTree: Tree = typecheck(q"object TestClass1 {}")

  case class TestClass1B(foo: String)

  val TestClass1BType = typeOf[TestClass1B]

  lazy val TestClass1BTree: Tree =
    typecheck(q"case class TestClass1B(foo: String)")

  case class TestClass2[T](name: T)

  val TestClass2Type = typeOf[TestClass2[_]]

  lazy val TestClass2Tree: Tree = typecheck(
    q"case class TestClass2[T](name: T)"
  )

  case class TestClass3[T](name: ::[T])

  val TestClass3Type = typeOf[TestClass3[_]]

  lazy val TestClass3Tree: Tree = typecheck(
    q"case class TestClass3[T](name: ::[T])"
  )

  case class TestClass4[T](name: TestClass3[T])

  val TestClass4Type = typeOf[TestClass4[_]]

  lazy val TestClass4Tree: Tree =
    q"case class TestClass4[T](name: TestClass3[T])"

  case class TestClass5[T](
      name: Option[T],
      counters: Map[String, java.math.BigInteger],
      time: java.time.LocalTime)

  val TestClass5Type = typeOf[TestClass5[_]]

  lazy val TestClass5Tree: Tree = typecheck(q"""case class TestClass5[T](
    name: Option[T],
    counters: Map[String, java.math.BigInteger],
    time: java.time.LocalTime
  )""")

  case class TestClass6[T](
      name: Option[TestClass5[List[Option[TestClass4[String]]]]],
      age: TestClass3[TestClass2[TestClass1]])

  val TestClass6Type = typeOf[TestClass6[_]]

  lazy val TestClass6Tree: Tree = q"""case class TestClass6[T](
    name: Option[TestClass5[List[Option[TestClass4[String]]]]],
    age: TestClass3[TestClass2[TestClass1]])"""

  case class TestClass7[T](name: Either[TestClass1, TestClass1B])

  val TestClass7Type = typeOf[TestClass7[_]]

  lazy val TestClass7Tree: Tree = q"""case class TestClass7[T](
    name: Either[TestClass1, TestClass1B])"""

  case class AnyValChild(value: String) extends AnyVal

  val AnyValChildType = typeOf[AnyValChild]

  lazy val AnyValChildTree: Tree = typecheck(
    q"case class AnyValChild(value: String)"
  )

  case class TestClass8(
      name: AnyValChild,
      aliases: Seq[AnyValChild])

  val TestClass8Type = typeOf[TestClass8]

  val TestClass8Tree: Tree = q"""case class TestClass8(
    name: AnyValChild, aliases: Seq[AnyValChild])"""

  object TestEnumeration extends scala.Enumeration {
    val A, B, C = Value
  }

  val TestEnumerationType = typeOf[TestEnumeration.type]

  lazy val TestEnumerationTree: Tree = typecheck(
    q"""object TestEnumeration extends scala.Enumeration {
    val A, B, C = Value
  }"""
  )

  case class TestClass9(name: TestEnumeration.Value)

  val TestClass9Type = typeOf[TestClass9]

  lazy val TestClass9Tree: Tree =
    q"case class TestClass9(name: TestEnumeration.Value)"

  case class TestClass10(
      name: String,
      tuple: Tuple1[Int],
      tupleA: (String, Int),
      tupleB: Tuple2[String, Long],
      tupleC: Tuple3[String, String, Long])

  val TestClass10Type = typeOf[TestClass10]

  lazy val TestClass10Tree: Tree = typecheck(q"""case class TestClass10(
    name: String,
    tuple: Tuple1[Int],
    tupleA: (String, Int),
    tupleB: Tuple2[String, Long],
    tupleC: Tuple3[String, String, Long])""")

  case object TestObject1

  val TestObject1Type = typeOf[TestObject1.type]

  lazy val TestObject1Tree: Tree = typecheck(q"case object TestObject1")

  object TestObject2 {
    val name = "Foo \"bar\""
    def code = 1
    val const = new String("value")
    def foo = name

    val list = ::("first", List(name))
    def set: Set[Int] = Set(code, 2)
    val mapping = Map("foo" -> "bar", (new String("lorem")) -> name)

    def dictOfList = Map(
      new String("excludes") -> Seq("*.txt", ".gitignore"),
      "includes" -> Seq("images/**", "*.jpg", "*.png")
    )

    val concatSeq = list ++ Seq("foo", "bar") ++ Seq("lorem")
    def concatList = List("foo") ++ list

    val mergedSet = set ++ Set(3)

    val tuple1 = Tuple3("foo", 2, 3D)
    def tuple2 = "bar" -> 2
    def tuple3 = tuple1
    val tuple4 = ("lorem", 10, 20)

    object Nested1
  }

  val TestObject2Type = typeOf[TestObject2.type]

  lazy val TestObject2Tree: Tree = typecheck(q"""
    class Foo(val name: String)

    object TestObject2 extends Foo("Foo \"bar\"") {
      def code = 1
      val const = new String("value")
      def foo = name

      val list = ::("first", List(name))
      def set: Set[Int] = Set(code, 2)
      val mapping = Map("foo" -> "bar", (new String("lorem")) -> name)

      def dictOfList = Map(
        new String("excludes") -> Seq("*.txt", ".gitignore"),
        "includes" -> Seq("images/**", "*.jpg", "*.png"))

      val concatSeq = list ++ Seq("foo", "bar") ++ Seq("lorem")
      def concatList = List("foo") ++ list

      val mergedSet = set ++ Set(3)

      val tuple1 = Tuple3("foo", 2, 3D)
      def tuple2 = "bar" -> 2
      def tuple3 = tuple1
      val tuple4 = ("lorem", 10, 20)

      object Nested1
    }""").children.drop(1).head

  sealed trait Family {
    def foo: String
    val bar = "lorem"
    def ipsum = 0.1D
  }

  val FamilyType = typeOf[Family]

  lazy val FamilyTree: Tree = typecheck(q"""trait Family {
    def foo: String
    val bar = "lorem"
    def ipsum = 0.1D
  }""")

  case class FamilyMember1(foo: String) extends Family {
    val code = 1
  }

  val FamilyMember1Type = typeOf[FamilyMember1]

  lazy val FamilyMember1Tree: Tree = typecheck(
    q"""${tb untypecheck FamilyTree}; case class FamilyMember1(foo: String) extends Family {
      val code = 1
    }"""
  )

  case object FamilyMember2 extends Family {
    // Members are unsupported for object,
    // and so the TS singleton won't implements the common interface
    val foo = "bar"
  }

  val FamilyMember2Type = typeOf[FamilyMember2.type]

  lazy val FamilyMember2Tree: Tree = typecheck(
    q"""${tb untypecheck FamilyTree}; object FamilyMember2 extends Family {
      val foo = "bar"
    }"""
  )

  object FamilyMember3 extends Family {
    def foo = "lorem"
  }

  val FamilyMember3Type = typeOf[FamilyMember3.type]

  lazy val FamilyMember3Tree: Tree = typecheck(
    q"""${tb untypecheck FamilyTree}; object FamilyMember3 extends Family {
      def foo = "lorem"
    }"""
  )

  val RefinementType = typeOf[Product with Serializable with Family]

  lazy val RefinementTree: Tree = typecheck(
    q"""${tb untypecheck FamilyTree}; type RefinementFoo = Product with Serializable with Family {}"""
  )
}
