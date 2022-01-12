package io.github.scalats.core

import io.github.scalats.{ scala => ScalaModel }
import io.github.scalats.core.Internals.ListSet
import io.github.scalats.typescript._

import ScalaRuntimeFixtures.results._
import TranspilerCompat.{ ns, valueClassNs }

final class TranspilerSpec extends org.specs2.mutable.Specification {
  "Transpiler".title

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

    "transpile object" >> {
      "with complex invariants" in {
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

      "with tagged invariants" in {
        import ScalaRuntimeFixtures.results.{ valueClassNs => vcns }

        val taggedRef = ScalaModel.TaggedRef(
          identifier = ScalaModel.QualifiedIdentifier("AnyValChild", vcns),
          tagged = ScalaModel.StringRef
        )

        val obj = ScalaModel.CaseObject(
          ScalaModel.QualifiedIdentifier("TestObject3", vcns),
          ListSet(
            ScalaModel.LiteralInvariant("name", taggedRef, "\"Foo\""),
            ScalaModel.DictionaryInvariant(
              name = "mapping",
              keyTypeRef = taggedRef,
              valueTypeRef = ScalaModel.StringRef,
              entries = Map(
                ScalaModel.LiteralInvariant(
                  "mapping.0",
                  taggedRef,
                  "\"foo\""
                ) -> ScalaModel.LiteralInvariant(
                  "mapping[0]",
                  ScalaModel.StringRef,
                  "\"bar\""
                ),
                ScalaModel.SelectInvariant(
                  "mapping.1",
                  taggedRef,
                  ScalaModel.ThisTypeRef,
                  "name"
                ) -> ScalaModel.LiteralInvariant(
                  "mapping[1]",
                  ScalaModel.StringRef,
                  "\"lorem\""
                )
              )
            )
          )
        )

        val result = defaultTranspiler(ListSet(obj))

        result must have size 1 and {
          result must contain(singleton3)
        }
      }

      "with enum invariants" in {
        import ScalaModel._

        val greetingTypeRef =
          UnknownTypeRef(QualifiedIdentifier("Greeting", Nil))

        val helloTypeRef = UnknownTypeRef(QualifiedIdentifier("Hello", Nil))
        val hiTypeRef = UnknownTypeRef(QualifiedIdentifier("Hi", Nil))

        val obj = CaseObject(
          identifier = QualifiedIdentifier("Words", Nil),
          values = ListSet(
            ListInvariant(
              name = "start",
              typeRef = CollectionRef(greetingTypeRef),
              valueTypeRef = greetingTypeRef,
              values = List(
                SelectInvariant(
                  "start[0]",
                  helloTypeRef,
                  greetingTypeRef,
                  "Hello"
                ),
                SelectInvariant("start[1]", hiTypeRef, greetingTypeRef, "Hi")
              )
            )
          )
        )

        val result = defaultTranspiler(ListSet(obj))

        result must have size 1 and {
          result must contain(singleton4)
        }
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
          s"${ns}FamilyMember1",
          ListSet(Member("foo", StringRef)),
          List.empty,
          Some(unionIface),
          false
        )

        result must contain(member1Interface)
      } and {
        result must contain(
          SingletonDeclaration(
            s"${ns}FamilyMember3",
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
    s"${ns}TestClass1",
    ListSet(Member("name", StringRef)),
    List.empty,
    Option.empty,
    false
  )

  val interface2 = InterfaceDeclaration(
    s"${ns}TestClass2",
    ListSet(Member("name", SimpleTypeRef("T"))),
    typeParams = List("T"),
    superInterface = Option.empty,
    union = false
  )

  val interface3 = InterfaceDeclaration(
    s"${ns}TestClass3",
    ListSet(Member("name", ArrayRef(SimpleTypeRef("T")))),
    typeParams = List("T"),
    superInterface = Option.empty,
    union = false
  )

  val interface5 = InterfaceDeclaration(
    s"${ns}TestClass5",
    ListSet(
      Member("name", NullableType(SimpleTypeRef("T"))),
      Member("counters", MapType(StringRef, NumberRef))
    ),
    typeParams = List("T"),
    superInterface = Option.empty,
    union = false
  )

  val interface7 = InterfaceDeclaration(
    s"${ns}TestClass7",
    ListSet(
      Member(
        "name",
        UnionType(
          ListSet(
            CustomTypeRef(s"${ns}TestClass1", List.empty),
            CustomTypeRef(s"${ns}TestClass1B", List.empty)
          )
        )
      )
    ),
    typeParams = List("T"),
    superInterface = Option.empty,
    union = false
  )

  val taggedDeclaration1 = TaggedDeclaration(
    name = s"${valueClassNs}AnyValChild",
    field = Member("value", StringRef)
  )

  val interface8 = InterfaceDeclaration(
    s"${valueClassNs}TestClass8",
    ListSet(
      Member("name", TaggedRef(s"${valueClassNs}AnyValChild", StringRef)),
      Member(
        "aliases",
        ArrayRef(TaggedRef(s"${valueClassNs}AnyValChild", StringRef))
      )
    ),
    typeParams = List.empty,
    superInterface = Option.empty,
    union = false
  )

  val interface10 = InterfaceDeclaration(
    s"${ns}TestClass10",
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
    name = s"${ns}TestObject1",
    values = ListSet.empty,
    superInterface = Option.empty
  )

  val singleton2 = SingletonDeclaration(
    s"${ns}TestObject2",
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
        keyTypeRef = StringRef,
        valueTypeRef = StringRef,
        entries = Map(
          LiteralValue("mapping.0", StringRef, "\"foo\"") -> LiteralValue(
            "mapping[0]",
            StringRef,
            "\"bar\""
          ),
          LiteralValue("mapping.1", StringRef, "\"lorem\"") -> SelectValue(
            "mapping[1]",
            StringRef,
            ThisTypeRef,
            "name"
          )
        )
      ),
      DictionaryValue(
        name = "dictOfList",
        keyTypeRef = StringRef,
        valueTypeRef = ArrayRef(StringRef),
        entries = Map(
          LiteralValue(
            "dictOfList.0",
            StringRef,
            "\"excludes\""
          ) -> ListValue(
            name = "dictOfList[0]",
            typeRef = ArrayRef(StringRef),
            valueTypeRef = StringRef,
            elements = List(
              LiteralValue(
                "dictOfList[0][0]",
                StringRef,
                "\"*.txt\""
              ),
              LiteralValue(
                "dictOfList[0][1]",
                StringRef,
                "\".gitignore\""
              )
            )
          ),
          LiteralValue(
            "dictOfList.1",
            StringRef,
            "\"includes\""
          ) -> ListValue(
            name = "dictOfList[1]",
            typeRef = ArrayRef(StringRef),
            valueTypeRef = StringRef,
            elements = List(
              LiteralValue(
                "dictOfList[1][0]",
                StringRef,
                "\"images/**\""
              ),
              LiteralValue(
                "dictOfList[1][1]",
                StringRef,
                "\"*.jpg\""
              ),
              LiteralValue(
                "dictOfList[1][2]",
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

  val singleton3 = {
    val taggedTpe =
      TaggedRef(s"${valueClassNs}AnyValChild", StringRef)

    SingletonDeclaration(
      s"${valueClassNs}TestObject3",
      ListSet(
        LiteralValue("name", taggedTpe, "\"Foo\""),
        DictionaryValue(
          name = "mapping",
          keyTypeRef = taggedTpe,
          valueTypeRef = StringRef,
          entries = Map(
            LiteralValue(
              "mapping.0",
              taggedTpe,
              "\"foo\""
            ) -> LiteralValue(
              "mapping[0]",
              StringRef,
              "\"bar\""
            ),
            SelectValue(
              "mapping.1",
              taggedTpe,
              ThisTypeRef,
              "name"
            ) -> LiteralValue(
              "mapping[1]",
              StringRef,
              "\"lorem\""
            )
          )
        )
      ),
      superInterface = None
    )
  }

  val singleton4: SingletonDeclaration = {
    val greetingTypeRef = CustomTypeRef("Greeting", Nil)
    val helloTypeRef = CustomTypeRef("Hello", Nil)
    val hiTypeRef = CustomTypeRef("Hi", Nil)

    SingletonDeclaration(
      name = "Words",
      values = ListSet(
        ListValue(
          name = "start",
          typeRef = ArrayRef(greetingTypeRef),
          valueTypeRef = greetingTypeRef,
          elements = List(
            SelectValue(
              "start[0]",
              helloTypeRef,
              greetingTypeRef,
              "Hello"
            ),
            SelectValue("start[1]", hiTypeRef, greetingTypeRef, "Hi")
          )
        )
      ),
      superInterface = None
    )
  }

  val enum1 = EnumDeclaration(
    s"${ns}TestEnumeration",
    ListSet("A", "B", "C")
  )

  val union1 = UnionDeclaration(
    name = s"${ns}Family",
    fields = ListSet(Member("foo", StringRef)),
    possibilities = ListSet(
      CustomTypeRef(s"${ns}FamilyMember1", List.empty),
      SingletonTypeRef(
        s"${ns}FamilyMember2",
        ListSet(LiteralValue("foo", StringRef, "\"bar\""))
      ),
      SingletonTypeRef(
        s"${ns}FamilyMember3",
        ListSet(LiteralValue("foo", StringRef, "\"lorem\""))
      )
    ),
    superInterface = Option.empty
  )

  val unionIface = InterfaceDeclaration(
    s"${ns}${sealedFamily1.identifier.name}",
    ListSet(Member("foo", StringRef)),
    typeParams = List.empty[String],
    superInterface = Option.empty,
    union = true
  )

  val unionMember2Singleton = SingletonDeclaration(
    s"${ns}FamilyMember2",
    ListSet(LiteralValue("foo", StringRef, "\"bar\"")),
    Some(unionIface)
  )

}
