package io.github.scalats.core

import scala.collection.immutable.ListSet

import dotty.tools.dotc.core.{ Contexts, Symbols, Types }

import dotty.tools.dotc.ast.Trees
import dotty.tools.dotc.ast.tpd.Tree

import io.github.scalats.scala as ScalaModel

import dotty.tools.repl.{ ReplCompiler, ReplDriver, State }

// Value class workaround (see bellow)
case class AnyValChild(value: String) extends AnyVal
case class TestClass8(name: AnyValChild, aliases: Seq[AnyValChild])

object ScalaRuntimeFixtures {

  lazy val results = new ScalaParserResults(
    ns = List(f"$$wrapper", "expr"),
    valueClassNs = List.empty
  )

  def objectClass(nme: String): String =
    f"$$wrapper._$$" + nme + '$'

  val logOpaqueAlias = ScalaModel.ValueClass(
    ScalaModel.QualifiedIdentifier("Log", results.ns :+ "Aliases"),
    ScalaModel.TypeMember("", ScalaModel.DoubleRef)
  )

  val unionType1 = ScalaModel.SealedUnion(
    ScalaModel.QualifiedIdentifier(
      "FamilyUnion",
      results.ns :+ "Aliases"
    ),
    ListSet.empty,
    ListSet(
      ScalaModel.CaseClass(
        ScalaModel.QualifiedIdentifier("FamilyMember1", results.ns),
        ListSet(ScalaModel.TypeMember("foo", ScalaModel.StringRef)),
        ListSet(ScalaModel.LiteralInvariant("code", ScalaModel.IntRef, "1")),
        List.empty
      ),
      ScalaModel.CaseObject(
        ScalaModel.QualifiedIdentifier("FamilyMember2", results.ns),
        ListSet(
          ScalaModel.LiteralInvariant("foo", ScalaModel.StringRef, "\"bar\"")
        )
      ),
      ScalaModel.CaseObject(
        ScalaModel.QualifiedIdentifier("FamilyMember3", results.ns),
        ListSet(
          ScalaModel.LiteralInvariant("foo", ScalaModel.StringRef, "\"lorem\"")
        )
      )
    )
  )

  val lorem = ScalaModel.CaseClass(
    ScalaModel.QualifiedIdentifier("Lorem", results.ns),
    ListSet(
      ScalaModel.TypeMember("name", ScalaModel.StringRef),
      ScalaModel.TypeMember(
        "ipsum",
        ScalaModel.UnionRef(
          ListSet(
            ScalaModel.StringRef,
            ScalaModel.UnknownTypeRef(
              ScalaModel.QualifiedIdentifier("Family", results.ns)
            )
          )
        )
      ),
      ScalaModel.TypeMember(
        "dolor",
        ScalaModel.UnionRef(ListSet(ScalaModel.IntRef, ScalaModel.DoubleRef))
      )
    ),
    ListSet.empty,
    List.empty
  )

  val ipsum = ScalaModel.CaseObject(
    ScalaModel.QualifiedIdentifier("Ipsum", results.ns),
    ListSet(
      ScalaModel.LiteralInvariant(
        "const",
        ScalaModel.UnionRef(ListSet(ScalaModel.StringRef, ScalaModel.IntRef)),
        "\"strVal\""
      ),
      ScalaModel.LiteralInvariant(
        "defaultScore",
        ScalaModel.UnionRef(ListSet(ScalaModel.IntRef, ScalaModel.DoubleRef)),
        "2"
      )
    )
  )

  private val colorId = ScalaModel.QualifiedIdentifier("Color", results.ns)
  private val colorRef = ScalaModel.EnumerationRef(colorId)

  val color = ScalaModel.EnumerationDef(
    identifier = colorId,
    possibilities = ListSet("Red", "Green", "Blue"),
    values = ListSet(
      ScalaModel.ListInvariant(
        "purple",
        ScalaModel.ListRef(colorRef),
        colorRef,
        List(
          ScalaModel.SelectInvariant(
            "purple[0]",
            colorRef,
            ScalaModel.CaseObjectRef(colorId),
            "Red"
          ),
          ScalaModel.SelectInvariant(
            "purple[1]",
            colorRef,
            ScalaModel.CaseObjectRef(colorId),
            "Blue"
          )
        )
      )
    )
  )

