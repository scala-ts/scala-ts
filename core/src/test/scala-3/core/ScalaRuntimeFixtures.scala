package io.github.scalats.core

import scala.collection.immutable.ListSet

import scala.util.control.NonFatal

import dotty.tools.dotc.core.{ Contexts, Symbols, Types }

import dotty.tools.dotc.ast.Trees
import dotty.tools.dotc.ast.tpd.Tree

import dotty.tools.repl.{ ReplDriver, ReplCompiler, State }

import io.github.scalats.{ scala => ScalaModel }

// Value class workaround (see bellow)
case class AnyValChild(value: String) extends AnyVal
case class TestClass8(name: AnyValChild, aliases: Seq[AnyValChild])

object ScalaRuntimeFixtures {

  lazy val results = new ScalaParserResults(
    ns = List(f"$$wrapper", "expr"),
    valueClassNs = List.empty
  )

  private val initialState: State = {
    import java.io.File.pathSeparator

    val classpath = getClass.getClassLoader match {
      case cls: java.net.URLClassLoader =>
        cls.getURLs.toSeq.collect {
          case url if url.getProtocol == "file" =>
            url.toString.stripPrefix("file:")
        }.mkString(pathSeparator)

      case _ =>
        ""
    }

    val replDriver = new ReplDriver(
      Array(
        "-d",
        sys.props("java.io.tmpdir"),
        "-classpath",
        classpath,
        "-Ydebug"
      )
    )

    replDriver.initialState
  }

  private lazy val replCompiler = new ReplCompiler

  private val state: State = {
    def newRun(state: State): State = {
      val run = replCompiler.newRun(state.context.fresh, state)
      state.copy(context = run.runContext)
    }

    newRun(initialState)
  }

  val typecheck = { (input: String) =>
    replCompiler.typeCheck(input)(using state) match {
      case Right(valDef) =>
        valDef.unforced match {
          case Trees.Block(d :: _, _) =>
            d.asInstanceOf[Tree]

          case invalid =>
            throw new Exception(s"Invalid definition: $invalid")
        }

      case Left(errors) =>
        throw new Exception(errors mkString "; ")
    }
  }

  implicit def defaultCtx: Contexts.Context = state.context

  private val scalaParser = new ScalaParser(
    compiled = Set("<typecheck>" /*, "core/src/test/scala-3/core/ScalaRuntimeFixtures.scala" */ ),
    logger = Logger(org.slf4j.LoggerFactory getLogger "ScalaParserSpec")
  )

  @annotation.tailrec
  def parseTypes(
      types: List[(Types.Type, Tree)],
      symtab: Map[String, (Types.Type, Tree)] = Map.empty,
      retries: Int = 3
    ): List[ScalaModel.TypeDef] =
    try {
      scalaParser
        .parseTypes(
          types,
          symtab,
          ListSet.empty,
          _ => true
        )
        .parsed
        .toList
    } catch {
      case _: dotty.tools.dotc.core.CyclicReference if retries > 0 =>
        Thread.sleep(200)
        parseTypes(types, symtab, retries - 1)
    }

  def fullName(sym: Symbols.Symbol): String =
    sym.fullName.toString

  lazy val EmptyTree = new Trees.EmptyTree

  // ---

  // TODO: Remove; case class TestClass1(name: String)

