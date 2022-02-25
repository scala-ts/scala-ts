package io.github.scalats.core

import io.github.scalats.{ scala => ScalaModel }
import io.github.scalats.core.Internals.ListSet
import io.github.scalats.typescript._

import ScalaParserResults._

final class TranspilerSpec extends org.specs2.mutable.Specification {
  "Transpiler" title

  import TranspilerResults._

  "Transpiler" should {
    "transpile a case class with one primitive member" in {
      val result = defaultTranspiler(ListSet(caseClass1))

      result must have size 1 and {
        result must contain(interface1)
      }
    }

    "transpile a generic class with one member" in {
      val result = defaultTranspiler(ListSet(caseClass2))

      result must have size 1 and {
        result must contain(interface2)
      }
    }

    "transpile a generic case class with one member list of type parameter" in {
      val result = defaultTranspiler(ListSet(caseClass3))

      result must have size 1 and {
        result must contain(interface3)
      }
    }

    "transpile a generic case class with one optional member" in {
      val result = defaultTranspiler(ListSet(caseClass5))

      result must have size 1 and {
        result must contain(interface5)
      }
    }

    "transpile disjunction types" in {
      val result = defaultTranspiler(ListSet(caseClass7))

      result must have size 1 and {
        result must contain(interface7)
      }
    }

    "transpile a tagged type" >> {
      "as tagged declaration" in {
        val result = defaultTranspiler(ListSet(tagged1))

        result must have size 1 and {
          result must contain(taggedDeclaration1)
        }
      }

      "interface member" in {
        val result = defaultTranspiler(ListSet(caseClass8))

        result must have size 1 and {
          result must contain(interface8)
        }
      }
    }

    "transpile Tuple types" in {
      val result = defaultTranspiler(ListSet(caseClass10))

      result must have size 1 and {
        result must contain(interface10)
      }
    }

    "transpile enumeration" in {
      val result = defaultTranspiler(ListSet(testEnumeration))

      result must have size 1 and {
        result must contain(enum1)
      }
    }

    "transpile case object" in {
      val result = defaultTranspiler(ListSet(caseObject1))

      result must have size 1 and {
        result must contain(singleton1)
      }
    }

    "transpile object" in {
      val result = defaultTranspiler(
        ListSet(caseObject2),
        Some(
          InterfaceDeclaration(
            "SupI",
            ListSet.empty,
            List.empty[String],
            Option.empty,
            false
          )
        )
      )

      result must have size 1 and {
        result must contain(singleton2)
      }
    }

    "transpile sealed trait as union" in {
      val result = defaultTranspiler(ListSet(sealedFamily1))

      result must have size 4 and {
        result must contain(union1)
      } and {
        result must contain(unionMember2Singleton)
      } and {

        val member1Interface = InterfaceDeclaration(
          "ScalaRuntimeFixturesFamilyMember1",
          ListSet(Member("foo", StringRef)),
          List.empty,
          Some(unionIface),
          false
        )

        result must contain(member1Interface)
      } and {
        result must contain(
          SingletonDeclaration(
            "ScalaRuntimeFixturesFamilyMember3",
            ListSet(LiteralValue("foo", StringRef, "\"lorem\"")),
            Some(unionIface)
          )
        )
      }
    }
  }
}

object TranspilerResults {
  private val defaultt = new Transpiler(Settings())

  def defaultTranspiler(
      in: ListSet[ScalaModel.TypeDef]
    ): List[Declaration] =
    defaultt.apply(in).toList

  def defaultTranspiler(
      scalaTypes: ListSet[ScalaModel.TypeDef],
      superInterface: Option[InterfaceDeclaration]
    ): List[Declaration] = defaultt(scalaTypes, superInterface).toList

  val interface1 = InterfaceDeclaration(
    "ScalaRuntimeFixturesTestClass1",
    ListSet(Member("name", StringRef)),
    List.empty,
    Option.empty,
    false
  )

  val interface2 = InterfaceDeclaration(
    "ScalaRuntimeFixturesTestClass2",
    ListSet(Member("name", SimpleTypeRef("T"))),
    typeParams = List("T"),
    superInterface = Option.empty,
    union = false
  )

