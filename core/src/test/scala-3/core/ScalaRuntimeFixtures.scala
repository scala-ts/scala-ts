package io.github.scalats.core

import java.net.URI

import scala.collection.immutable.ListSet

import dotty.tools.dotc.core.{ Flags, Symbols, Types }
import dotty.tools.dotc.core.Contexts.Context

import dotty.tools.dotc.ast.{ tpd, Trees }
import dotty.tools.dotc.ast.tpd.Tree

import io.github.scalats.scala as ScalaModel

import dotty.tools.dotc.interactive.InteractiveDriver
import dotty.tools.dotc.reporting.Diagnostic
import dotty.tools.dotc.util.SourceFile

object ScalaRuntimeFixtures {
  val compilerMaxRetries = 3

  lazy val results = new ScalaParserResults(
    ns = List.empty,
    valueClassNs = List.empty,
    nonEmptySelectedListInvariant = true
  )

  def objectClass(nme: String): String =
    nme + '$'

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
        ScalaModel.ListRef(colorRef, false),
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

  private val classpath: String = {
    import java.io.File.pathSeparator

    val fromLoader = getClass.getClassLoader match {
      case cls: java.net.URLClassLoader =>
        cls.getURLs.toSeq.collect {
          case url if url.getProtocol == "file" =>
            url.toString.stripPrefix("file:")
        }.mkString(pathSeparator)

      case _ =>
        ""
    }

    if (fromLoader.nonEmpty) fromLoader
    else sys.props.getOrElse("java.class.path", "")
  }

  private val driverSettings: List[String] =
    List(
      "-d",
      sys.props("java.io.tmpdir"),
      "-classpath",
      classpath
    )

  // Prefer an explicit classpath only (no -usejavacp) to avoid double-loading
  // stdlib symbols ("already has a symbol") under sbt's test classloader.
  private val driver = new InteractiveDriver(driverSettings)

  // Frozen after the fixture unit is compiled. Do not run further compiles on
  // `driver` or denotations from the fixture trees become invalid.
  private var fixtureCtx: Context = driver.currentCtx

  implicit def defaultCtx: Context = fixtureCtx

  private def compileOn(
      drv: InteractiveDriver,
      name: String,
      source: String
    ): (Context, Tree) = {
    // URI path cannot contain '<'/'>'; keep virtual source name as-is for compiled set
    val uri = new URI(
      "memory:///" + name.replace("<", "").replace(">", "")
    )
    val src = SourceFile.virtual(name, source)
    val diagnostics = drv.run(uri, src)
    val ctx = drv.currentCtx

    val errors = diagnostics.collect { case e: Diagnostic.Error => e }
    if (errors.nonEmpty) {
      throw new Exception(errors.map(_.message).mkString("; "))
    }

    drv.compilationUnits.get(uri) match {
      case Some(unit) =>
        ctx -> unit.tpdTree.asInstanceOf[Tree]

      case None =>
        throw new Exception(s"No compilation unit for $name")
    }
  }

  private def topLevelDefs(
      tree: Tree
    )(using
      Context
    ): List[tpd.TypeDef] = {
    def go(t: Tree): List[tpd.TypeDef] = t match {
      case pkg: tpd.PackageDef =>
        pkg.stats.flatMap(go)

      case td: tpd.TypeDef =>
        td :: Nil

      case _ =>
        Nil
    }

    go(tree)
  }

  private def simpleName(
      td: tpd.TypeDef
    )(using
      Context
    ): String =
    td.name.toString.stripSuffix("$")

  private def isModuleClass(
      td: tpd.TypeDef
    )(using
      Context
    ): Boolean =
    td.symbol.is(Flags.ModuleClass) || td.symbol.is(Flags.Module)

  private def findClass(
      defs: List[tpd.TypeDef],
      name: String
    )(using
      Context
    ): Tree =
    defs
      .find(td => !isModuleClass(td) && simpleName(td) == name)
      .getOrElse(
        throw new Exception(
          s"Class $name not found among ${defs.map(simpleName).mkString(", ")}"
        )
      )