  val (
    testClass1Tree,
    testClass1CompanionTree,
    testClass1BTree,
    testClass2Tree,
    testClass3Tree,
    testClass4Tree,
    testClass5Tree,
    testClass6Tree,
    testClass7Tree,
    anyValChildTree,
    testClass8Tree,
    testEnumerationTree,
    testClass9Tree,
    testClass10Tree,
    testObject1Tree,
    testObject2Tree,
    familyTree,
    familyMember1Tree,
    familyMember2Tree,
    familyMember3Tree
  ) = replCompiler.typeCheck("""
case class TestClass1(name: String)

object TestClass1 {}

case class TestClass1B(foo: String)

case class TestClass2[T](name: T)

case class TestClass3[T](name: List[T])

case class TestClass4[T](name: TestClass3[T])

case class TestClass5[T](
    name: Option[T],
    counters: Map[String, java.math.BigInteger])

case class TestClass6[T](
    name: Option[TestClass5[List[Option[TestClass4[String]]]]],
    age: TestClass3[TestClass2[TestClass1]])

case class TestClass7[T](
    name: Either[TestClass1, TestClass1B])

case class AnyValChild(value: String) // Cannot (workaround): extends AnyVal

case class TestClass8(
    name: AnyValChild, aliases: Seq[AnyValChild])

object TestEnumeration extends scala.Enumeration {
  val A, B, C = Value
}

case class TestClass9(name: TestEnumeration.Value)

case class TestClass10(
    name: String,
    tuple: Tuple1[Int],
    tupleA: (String, Int),
    tupleB: Tuple2[String, Long],
    tupleC: Tuple3[String, String, Long])

case object TestObject1

object TestObject2 extends Foo("Foo") {
  def code = 1
  val const = new String("value")
  def foo = name

  val list = Seq("first", name)
  def set: Set[Int] = Set(code, 2)
  val mapping = Map("foo" -> "bar", (new String("lorem")) -> name)

  def dictOfList = Map(
    new String("excludes") -> Seq("*.txt", ".gitignore"),
    "includes" -> Seq("images/**", "*.jpg", "*.png"))

  val concatSeq = list ++ Seq("foo", "bar") ++ Seq("lorem")
  def concatList = List("foo") ++ list

  val mergedSet = set ++ Set(3)
}

class Foo(val name: String)

sealed trait Family {
  def foo: String
  val bar = "lorem"
  def ipsum = 0.1D
}

case class FamilyMember1(foo: String) extends Family {
  val code = 1
}

object FamilyMember2 extends Family {
  val foo = "bar"
}

object FamilyMember3 extends Family {
  def foo = "lorem"
}
""")(using state) match {
    case Right(valDef) =>
      valDef.unforced match {
        case Trees.Block(
              testClass1Tree :: _ :: testClass1CompanionTree :: testClass1BTree :: _ :: _ :: testClass2Tree :: _ :: _ :: testClass3Tree :: _ :: _ :: testClass4Tree :: _ :: _ :: testClass5Tree :: _ :: _ :: testClass6Tree :: _ :: _ :: testClass7Tree :: _ :: _ :: anyValChildTree :: _ :: _ :: testClass8Tree :: _ :: _ :: testEnumerationTree :: _ :: testClass9Tree :: _ :: _ :: testClass10Tree :: _ :: _ :: _ :: testObject1Tree :: _ :: testObject2Tree :: _ /*Foo*/ :: familyTree :: familyMember1Tree :: _ :: _ :: _ :: familyMember2Tree :: _ :: familyMember3Tree :: _,
              _
            ) =>
          (
            testClass1Tree.asInstanceOf[Tree],
            testClass1CompanionTree.asInstanceOf[Tree],
            testClass1BTree.asInstanceOf[Tree],
            testClass2Tree.asInstanceOf[Tree],
            testClass3Tree.asInstanceOf[Tree],
            testClass4Tree.asInstanceOf[Tree],
            testClass5Tree.asInstanceOf[Tree],
            testClass6Tree.asInstanceOf[Tree],
            testClass7Tree.asInstanceOf[Tree],
            anyValChildTree.asInstanceOf[Tree],
            testClass8Tree.asInstanceOf[Tree],
            testEnumerationTree.asInstanceOf[Tree],
            testClass9Tree.asInstanceOf[Tree],
            testClass10Tree.asInstanceOf[Tree],
            testObject1Tree.asInstanceOf[Tree],
            testObject2Tree.asInstanceOf[Tree],
            familyTree.asInstanceOf[Tree],
            familyMember1Tree.asInstanceOf[Tree],
            familyMember2Tree.asInstanceOf[Tree],
            familyMember3Tree.asInstanceOf[Tree]
          )

        case invalid =>
          throw new Exception(s"Invalid definition: $invalid")
      }

    case Left(errors) =>
      throw new Exception(errors mkString "; ")
  }

  val TestClass1Tree: Tree = testClass1Tree

  lazy val TestClass1Type: Types.Type = TestClass1Tree.tpe

  val TestClass1CompanionTree: Tree = testClass1CompanionTree

  lazy val TestClass1CompanionType = TestClass1CompanionTree.tpe

  // case class TestClass1B(foo: String)

  val TestClass1BTree: Tree = testClass1BTree