  val interface3 = InterfaceDeclaration(
    "ScalaRuntimeFixturesTestClass3",
    ListSet(Member("name", ArrayRef(SimpleTypeRef("T")))),
    typeParams = List("T"),
    superInterface = Option.empty,
    union = false
  )

  val interface5 = InterfaceDeclaration(
    "ScalaRuntimeFixturesTestClass5",
    ListSet(
      Member("name", NullableType(SimpleTypeRef("T"))),
      Member("counters", MapType(StringRef, NumberRef))
    ),
    typeParams = List("T"),
    superInterface = Option.empty,
    union = false
  )

  val interface7 = InterfaceDeclaration(
    "ScalaRuntimeFixturesTestClass7",
    ListSet(
      Member(
        "name",
        UnionType(
          ListSet(
            CustomTypeRef("ScalaRuntimeFixturesTestClass1", List.empty),
            CustomTypeRef("ScalaRuntimeFixturesTestClass1B", List.empty)
          )
        )
      )
    ),
    typeParams = List("T"),
    superInterface = Option.empty,
    union = false
  )

  val taggedDeclaration1 = TaggedDeclaration(
    name = "ScalaRuntimeFixturesAnyValChild",
    field = Member("value", StringRef)
  )

  val interface8 = InterfaceDeclaration(
    "ScalaRuntimeFixturesTestClass8",
    ListSet(
      Member("name", TaggedRef("ScalaRuntimeFixturesAnyValChild", StringRef)),
      Member(
        "aliases",
        ArrayRef(TaggedRef("ScalaRuntimeFixturesAnyValChild", StringRef))
      )
    ),
    typeParams = List.empty,
    superInterface = Option.empty,
    union = false
  )

  val interface10 = InterfaceDeclaration(
    "ScalaRuntimeFixturesTestClass10",
    ListSet(
      Member("name", StringRef),
      Member("tuple", TupleRef(List(NumberRef))),
      Member("tupleA", TupleRef(List(StringRef, NumberRef))),
      Member("tupleB", TupleRef(List(StringRef, NumberRef))),
      Member("tupleC", TupleRef(List(StringRef, StringRef, NumberRef)))
    ),
    typeParams = List.empty,
    superInterface = None,
    union = false
  )

  val singleton1 = SingletonDeclaration(
    name = "ScalaRuntimeFixturesTestObject1",
    values = ListSet.empty,
    superInterface = Option.empty
  )