  private def findModule(
      defs: List[tpd.TypeDef],
      name: String
    )(using
      Context
    ): Tree =
    defs
      .find(td => isModuleClass(td) && simpleName(td) == name)
      .getOrElse(
        throw new Exception(
          s"Module $name not found among ${defs
              .map(d => s"${simpleName(d)}${if (isModuleClass(d)) "$" else ""}")
              .mkString(", ")}"
        )
      )

  private def nestedTypeDefs(
      module: Tree
    )(using
      Context
    ): List[tpd.TypeDef] =
    module match {
      case tpd.TypeDef(_, tpl: tpd.Template) =>
        tpl.body.collect { case td: tpd.TypeDef => td }

      case _ =>
        Nil
    }

  // Separate driver so ad-hoc typecheck does not invalidate fixture denotations.
  val typecheck = { (input: String) =>
    val (ctx, tree) =
      compileOn(new InteractiveDriver(driverSettings), "<typecheck>", input)
    topLevelDefs(tree)(using ctx) match {
      case head :: _ =>
        head

      case Nil =>
        throw new Exception(s"Invalid definition: $tree")
    }
  }

  def fullName(sym: Symbols.Symbol): String =
    sym.fullName.toString

  lazy val EmptyTree = new Trees.EmptyTree

  // ---

  private val fixtureSource =
    """
case class TestClass1(name: String)

object TestClass1 {}

case class TestClass1B(foo: String)

case class TestClass2[T](name: T)

case class TestClass3[T](name: ::[T])

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

case class AnyValChild(value: String) extends AnyVal

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

sealed abstract class State(val entryName: String)

object Alabama extends State("AL")

object Alaska extends State("AK")
"""

  private val (
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
    familyUnionTree,
    loremTree,
    ipsumTree,
    colorTree,
    styleTree,
    refinementTree,
    stateUnionTree,
    alabamaTree,
    alaskaTree
  ) = {
    val (ctx, unitTree) = compileOn(driver, "<typecheck>", fixtureSource)
    fixtureCtx = ctx
    given Context = fixtureCtx
    val defs = topLevelDefs(unitTree)

    val aliasesBody = nestedTypeDefs(findModule(defs, "Aliases"))
    val logOpaque = aliasesBody
      .find(td => simpleName(td) == "Log")
      .getOrElse(throw new Exception("Aliases.Log not found"))
    val familyUnion = aliasesBody
      .find(td => simpleName(td) == "FamilyUnion")
      .getOrElse(throw new Exception("Aliases.FamilyUnion not found"))

    // Top-level type aliases are nested under the synthetic $package module
    val packageBody = nestedTypeDefs(findModule(defs, "$package"))
    val refinement = packageBody
      .find(td => simpleName(td) == "RefinementFoo")
      .getOrElse(
        throw new Exception(
          s"RefinementFoo not found among ${packageBody.map(simpleName).mkString(", ")}"
        )
      )

    // Enum parsing walks the companion module (linkedClass is the enum)
    val color = findModule(defs, "Color")

    (
      findClass(defs, "TestClass1"),
      findModule(defs, "TestClass1"),
      findClass(defs, "TestClass1B"),
      findClass(defs, "TestClass2"),
      findClass(defs, "TestClass3"),
      findClass(defs, "TestClass4"),
      findClass(defs, "TestClass5"),
      findClass(defs, "TestClass6"),
      findClass(defs, "TestClass7"),
      findClass(defs, "AnyValChild"),
      findClass(defs, "TestClass8"),
      findModule(defs, "TestEnumeration"),
      findClass(defs, "TestClass9"),
      findClass(defs, "TestClass10"),
      findModule(defs, "TestObject1"),
      findModule(defs, "TestObject2"),
      findClass(defs, "Family"),
      findClass(defs, "FamilyMember1"),
      findModule(defs, "FamilyMember2"),
      findModule(defs, "FamilyMember3"),
      logOpaque,
      familyUnion,
      findClass(defs, "Lorem"),
      findModule(defs, "Ipsum"),
      color,
      findClass(defs, "Style"),
      refinement,
      findClass(defs, "State"),
      findModule(defs, "Alabama"),
      findModule(defs, "Alaska")
    )
  }

  // Constructed after fixture compile so it captures the frozen fixtureCtx.
  private[core] val scalaParser = new ScalaParser(
    compiled = Set("<typecheck>"),
    logger = Logger(org.slf4j.LoggerFactory getLogger "ScalaParserSpec")
  )

  def parseTypes(
      types: List[(Types.Type, Tree)],
      symtab: Map[String, ListSet[(Types.Type, Tree)]] = Map.empty
    ): List[(String, ListSet[ScalaModel.TypeDef])] =
    scalaParser
      .parseTypes(
        types,
        symtab,
        ListSet.empty,
        _ => true
      )
      .parsed
      .toList

  def parseType(
      tpe: (Types.Type, Tree),
      symtab: ScalaParser.StringMap[(Types.Type, Tree)],
      examined: ListSet[ScalaParser.TypeFullId],
      acceptsType: Symbols.Symbol => Boolean
    ): ScalaParser.Result[ScalaParser.StringMap, ScalaParser.TypeFullId] =
    scalaParser.parseType(tpe, symtab, examined, acceptsType)

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

  lazy val AnyValChildType = AnyValChildTree.tpe

  val TestClass8Tree: Tree = testClass8Tree

  lazy val TestClass8Type = TestClass8Tree.tpe

  val TestEnumerationTree: Tree = testEnumerationTree

  lazy val TestEnumerationType = TestEnumerationTree.tpe

  val TestClass9Tree: Tree = testClass9Tree

  val TestClass9Type = TestClass9Tree.tpe

  val TestClass10Tree: Tree = testClass10Tree

  lazy val TestClass10Type = TestClass10Tree.tpe

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

  val StateTree: Tree = stateUnionTree

  lazy val StateType = StateTree.tpe

  val AlabamaTree: Tree = alabamaTree

  lazy val AlabamaType = AlabamaTree.tpe

  val AlaskaTree: Tree = alaskaTree

  val AlaskaType = AlaskaTree.tpe

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