  lazy val TestClass1BType = TestClass1BTree.tpe

  // case class TestClass2[T](name: T)

  val TestClass2Tree: Tree = testClass2Tree

  lazy val TestClass2Type = TestClass2Tree.tpe

  // case class TestClass3[T](name: List[T])

  val TestClass3Tree: Tree = testClass3Tree

  lazy val TestClass3Type = TestClass3Tree.tpe

  // case class TestClass4[T](name: TestClass3[T])

  val TestClass4Tree: Tree = testClass4Tree

  lazy val TestClass4Type = TestClass4Tree.tpe

  /*
  case class TestClass5[T](
      name: Option[T],
      counters: Map[String, java.math.BigInteger])
   */

  val TestClass5Tree: Tree = testClass5Tree

  lazy val TestClass5Type = TestClass5Tree.tpe

  /*
  case class TestClass6[T](
      name: Option[TestClass5[List[Option[TestClass4[String]]]]],
      age: TestClass3[TestClass2[TestClass1]])
   */

  val TestClass6Tree: Tree = testClass6Tree

  lazy val TestClass6Type = TestClass6Tree.tpe

  // case class TestClass7[T](name: Either[TestClass1, TestClass1B])

  val TestClass7Tree: Tree = testClass7Tree

  lazy val TestClass7Type = TestClass7Tree.tpe

  // case class AnyValChild(value: String) extends AnyVal

  val AnyValChildTree: Tree = anyValChildTree

  // lazy val AnyValChildType = AnyValChildTree.tpe
  // !! Workaround as cannot typeCheck Value classes
  val AnyValChildType: Types.Type =
    Symbols.requiredClassRef(classOf[AnyValChild].getName)

  /*
  case class TestClass8(
      name: AnyValChild,
      aliases: Seq[AnyValChild])
   */

  val TestClass8Tree: Tree = testClass8Tree

  // lazy val TestClass8Type = TestClass8Tree.tpe
  // !! Workaround as cannot typeCheck Value classes
  val TestClass8Type: Types.Type =
    Symbols.requiredClassRef(classOf[TestClass8].getName)

  /*
  object TestEnumeration extends scala.Enumeration {
    val A, B, C = Value
  }
   */

  val TestEnumerationTree: Tree = testEnumerationTree

  lazy val TestEnumerationType = TestEnumerationTree.tpe

  // case class TestClass9(name: TestEnumeration.Value)

  val TestClass9Tree: Tree = testClass9Tree

  val TestClass9Type = TestClass9Tree.tpe

  /*
  case class TestClass10(
      name: String,
      tuple: Tuple1[Int],
      tupleA: (String, Int),
      tupleB: Tuple2[String, Long],
      tupleC: Tuple3[String, String, Long])
   */

  val TestClass10Tree: Tree = testClass10Tree

  lazy val TestClass10Type = TestClass10Tree.tpe

  // case object TestObject1

  val TestObject1Tree: Tree = testObject1Tree

  lazy val TestObject1Type = TestObject1Tree.tpe

  /*
  object TestObject2 {
    val name = "Foo"
    def code = 1
  }
   */

  val TestObject2Tree: Tree = testObject2Tree

  val TestObject2Type = TestObject2Tree.tpe

  /*
  sealed trait Family {
    def foo: String
    val bar = "lorem"
    def ipsum = 0.1D
  }
   */

  val FamilyTree: Tree = familyTree

  lazy val FamilyType = FamilyTree.tpe

  /*
  case class FamilyMember1(foo: String) extends Family {
    val code = 1
  }
   */

  val FamilyMember1Tree: Tree = familyMember1Tree

  lazy val FamilyMember1Type = FamilyMember1Tree.tpe

  /*
  case object FamilyMember2 extends Family {
    // Members are unsupported for object,
    // and so the TS singleton won't implements the common interface
    val foo = "bar"
  }
   */

  val FamilyMember2Tree: Tree = familyMember2Tree

  lazy val FamilyMember2Type = FamilyMember2Tree.tpe

  /*
  object FamilyMember3 extends Family {
    def foo = "lorem"
  }
   */

  val FamilyMember3Tree: Tree = familyMember3Tree

  val FamilyMember3Type = FamilyMember3Tree.tpe
}