  val singleton2 = SingletonDeclaration(
    "ScalaRuntimeFixturesTestObject2",
    ListSet(
      LiteralValue("name", StringRef, "\"Foo\""),
      LiteralValue("code", NumberRef, "1"),
      LiteralValue("const", StringRef, "\"value\""),
      SelectValue("foo", StringRef, ThisTypeRef, "name"),
      ListValue(
        name = "list",
        typeRef = ArrayRef(StringRef),
        valueTypeRef = StringRef,
        elements = List(
          LiteralValue("list[0]", StringRef, "\"first\""),
          SelectValue("list[1]", StringRef, ThisTypeRef, "name")
        )
      ),
      SetValue(
        name = "set",
        typeRef = ArrayRef(NumberRef),
        valueTypeRef = NumberRef,
        elements = Set(
          SelectValue("set[0]", NumberRef, ThisTypeRef, "code"),
          LiteralValue("set[1]", NumberRef, "2")
        )
      ),
      DictionaryValue(
        name = "mapping",
        typeRef = MapType(StringRef, StringRef),
        valueTypeRef = StringRef,
        entries = Map(
          "foo" -> LiteralValue("mapping[foo]", StringRef, "\"bar\""),
          "lorem" -> SelectValue(
            "mapping[lorem]",
            StringRef,
            ThisTypeRef,
            "name"
          )
        )
      ),
      DictionaryValue(
        name = "dictOfList",
        typeRef = MapType(StringRef, ArrayRef(StringRef)),
        valueTypeRef = ArrayRef(StringRef),
        entries = Map(
          "excludes" -> ListValue(
            name = "dictOfList[excludes]",
            typeRef = ArrayRef(StringRef),
            valueTypeRef = StringRef,
            elements = List(
              LiteralValue(
                "dictOfList[excludes][0]",
                StringRef,
                "\"*.txt\""
              ),
              LiteralValue(
                "dictOfList[excludes][1]",
                StringRef,
                "\".gitignore\""
              )
            )
          ),
          "includes" -> ListValue(
            name = "dictOfList[includes]",
            typeRef = ArrayRef(StringRef),
            valueTypeRef = StringRef,
            elements = List(
              LiteralValue(
                "dictOfList[includes][0]",
                StringRef,
                "\"images/**\""
              ),
              LiteralValue(
                "dictOfList[includes][1]",
                StringRef,
                "\"*.jpg\""
              ),
              LiteralValue(
                "dictOfList[includes][2]",
                StringRef,
                "\"*.png\""
              )
            )
          )
        )
      ),
      MergedListsValue(
        name = "concatSeq",
        valueTypeRef = StringRef,
        children = List(
          SelectValue(
            name = "concatSeq[0]",
            typeRef = ArrayRef(StringRef),
            qualifier = ThisTypeRef,
            term = "list"
          ),
          ListValue(
            name = "concatSeq[1]",
            typeRef = ArrayRef(StringRef),
            valueTypeRef = StringRef,
            elements = List(
              LiteralValue("concatSeq[1][0]", StringRef, "\"foo\""),
              LiteralValue("concatSeq[1][1]", StringRef, "\"bar\"")
            )
          ),
          ListValue(
            name = "concatSeq[2]",
            typeRef = ArrayRef(StringRef),
            valueTypeRef = StringRef,
            elements =
              List(LiteralValue("concatSeq[2][0]", StringRef, "\"lorem\""))
          )
        )
      ),
      MergedListsValue(
        name = "concatList",
        valueTypeRef = StringRef,
        children = List(
          ListValue(
            name = "concatList[0]",
            typeRef = ArrayRef(StringRef),
            valueTypeRef = StringRef,
            elements =
              List(LiteralValue("concatList[0][0]", StringRef, "\"foo\""))
          ),
          SelectValue(
            name = "concatList[1]",
            typeRef = ArrayRef(StringRef),
            qualifier = ThisTypeRef,
            term = "list"
          )
        )
      ),
      MergedSetsValue(
        name = "mergedSet",
        valueTypeRef = NumberRef,
        children = List(
          SelectValue(
            name = "mergedSet[0]",
            typeRef = ArrayRef(NumberRef),
            qualifier = ThisTypeRef,
            term = "set"
          ),
          SetValue(
            name = "mergedSet[1]",
            typeRef = ArrayRef(NumberRef),
            valueTypeRef = NumberRef,
            elements = Set(LiteralValue("mergedSet[1][0]", NumberRef, "3"))
          )
        )
      )
    ),
    superInterface = Some(
      InterfaceDeclaration(
        "SupI",
        ListSet.empty,
        List.empty[String],
        None,
        false
      )
    )
  )

  val enum1 = EnumDeclaration(
    "ScalaRuntimeFixturesTestEnumeration",
    ListSet("A", "B", "C")
  )

  val union1 = UnionDeclaration(
    name = "ScalaRuntimeFixturesFamily",
    fields = ListSet(Member("foo", StringRef)),
    possibilities = ListSet(
      CustomTypeRef("ScalaRuntimeFixturesFamilyMember1", List.empty),
      SingletonTypeRef(
        "ScalaRuntimeFixturesFamilyMember2",
        ListSet(LiteralValue("foo", StringRef, "\"bar\""))
      ),
      SingletonTypeRef(
        "ScalaRuntimeFixturesFamilyMember3",
        ListSet(LiteralValue("foo", StringRef, "\"lorem\""))
      )
    ),
    superInterface = Option.empty
  )

  val unionIface = InterfaceDeclaration(
    s"ScalaRuntimeFixtures${sealedFamily1.identifier.name}",
    ListSet(Member("foo", StringRef)),
    typeParams = List.empty[String],
    superInterface = Option.empty,
    union = true
  )

  val unionMember2Singleton = SingletonDeclaration(
    "ScalaRuntimeFixturesFamilyMember2",
    ListSet(LiteralValue("foo", StringRef, "\"bar\"")),
    Some(unionIface)
  )

}