  val style = ScalaModel.CaseClass(
    ScalaModel.QualifiedIdentifier("Style", results.ns),
    ListSet(
      ScalaModel.TypeMember("name", ScalaModel.StringRef),
      ScalaModel.TypeMember(
        "color",
        ScalaModel.EnumerationRef(
          ScalaModel.QualifiedIdentifier("Color", results.ns)
        )
      )
    ),
    ListSet.empty,
    List.empty
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
      case Right((_, valDef)) =>
        valDef.unforcedRhs match {
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

  private[core] val scalaParser = new ScalaParser(
    compiled = Set("<typecheck>" /*, "core/src/test/scala-3/core/ScalaRuntimeFixtures.scala" */ ),
    logger = Logger(org.slf4j.LoggerFactory getLogger "ScalaParserSpec")
  )

  @annotation.tailrec
  def parseTypes(
      types: List[(Types.Type, Tree)],
      symtab: Map[String, ListSet[(Types.Type, Tree)]] = Map.empty,
      retries: Int = 3
    ): List[(String, ListSet[ScalaModel.TypeDef])] =
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

  @inline def parseType(
      tpe: (Types.Type, Tree),
      symtab: ScalaParser.StringMap[(Types.Type, Tree)],
      examined: ListSet[ScalaParser.TypeFullId],
      acceptsType: Symbols.Symbol => Boolean,
      retries: Int = 3
    ): ScalaParser.Result[ScalaParser.StringMap, ScalaParser.TypeFullId] =
    try {
      scalaParser.parseType(tpe, symtab, examined, acceptsType)
    } catch {
      case _: dotty.tools.dotc.core.CyclicReference if retries > 0 =>
        Thread.sleep(200)
        parseType(tpe, symtab, examined, acceptsType, retries - 1)
    }

  def fullName(sym: Symbols.Symbol): String =
    sym.fullName.toString

  lazy val EmptyTree = new Trees.EmptyTree

  // ---

  val Tuple2(
    Tuple22(
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
      familyMember3Tree,
      logOpaqueAliasTree,
      familyUnionTree
    ),
    Tuple5(loremTree, ipsumTree, colorTree, styleTree, refinementTree)
  ) = replCompiler.typeCheck("""
case class TestClass1(name: String)

object TestClass1 {}

case class TestClass1B(foo: String)

case class TestClass2[T](name: T)

case class TestClass3[T](name: List[T])

case class TestClass4[T](name: TestClass3[T])

case class TestClass5[T](
    name: Option[T],
    counters: Map[String, java.math.BigInteger],
    time: java.time.LocalTime)

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

object TestObject2 extends Foo("Foo \"bar\"") {
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

  val tuple1 = Tuple3("foo", 2, 3D)
  def tuple2 = "bar" -> 2
  def tuple3 = tuple1
  val tuple4 = ("lorem", 10, 20)

  object Nested1
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

object Aliases {
  opaque type Log = Double
  type FamilyUnion = FamilyMember1 | FamilyMember2.type | FamilyMember3.type
  type Score = Int | Double
}

case class Lorem(
  name: String,
  ipsum: String | Family,
  dolor: Aliases.Score)

object Ipsum {
  val const: String | Int = "strVal"
  val defaultScore: Aliases.Score = 2
}

enum Color {
  case Red, Green, Blue
}

object Color {
  val purple = Seq(Color.Red, Color.Blue)
}

case class Style(name: String, color: Color)

type RefinementFoo = Product with Serializable with Family
""")(using state) match {
    case Right((_, valDef)) =>
      valDef.unforcedRhs match {
        case Trees.Block(
              testClass1Tree :: _ :: testClass1CompanionTree :: testClass1BTree :: _ :: _ :: testClass2Tree :: _ :: _ :: testClass3Tree :: _ :: _ :: testClass4Tree :: _ :: _ :: testClass5Tree :: _ :: _ :: testClass6Tree :: _ :: _ :: testClass7Tree :: _ :: _ :: anyValChildTree :: _ :: _ :: testClass8Tree :: _ :: _ :: testEnumerationTree :: _ :: testClass9Tree :: _ :: _ :: testClass10Tree :: _ :: _ :: _ :: testObject1Tree :: _ :: testObject2Tree :: _ /*Foo*/ :: familyTree :: familyMember1Tree :: _ :: _ :: _ :: familyMember2Tree :: _ :: familyMember3Tree :: _ :: Trees
                .TypeDef(
                  _,
                  Trees.Template(
                    _,
                    _,
                    _,
                    (logOpaqueAliasTree @ Trees
                      .TypeDef(_, _)) :: familyUnionTree :: _
                  )
                ) :: loremTree :: _ :: _ :: _ :: ipsumTree :: _ :: _ :: colorTree :: styleTree :: _ :: _ :: refinementTree :: Nil,
              _
            ) =>
          Tuple2(
            Tuple22(
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
              familyMember3Tree.asInstanceOf[Tree],
              logOpaqueAliasTree.asInstanceOf[Tree],
              familyUnionTree.asInstanceOf[Tree]
            ),
            Tuple5(
              loremTree.asInstanceOf[Tree],
              ipsumTree.asInstanceOf[Tree],
              colorTree.asInstanceOf[Tree],
              styleTree.asInstanceOf[Tree],
              refinementTree.asInstanceOf[Tree]
            )
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

  val TestClass1BTree: Tree = testClass1BTree

  lazy val TestClass1BType = TestClass1BTree.tpe

  val TestClass2Tree: Tree = testClass2Tree

  lazy val TestClass2Type = TestClass2Tree.tpe

  val TestClass3Tree: Tree = testClass3Tree

  lazy val TestClass3Type = TestClass3Tree.tpe

  val TestClass4Tree: Tree = testClass4Tree

  lazy val TestClass4Type = TestClass4Tree.tpe

  val TestClass5Tree: Tree = testClass5Tree

  lazy val TestClass5Type = TestClass5Tree.tpe

  val TestClass6Tree: Tree = testClass6Tree

  lazy val TestClass6Type = TestClass6Tree.tpe

  val TestClass7Tree: Tree = testClass7Tree

  lazy val TestClass7Type = TestClass7Tree.tpe

  val AnyValChildTree: Tree = anyValChildTree

  // lazy val AnyValChildType = AnyValChildTree.tpe
  // !! Workaround as cannot typeCheck Value classes
  val AnyValChildType: Types.Type =
    Symbols.requiredClassRef(classOf[AnyValChild].getName)

  val TestClass8Tree: Tree = testClass8Tree

  // lazy val TestClass8Type = TestClass8Tree.tpe
  // !! Workaround as cannot typeCheck Value classes
  val TestClass8Type: Types.Type =
    Symbols.requiredClassRef(classOf[TestClass8].getName)

  val TestEnumerationTree: Tree = testEnumerationTree

  lazy val TestEnumerationType = TestEnumerationTree.tpe

  val TestClass9Tree: Tree = testClass9Tree

  val TestClass9Type = TestClass9Tree.tpe

  val TestClass10Tree: Tree = testClass10Tree

  lazy val TestClass10Type = TestClass10Tree.tpe

  // case object TestObject1

  val TestObject1Tree: Tree = testObject1Tree

  lazy val TestObject1Type = TestObject1Tree.tpe

  val TestObject2Tree: Tree = testObject2Tree

  val TestObject2Type = TestObject2Tree.tpe

  val FamilyTree: Tree = familyTree

  lazy val FamilyType = FamilyTree.tpe

  val FamilyMember1Tree: Tree = familyMember1Tree

  lazy val FamilyMember1Type = FamilyMember1Tree.tpe

  val FamilyMember2Tree: Tree = familyMember2Tree

  lazy val FamilyMember2Type = FamilyMember2Tree.tpe

  val FamilyMember3Tree: Tree = familyMember3Tree

  val FamilyMember3Type = FamilyMember3Tree.tpe

  val LogOpaqueAliasTree: Tree = logOpaqueAliasTree

  lazy val LogOpaqueAliasType = LogOpaqueAliasTree.tpe

  val FamilyUnionTree: Tree = familyUnionTree

  lazy val FamilyUnionType = familyUnionTree.tpe

  val LoremTree: Tree = loremTree

  lazy val LoremType = loremTree.tpe

  val IpsumTree: Tree = ipsumTree

  lazy val IpsumType = ipsumTree.tpe

  val ColorTree: Tree = colorTree

  lazy val ColorType = colorTree.tpe

  val StyleTree: Tree = styleTree

  lazy val StyleType = styleTree.tpe

  val RefinementTree: Tree = refinementTree

  lazy val RefinementType = refinementTree.tpe
}
