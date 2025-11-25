package io.github.scalats.core

import io.github.scalats.{ scala => ScalaModel }
import io.github.scalats.ast._
import io.github.scalats.core.Internals.ListSet

import ScalaRuntimeFixtures.results._
import TranspilerCompat.{ ns, valueClassNs }

final class TranspilerSpec
    extends org.specs2.mutable.Specification
    with TranspilerExtraSpec {

  "Transpiler".title

  import TranspilerResults._
  import ScalaModel.QualifiedIdentifier

  "Transpiler" should {
    "transpile a case class with one primitive member" in {
      val result = defaultTranspiler(
        Map(caseClass1.identifier.name -> ListSet(caseClass1))
      )

      result must have size 1 and {
        result must contain(caseClass1.identifier.name -> ListSet(interface1))
      }
    }

    "transpile a generic class with one member" in {
      val result = defaultTranspiler(
        Map(caseClass2.identifier.name -> ListSet(caseClass2))
      )

      result must have size 1 and {
        result must contain(caseClass2.identifier.name -> ListSet(interface2))
      }
    }

    "transpile a generic case class with one member list of type parameter" in {
      val result = defaultTranspiler(
        Map(caseClass3.identifier.name -> ListSet(caseClass3))
      )

      result must have size 1 and {
        result must contain(caseClass3.identifier.name -> ListSet(interface3))
      }
    }

    "transpile a generic case class with one optional member" in {
      val result = defaultTranspiler(
        Map(caseClass5.identifier.name -> ListSet(caseClass5))
      )

      result must have size 1 and {
        result must contain(caseClass5.identifier.name -> ListSet(interface5))
      }
    }

    "transpile disjunction types" in {
      val result = defaultTranspiler(
        Map(caseClass7.identifier.name -> ListSet(caseClass7))
      )

      result must have size 1 and {
        result must contain(caseClass7.identifier.name -> ListSet(interface7))
      }
    }

    "transpile a tagged type" >> {
      "as tagged declaration" in {
        val result =
          defaultTranspiler(Map(tagged1.identifier.name -> ListSet(tagged1)))

        result must have size 1 and {
          result must contain(
            tagged1.identifier.name -> ListSet(taggedDeclaration1)
          )
        }
      }

      "interface member" in {
        val result = defaultTranspiler(
          Map(caseClass8.identifier.name -> ListSet(caseClass8))
        )

        result must have size 1 and {
          result must contain(caseClass8.identifier.name -> ListSet(interface8))
        }
      }
    }

    "transpile Tuple types" in {
      val result = defaultTranspiler(
        Map(caseClass10.identifier.name -> ListSet(caseClass10))
      )

      result must have size 1 and {
        result must contain(caseClass10.identifier.name -> ListSet(interface10))
      }
    }

    "transpile enumeration" in {
      val result = defaultTranspiler(
        Map(testEnumeration.identifier.name -> ListSet(testEnumeration))
      )

      result must have size 1 and {
        result must contain(testEnumeration.identifier.name -> ListSet(enum1))
      }
    }

    "transpile case object" in {
      val result = defaultTranspiler(
        Map(caseObject1.identifier.name -> ListSet(caseObject1))
      )

      result must have size 1 and {
        result must contain(caseObject1.identifier.name -> ListSet(singleton1))
      }
    }

    "transpile object" >> {
      "with complex invariants" in {
        val result = defaultTranspiler(
          Map(
            caseObject2.identifier.name -> ListSet(caseObject2),
            "Nested1" -> ListSet(
              ScalaModel.CaseObject(
                ScalaModel.QualifiedIdentifier("Nested1", Nil),
                ListSet(
                  ScalaModel.LiteralInvariant(
                    "name",
                    ScalaModel.StringRef,
                    "\"Foo \\\"bar\\\"\""
                  )
                )
              )
            )
          ),
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

        result must have size 2 and {
          result must contain(
            caseObject2.identifier.name -> ListSet(singleton2)
          )
        } and {
          result.exists(_._1 == "Nested1") must beTrue
        }
      }

      "with tagged invariants" in {
        import ScalaRuntimeFixtures.results.{ valueClassNs => vcns }

        val taggedRef = ScalaModel.TaggedRef(
          identifier = QualifiedIdentifier("AnyValChild", vcns),
          tagged = ScalaModel.StringRef
        )

        val obj = ScalaModel.CaseObject(
          QualifiedIdentifier("TestObject3", vcns),
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

        val result = defaultTranspiler(Map(obj.identifier.name -> ListSet(obj)))

        result must have size 1 and {
          result must contain(obj.identifier.name -> ListSet(singleton3))
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
              typeRef = ListRef(greetingTypeRef, false),
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

        val result = defaultTranspiler(Map(obj.identifier.name -> ListSet(obj)))

        result must have size 1 and {
          result must contain(obj.identifier.name -> ListSet(singleton4))
        }
      }

      "with reference to another empty object" >> {
        val referencedId = QualifiedIdentifier("Referenced", Nil)
        val referencing = ScalaModel.CaseObject(
          QualifiedIdentifier("Referencing", Nil),
          ListSet(
            ScalaModel
              .LiteralInvariant("name", ScalaModel.StringRef, "\"Foo\""),
            ScalaModel.ObjectInvariant(
              "foo",
              ScalaModel.CaseObjectRef(referencedId)
            )
          )
        )

        def spec(tps: Map[String, ListSet[ScalaModel.TypeDef]], sz: Int) = {
          val result = defaultTranspiler(tps)

          result must have size sz and {
            result must contain(
              referencing.identifier.name -> ListSet(
                SingletonDeclaration(
                  "Referencing",
                  ListSet(LiteralValue("name", StringRef, "\"Foo\"") /* SingletonValue not transpile as CaseObjectRef do not resolve */ ),
                  None
                )
              )
            )
          }
        }

        "skip unresolved object invariant" in {
          spec(
            Map(
              referencing.identifier.name -> ListSet(referencing),
              referencedId.name -> ListSet.empty[ScalaModel.TypeDef]
            ),
            sz = 1
          )
        }

        "skip object invariant resolved to not singleton type" in {
          spec(
            Map(
              referencing.identifier.name -> ListSet(referencing),
              referencedId.name -> ListSet(
                ScalaModel.CaseClass(
                  identifier = referencedId,
                  fields = ListSet.empty,
                  values = ListSet.empty,
                  typeArgs = List.empty
                )
              )
            ),
            sz = 2
          )
        }

        "skip object invariant resolved to empty singleton" in {
          spec(
            Map(
              referencing.identifier.name -> ListSet(referencing),
              referencedId.name -> ListSet(
                ScalaModel.CaseObject(referencedId, ListSet.empty),
                ScalaModel.CaseClass(
                  identifier = referencedId,
                  fields = ListSet.empty,
                  values = ListSet.empty,
                  typeArgs = List.empty
                )
              )
            ),
            sz = 2
          )
        }

        "not skip object invariant resolved to empty singleton" in {
          // When is the only type for the name

          val result = defaultTranspiler(
            Map(
              referencing.identifier.name -> ListSet(referencing),
              referencedId.name -> ListSet(
                ScalaModel.CaseObject(referencedId, ListSet.empty)
              )
            )
          )

          result must have size 2 and {
            result must contain(
              referencing.identifier.name -> ListSet(
                SingletonDeclaration(
                  "Referencing",
                  ListSet(
                    LiteralValue("name", StringRef, "\"Foo\""),
                    SingletonValue(
                      "foo",
                      SingletonTypeRef(referencedId.name, ListSet.empty)
                    )
                  ),
                  None
                )
              )
            )
          }
        }
      }
    }

    "transpile sealed trait as union" in {
      val result = defaultTranspiler(
        Map(sealedFamily1.identifier.name -> ListSet(sealedFamily1))
      )

      result must have size 4 and {
        result must contain(sealedFamily1.identifier.name -> ListSet(union1))
      } and {
        result must contain(
          unionMember2Singleton.name -> ListSet(unionMember2Singleton)
        )
      } and {

        val member1Interface = InterfaceDeclaration(
          s"${ns}FamilyMember1",
          ListSet(Member("foo", StringRef)),
          List.empty,
          Some(unionIface),
          false
        )

        result must contain(member1Interface.name -> ListSet(member1Interface))
      } and {
        result must contain(
          s"${ns}FamilyMember3" -> ListSet(
            SingletonDeclaration(
              s"${ns}FamilyMember3",
              ListSet(LiteralValue("foo", StringRef, "\"lorem\"")),
              Some(unionIface)
            )
          )
        )
      }
    }
  }
}

object TranspilerResults {

  private val defaultt = new Transpiler(
    Settings(),
    Logger(org.slf4j.LoggerFactory getLogger getClass)
  )

  def defaultTranspiler(
      in: Map[String, ListSet[ScalaModel.TypeDef]]
    ): List[(String, ListSet[Declaration])] =
    defaultt.apply(in).toList

  def defaultTranspiler(
      scalaTypes: Map[String, ListSet[ScalaModel.TypeDef]],
      superInterface: Option[InterfaceDeclaration]
    ): List[(String, ListSet[Declaration])] =
    defaultt(scalaTypes, superInterface).toList

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
    ListSet(Member("name", ArrayRef(SimpleTypeRef("T"), true))),
    typeParams = List("T"),
    superInterface = Option.empty,
    union = false
  )

  val interface5 = InterfaceDeclaration(
    s"${ns}TestClass5",
    ListSet(
      Member("name", NullableType(SimpleTypeRef("T"))),
      Member("counters", MapType(StringRef, NumberRef.bigInt)),
      Member("time", TimeRef)
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
        ArrayRef(TaggedRef(s"${valueClassNs}AnyValChild", StringRef), false)
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
      Member("tuple", TupleRef(List(NumberRef.int))),
      Member("tupleA", TupleRef(List(StringRef, NumberRef.int))),
      Member("tupleB", TupleRef(List(StringRef, NumberRef.long))),
      Member("tupleC", TupleRef(List(StringRef, StringRef, NumberRef.long)))
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
      LiteralValue("name", StringRef, "\"Foo \\\"bar\\\"\""),
      LiteralValue("code", NumberRef.int, "1"),
      LiteralValue("const", StringRef, "\"value\""),
      SelectValue("foo", StringRef, ThisTypeRef, "name"),
      ListValue(
        name = "list",
        typeRef = ArrayRef(StringRef, true),
        valueTypeRef = StringRef,
        elements = List(
          LiteralValue("list[0]", StringRef, "\"first\""),
          SelectValue("list[1]", StringRef, ThisTypeRef, "name")
        )
      ),
      SetValue(
        name = "set",
        typeRef = SetRef(NumberRef.int),
        valueTypeRef = NumberRef.int,
        elements = Set(
          SelectValue("set[0]", NumberRef.int, ThisTypeRef, "code"),
          LiteralValue("set[1]", NumberRef.int, "2")
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
        valueTypeRef = ArrayRef(StringRef, false),
        entries = Map(
          LiteralValue(
            "dictOfList.0",
            StringRef,
            "\"excludes\""
          ) -> ListValue(
            name = "dictOfList[0]",
            typeRef = ArrayRef(StringRef, false),
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
            typeRef = ArrayRef(StringRef, false),
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
            typeRef = ArrayRef(StringRef, nonEmptySelectedListInvariant),
            qualifier = ThisTypeRef,
            term = "list"
          ),
          ListValue(
            name = "concatSeq[1]",
            typeRef = ArrayRef(StringRef, false),
            valueTypeRef = StringRef,
            elements = List(
              LiteralValue("concatSeq[1][0]", StringRef, "\"foo\""),
              LiteralValue("concatSeq[1][1]", StringRef, "\"bar\"")
            )
          ),
          ListValue(
            name = "concatSeq[2]",
            typeRef = ArrayRef(StringRef, false),
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
            typeRef = ArrayRef(StringRef, false),
            valueTypeRef = StringRef,
            elements =
              List(LiteralValue("concatList[0][0]", StringRef, "\"foo\""))
          ),
          SelectValue(
            name = "concatList[1]",
            typeRef = ArrayRef(StringRef, true),
            qualifier = ThisTypeRef,
            term = "list"
          )
        )
      ),
      MergedSetsValue(
        name = "mergedSet",
        valueTypeRef = NumberRef.int,
        children = List(
          SelectValue(
            name = "mergedSet[0]",
            typeRef = SetRef(NumberRef.int),
            qualifier = ThisTypeRef,
            term = "set"
          ),
          SetValue(
            name = "mergedSet[1]",
            typeRef = SetRef(NumberRef.int),
            valueTypeRef = NumberRef.int,
            elements = Set(LiteralValue("mergedSet[1][0]", NumberRef.int, "3"))
          )
        )
      ),
      TupleValue(
        name = "tuple1",
        typeRef = TupleRef(List(StringRef, NumberRef.int, NumberRef.double)),
        values = List(
          LiteralValue("_1", StringRef, "\"foo\""),
          LiteralValue("_2", NumberRef.int, "2"),
          LiteralValue("_3", NumberRef.double, "3.0")
        )
      ),
      TupleValue(
        name = "tuple2",
        typeRef = TupleRef(List(StringRef, NumberRef.int)),
        values = List(
          LiteralValue("_1", StringRef, "\"bar\""),
          LiteralValue("_2", NumberRef.int, "2")
        )
      ),
      SelectValue(
        "tuple3",
        TupleRef(List(StringRef, NumberRef.int, NumberRef.double)),
        ThisTypeRef,
        "tuple1"
      ),
      TupleValue(
        name = "tuple4",
        typeRef = TupleRef(List(StringRef, NumberRef.int, NumberRef.int)),
        values = List(
          LiteralValue("_1", StringRef, "\"lorem\""),
          LiteralValue("_2", NumberRef.int, "10"),
          LiteralValue("_3", NumberRef.int, "20")
        )
      ),
      SingletonValue(
        name = "Nested1",
        typeRef = SingletonTypeRef(
          name = s"${ns}TestObject2Nested1",
          values = ListSet.empty
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
          typeRef = ArrayRef(greetingTypeRef, false),
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
    name = s"${ns}TestEnumeration",
    possibilities = ListSet("A", "B", "C"),
    values = ListSet.empty
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
