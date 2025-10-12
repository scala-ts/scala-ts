package io.github.scalats.core

import io.github.scalats.core.Internals.ListSet
import io.github.scalats.scala._

final class ScalaParserResults(
    val ns: List[String],
    val valueClassNs: List[String]) {

  val caseClass1 = CaseClass(
    identifier = QualifiedIdentifier("TestClass1", ns),
    fields = ListSet(TypeMember("name", StringRef)),
    values = ListSet.empty,
    typeArgs = List.empty
  )

  val caseClass1B = CaseClass(
    identifier = QualifiedIdentifier("TestClass1B", ns),
    fields = ListSet(TypeMember("foo", StringRef)),
    values = ListSet.empty,
    typeArgs = List.empty
  )

  val caseClass2 = CaseClass(
    identifier = QualifiedIdentifier("TestClass2", ns),
    fields = ListSet(TypeMember("name", TypeParamRef("T"))),
    values = ListSet.empty,
    typeArgs = List("T")
  )

  val caseClass3 = CaseClass(
    identifier = QualifiedIdentifier("TestClass3", ns),
    fields = ListSet(TypeMember("name", ListRef(TypeParamRef("T")))),
    values = ListSet.empty,
    typeArgs = List("T")
  )

  val caseClass4 = CaseClass(
    identifier = QualifiedIdentifier("TestClass4", ns),
    fields = ListSet(
      TypeMember(
        "name",
        CaseClassRef(
          QualifiedIdentifier("TestClass3", ns),
          typeArgs = List(TypeParamRef("T"))
        )
      )
    ),
    values = ListSet.empty,
    typeArgs = List("T")
  )

  val caseClass5 = CaseClass(
    identifier = QualifiedIdentifier("TestClass5", ns),
    fields = ListSet(
      TypeMember("name", OptionRef(TypeParamRef("T"))),
      TypeMember("counters", MapRef(StringRef, BigIntegerRef)),
      TypeMember("time", TimeRef)
    ),
    values = ListSet.empty,
    typeArgs = List("T")
  )

  val caseClass6 = CaseClass(
    identifier = QualifiedIdentifier("TestClass6", ns),
    fields = ListSet(
      TypeMember(
        "name",
        OptionRef(
          CaseClassRef(
            QualifiedIdentifier("TestClass5", ns),
            typeArgs = List(
              ListRef(
                OptionRef(
                  CaseClassRef(
                    QualifiedIdentifier(
                      "TestClass4",
                      ns
                    ),
                    typeArgs = List(StringRef)
                  )
                )
              )
            )
          )
        )
      ),
      TypeMember(
        "age",
        CaseClassRef(
          QualifiedIdentifier("TestClass3", ns),
          typeArgs = List(
            CaseClassRef(
              QualifiedIdentifier("TestClass2", ns),
              typeArgs = List(
                CaseClassRef(
                  QualifiedIdentifier(
                    "TestClass1",
                    ns
                  ),
                  typeArgs = List.empty
                )
              )
            )
          )
        )
      )
    ),
    values = ListSet.empty,
    typeArgs = List("T")
  )

  val caseClass7 = CaseClass(
    identifier = QualifiedIdentifier("TestClass7", ns),
    fields = ListSet(
      TypeMember(
        "name",
        UnionRef(
          ListSet(
            CaseClassRef(
              QualifiedIdentifier("TestClass1", ns),
              typeArgs = List.empty
            ),
            CaseClassRef(
              QualifiedIdentifier("TestClass1B", ns),
              typeArgs = List.empty
            )
          )
        )
      )
    ),
    values = ListSet.empty,
    typeArgs = List("T")
  )

  val tagged1 = ValueClass(
    identifier = QualifiedIdentifier("AnyValChild", valueClassNs),
    field = TypeMember("value", StringRef)
  )

  val caseClass8 = CaseClass(
    identifier = QualifiedIdentifier("TestClass8", valueClassNs),
    fields = ListSet(
      TypeMember(
        "name",
        TaggedRef(
          identifier = QualifiedIdentifier("AnyValChild", valueClassNs),
          tagged = StringRef
        )
      ),
      TypeMember(
        "aliases",
        ListRef(
          TaggedRef(
            identifier = QualifiedIdentifier("AnyValChild", valueClassNs),
            tagged = StringRef
          )
        )
      )
    ),
    values = ListSet.empty,
    typeArgs = List.empty
  )

  val caseClass9 = CaseClass(
    identifier = QualifiedIdentifier("TestClass9", ns),
    fields = ListSet(
      TypeMember(
        "name",
        EnumerationRef(
          QualifiedIdentifier("TestEnumeration", ns)
        )
      )
    ),
    values = ListSet.empty,
    typeArgs = List.empty
  )

  val testEnumeration = EnumerationDef(
    identifier = QualifiedIdentifier("TestEnumeration", ns),
    possibilities = ListSet("A", "B", "C"),
    values = ListSet.empty
  )

  val caseClass10 = CaseClass(
    identifier = QualifiedIdentifier("TestClass10", ns),
    fields = ListSet(
      TypeMember("name", StringRef),
      TypeMember("tuple", TupleRef(List(IntRef))),
      TypeMember("tupleA", TupleRef(List(StringRef, IntRef))),
      TypeMember("tupleB", TupleRef(List(StringRef, LongRef))),
      TypeMember("tupleC", TupleRef(List(StringRef, StringRef, LongRef)))
    ),
    values = ListSet.empty,
    typeArgs = List.empty
  )

  val caseObject1 = CaseObject(
    QualifiedIdentifier("TestObject1", ns),
    ListSet.empty
  )

  val caseObject2 = CaseObject(
    QualifiedIdentifier("TestObject2", ns),
    ListSet(
      LiteralInvariant("name", StringRef, "\"Foo \\\"bar\\\"\""),
      LiteralInvariant("code", IntRef, "1"),
      LiteralInvariant("const", StringRef, "\"value\""),
      SelectInvariant("foo", StringRef, ThisTypeRef, "name"),
      ListInvariant(
        name = "list",
        typeRef = ListRef(StringRef),
        valueTypeRef = StringRef,
        values = List(
          LiteralInvariant("list[0]", StringRef, "\"first\""),
          SelectInvariant("list[1]", StringRef, ThisTypeRef, "name")
        )
      ),
      SetInvariant(
        name = "set",
        typeRef = SetRef(IntRef),
        valueTypeRef = IntRef,
        values = Set(
          SelectInvariant("set[0]", IntRef, ThisTypeRef, "code"),
          LiteralInvariant("set[1]", IntRef, "2")
        )
      ),
      DictionaryInvariant(
        name = "mapping",
        keyTypeRef = StringRef,
        valueTypeRef = StringRef,
        entries = Map(
          LiteralInvariant(
            "mapping.0",
            StringRef,
            "\"foo\""
          ) -> LiteralInvariant("mapping[0]", StringRef, "\"bar\""),
          LiteralInvariant(
            "mapping.1",
            StringRef,
            "\"lorem\""
          ) -> SelectInvariant(
            "mapping[1]",
            StringRef,
            ThisTypeRef,
            "name"
          )
        )
      ),
      DictionaryInvariant(
        name = "dictOfList",
        keyTypeRef = StringRef,
        valueTypeRef = ListRef(StringRef),
        entries = Map(
          LiteralInvariant(
            "dictOfList.0",
            StringRef,
            "\"excludes\""
          ) -> ListInvariant(
            "dictOfList[0]",
            ListRef(StringRef),
            StringRef,
            List(
              LiteralInvariant(
                "dictOfList[0][0]",
                StringRef,
                "\"*.txt\""
              ),
              LiteralInvariant(
                "dictOfList[0][1]",
                StringRef,
                "\".gitignore\""
              )
            )
          ),
          LiteralInvariant(
            "dictOfList.1",
            StringRef,
            "\"includes\""
          ) -> ListInvariant(
            "dictOfList[1]",
            ListRef(StringRef),
            StringRef,
            List(
              LiteralInvariant(
                "dictOfList[1][0]",
                StringRef,
                "\"images/**\""
              ),
              LiteralInvariant(
                "dictOfList[1][1]",
                StringRef,
                "\"*.jpg\""
              ),
              LiteralInvariant(
                "dictOfList[1][2]",
                StringRef,
                "\"*.png\""
              )
            )
          )
        )
      ),
      MergedListsInvariant(
        name = "concatSeq",
        valueTypeRef = StringRef,
        children = List(
          SelectInvariant(
            name = "concatSeq[0]",
            typeRef = ListRef(StringRef),
            qualifier = ThisTypeRef,
            term = "list"
          ),
          ListInvariant(
            name = "concatSeq[1]",
            typeRef = ListRef(StringRef),
            valueTypeRef = StringRef,
            values = List(
              LiteralInvariant("concatSeq[1][0]", StringRef, "\"foo\""),
              LiteralInvariant("concatSeq[1][1]", StringRef, "\"bar\"")
            )
          ),
          ListInvariant(
            name = "concatSeq[2]",
            typeRef = ListRef(StringRef),
            valueTypeRef = StringRef,
            values =
              List(LiteralInvariant("concatSeq[2][0]", StringRef, "\"lorem\""))
          )
        )
      ),
      MergedListsInvariant(
        name = "concatList",
        valueTypeRef = StringRef,
        children = List(
          ListInvariant(
            name = "concatList[0]",
            typeRef = ListRef(StringRef),
            valueTypeRef = StringRef,
            values =
              List(LiteralInvariant("concatList[0][0]", StringRef, "\"foo\""))
          ),
          SelectInvariant(
            name = "concatList[1]",
            typeRef = ListRef(StringRef),
            qualifier = ThisTypeRef,
            term = "list"
          )
        )
      ),
      MergedSetsInvariant(
        name = "mergedSet",
        valueTypeRef = IntRef,
        children = List(
          SelectInvariant(
            name = "mergedSet[0]",
            typeRef = SetRef(IntRef),
            qualifier = ThisTypeRef,
            term = "set"
          ),
          SetInvariant(
            name = "mergedSet[1]",
            typeRef = SetRef(IntRef),
            valueTypeRef = IntRef,
            values = Set(LiteralInvariant("mergedSet[1][0]", IntRef, "3"))
          )
        )
      ),
      TupleInvariant(
        name = "tuple1",
        typeRef = TupleRef(List(StringRef, IntRef, DoubleRef)),
        values = List(
          LiteralInvariant(
            name = "_1",
            typeRef = StringRef,
            value = "\"foo\""
          ),
          LiteralInvariant(
            name = "_2",
            typeRef = IntRef,
            value = "2"
          ),
          LiteralInvariant(
            name = "_3",
            typeRef = DoubleRef,
            value = "3.0"
          )
        )
      ),
      TupleInvariant(
        name = "tuple2",
        typeRef = TupleRef(List(StringRef, IntRef)),
        values = List(
          LiteralInvariant(
            name = "_1",
            typeRef = StringRef,
            value = "\"bar\""
          ),
          LiteralInvariant(
            name = "_2",
            typeRef = IntRef,
            value = "2"
          )
        )
      ),
      SelectInvariant(
        "tuple3",
        TupleRef(List(StringRef, IntRef, DoubleRef)),
        ThisTypeRef,
        "tuple1"
      ),
      TupleInvariant(
        name = "tuple4",
        typeRef = TupleRef(List(StringRef, IntRef, IntRef)),
        values = List(
          LiteralInvariant(
            name = "_1",
            typeRef = StringRef,
            value = "\"lorem\""
          ),
          LiteralInvariant(
            name = "_2",
            typeRef = IntRef,
            value = "10"
          ),
          LiteralInvariant(
            name = "_3",
            typeRef = IntRef,
            value = "20"
          )
        )
      ),
      ObjectInvariant(
        name = "Nested1",
        typeRef =
          CaseObjectRef(QualifiedIdentifier("Nested1", ns :+ "TestObject2"))
      )
    )
  )

  val nestedObj1 = CaseObject(
    QualifiedIdentifier("Nested1", ns :+ "TestObject2"),
    ListSet.empty
  )

  val sealedFamily1 = {
    // Not 'bar', as not abstract
    val fooMember = TypeMember("foo", StringRef)
    val fooBar = LiteralInvariant("foo", StringRef, "\"bar\"")
    val fooLorem = LiteralInvariant("foo", StringRef, "\"lorem\"")
    val code = LiteralInvariant("code", IntRef, "1")

    SealedUnion(
      identifier = QualifiedIdentifier("Family", ns),
      fields = ListSet(fooMember),
      possibilities = ListSet(
        CaseClass(
          identifier = QualifiedIdentifier("FamilyMember1", ns),
          fields = ListSet(fooMember),
          values = ListSet(code),
          typeArgs = List.empty
        ),
        CaseObject(
          QualifiedIdentifier("FamilyMember2", ns),
          ListSet(fooBar)
        ),
        CaseObject(
          QualifiedIdentifier("FamilyMember3", ns),
          ListSet(fooLorem)
        )
      )
    )
  }
}
