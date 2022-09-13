package io.github.scalats.python

import io.github.scalats.core.{
  Settings,
  TranspilerResults,
  TypeScriptDeclarationMapper,
  TypeScriptEmitter,
  TypeScriptField,
  TypeScriptImportResolver,
  TypeScriptTypeMapper
}
import io.github.scalats.core.Internals.ListSet
import io.github.scalats.typescript._

final class PythonEmitterSpec extends org.specs2.mutable.Specification {
  "Python emitter".title

  import TranspilerResults._
  import TestCompat.{ ns, valueClassNs }

  "Emitter" should {
    "emit empty interface" in {
      val empty = InterfaceDeclaration(
        "Empty",
        ListSet.empty,
        typeParams = List.empty,
        superInterface = None,
        union = false
      )

      emit(ListSet(empty)) must beTypedEqualTo("""# Declare interface Empty
@dataclass
class Empty:
  pass
""")
    }

    "emit interface for a class with one primitive member" in {
      emit(ListSet(interface1)) must beTypedEqualTo(
        s"""# Declare interface ${ns}TestClass1
@dataclass
class ${ns}TestClass1:
  name: str
"""
      )
    }

    "emit interface for a class with generic member" in {
      emit(ListSet(interface2)) must beTypedEqualTo(
        s"""# Declare interface ${ns}TestClass2

T = typing.TypeVar('T')


@dataclass
class ${ns}TestClass2(typing.Generic[T]):
  name: T
"""
      )
    }

    "emit interface for a class with generic array" in {
      emit(ListSet(interface3)) must beTypedEqualTo(
        s"""# Declare interface ${ns}TestClass3

T = typing.TypeVar('T')


@dataclass
class ${ns}TestClass3(typing.Generic[T]):
  name: typing.List[T]
"""
      )
    }

    "emit interface for a generic case class with a optional member" in {
      emit(ListSet(interface5)) must beTypedEqualTo(
        s"""# Declare interface ${ns}TestClass5

T = typing.TypeVar('T')


@dataclass
class ${ns}TestClass5(typing.Generic[T]):
  name: typing.Optional[T]
  counters: typing.Dict[str, complex]
"""
      )
    }

    "emit interface for a generic case class with disjunction" in {
      // TODO: Add example to documentation
      emit(ListSet(interface7)) must beTypedEqualTo(
        s"""# Declare interface ${ns}TestClass7

T = typing.TypeVar('T')


@dataclass
class ${ns}TestClass7(typing.Generic[T]):
  name: typing.Union[${ns}TestClass1, ${ns}TestClass1B]
"""
      )
    }

    "emit tagged type" in {
      "as NewType" in {
        emit(ListSet(taggedDeclaration1)) must beTypedEqualTo(
          s"""# Declare tagged type ${valueClassNs}AnyValChild
${valueClassNs}AnyValChild = typing.NewType('${valueClassNs}AnyValChild', str)
"""
        )
      }

      "as member" in {
        emit(ListSet(interface8)) must beTypedEqualTo(
          s"""# Declare interface ${valueClassNs}TestClass8
@dataclass
class ${valueClassNs}TestClass8:
  name: ${valueClassNs}AnyValChild
  aliases: typing.List[${valueClassNs}AnyValChild]
"""
        )
      }
    }

    "for singleton" >> {
      "emit class #1" >> {
        "as literal when super interface but no value" in {
          val singleton = SingletonDeclaration(
            name = singleton1.name,
            values = singleton1.values,
            superInterface = Some(interface1)
          )

          emit(ListSet(singleton)) must beTypedEqualTo(
            s"""# Declare singleton ${ns}TestObject1
${ns}TestObject1 = typing.Literal['${ns}TestObject1']
${ns}TestObject1Inhabitant: ${ns}TestObject1 = '${ns}TestObject1'
"""
          )
        }

        "as literal when super interface with a value" in {
          val singleton = SingletonDeclaration(
            name = singleton1.name,
            values = ListSet(
              LiteralValue("name", StringRef, "\"Foo\""),
              LiteralValue("i", NumberRef.int, "3"),
              LiteralValue("d", NumberRef.double, "4.56")
            ),
            superInterface = Some(interface1)
          )

          emit(ListSet(singleton)) must beTypedEqualTo(
            s"""# Declare singleton ${ns}TestObject1
${ns}TestObject1 = typing.Literal["Foo"]
${ns}TestObject1Inhabitant: ${ns}TestObject1 = "Foo"


class ${ns}TestObject1InvariantsFactory:
  @classmethod
  def name(self) -> str:
    return "Foo"

  @classmethod
  def i(self) -> int:
    return 3

  @classmethod
  def d(self) -> float:
    return 4.56


${ns}TestObject1Invariants = {
  'name': ${ns}TestObject1InvariantsFactory.name(),
  'i': ${ns}TestObject1InvariantsFactory.i(),
  'd': ${ns}TestObject1InvariantsFactory.d(),
}
"""
          )
        }

        "as empty" in {
          singleton1.superInterface must beNone and {
            singleton1.values must beEmpty
          } and {
            emit(ListSet(singleton1)) must beTypedEqualTo(
              s"""# Declare singleton ${ns}TestObject1
"""
            )
          }
        }
      }

      "emit class #2" >> {
        "with value class as literal" in {

          // SCALATS1: No implements SupI
          emit(
            ListSet(singleton2)
          ) must_=== s"""# Declare singleton ${ns}TestObject2
${ns}TestObject2 = typing.Literal["Foo \\"bar\\""]
${ns}TestObject2Inhabitant: ${ns}TestObject2 = "Foo \\"bar\\""


class ${ns}TestObject2InvariantsFactory:
  @classmethod
  def name(self) -> str:
    return "Foo \\"bar\\""

  @classmethod
  def code(self) -> int:
    return 1

  @classmethod
  def const(self) -> str:
    return "value"

  @classmethod
  def foo(self) -> str:
    return self.name()

  @classmethod
  def list(self) -> typing.List[str]:
    return ["first", self.name()]

  @classmethod
  def set(self) -> typing.List[int]:
    return {self.code(), 2}

  @classmethod
  def mapping(self) -> typing.Dict[str, str]:
    return {"foo": "bar", "lorem": self.name()}

  @classmethod
  def dictOfList(self) -> typing.Dict[str, typing.List[str]]:
    return {"excludes": ["*.txt", ".gitignore"], "includes": ["images/**", "*.jpg", "*.png"]}

  @classmethod
  def concatSeq(self) -> typing.List[str]:
    return self.list() + ["foo", "bar"] + ["lorem"]

  @classmethod
  def concatList(self) -> typing.List[str]:
    return ["foo"] + self.list()

  @classmethod
  def mergedSet(self) -> typing.List[int]:
    return self.set().union({3})


${ns}TestObject2Invariants = {
  'name': ${ns}TestObject2InvariantsFactory.name(),
  'code': ${ns}TestObject2InvariantsFactory.code(),
  'const': ${ns}TestObject2InvariantsFactory.const(),
  'foo': ${ns}TestObject2InvariantsFactory.foo(),
  'list': ${ns}TestObject2InvariantsFactory.list(),
  'set': ${ns}TestObject2InvariantsFactory.set(),
  'mapping': ${ns}TestObject2InvariantsFactory.mapping(),
  'dictOfList': ${ns}TestObject2InvariantsFactory.dictOfList(),
  'concatSeq': ${ns}TestObject2InvariantsFactory.concatSeq(),
  'concatList': ${ns}TestObject2InvariantsFactory.concatList(),
  'mergedSet': ${ns}TestObject2InvariantsFactory.mergedSet(),
}
"""
        }

        val taggedRef =
          TaggedRef(s"${valueClassNs}AnyValChild", StringRef)

        "with complex values" in {
          val singleton = SingletonDeclaration(
            "Singleton",
            ListSet(
              LiteralValue("name", taggedRef, "\"Foo\""),
              LiteralValue("code", NumberRef.int, "1"),
              LiteralValue("const", StringRef, "\"value\""),
              SelectValue("foo", taggedRef, ThisTypeRef, "name"),
              ListValue(
                name = "list",
                typeRef = ArrayRef(StringRef),
                valueTypeRef = StringRef,
                elements = List(
                  LiteralValue("list[0]", taggedRef, "\"first\""),
                  SelectValue("list[1]", taggedRef, ThisTypeRef, "name")
                )
              ),
              SetValue(
                name = "set",
                typeRef = ArrayRef(NumberRef.int),
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
                  LiteralValue(
                    "mapping.0",
                    StringRef,
                    "\"foo\""
                  ) -> LiteralValue("mapping[0]", taggedRef, "\"bar\""),
                  LiteralValue(
                    "mapping.1",
                    StringRef,
                    "\"lorem\""
                  ) -> SelectValue(
                    "mapping[1]",
                    taggedRef,
                    ThisTypeRef,
                    "name"
                  )
                )
              ),
              DictionaryValue(
                name = "dictOfList",
                keyTypeRef = StringRef,
                valueTypeRef = ArrayRef(taggedRef),
                entries = Map(
                  LiteralValue(
                    "dictOfList.0",
                    StringRef,
                    "\"excludes\""
                  ) -> ListValue(
                    name = "dictOfList[0]",
                    typeRef = ArrayRef(taggedRef),
                    valueTypeRef = taggedRef,
                    elements = List(
                      LiteralValue(
                        "dictOfList[0][0]",
                        taggedRef,
                        "\"*.txt\""
                      ),
                      LiteralValue(
                        "dictOfList[0][1]",
                        taggedRef,
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
                    typeRef = ArrayRef(taggedRef),
                    valueTypeRef = taggedRef,
                    elements = List(
                      LiteralValue(
                        "dictOfList[1][0]",
                        taggedRef,
                        "\"images/**\""
                      ),
                      LiteralValue(
                        "dictOfList[1][1]",
                        taggedRef,
                        "\"*.jpg\""
                      ),
                      LiteralValue(
                        "dictOfList[includes][2]",
                        taggedRef,
                        "\"*.png\""
                      )
                    )
                  )
                )
              ),
              MergedListsValue(
                name = "concatSeq",
                valueTypeRef = taggedRef,
                children = List(
                  SelectValue(
                    name = "concatSeq[0]",
                    typeRef = ArrayRef(taggedRef),
                    qualifier = ThisTypeRef,
                    term = "list"
                  ),
                  ListValue(
                    name = "concatSeq[1]",
                    typeRef = ArrayRef(taggedRef),
                    valueTypeRef = taggedRef,
                    elements = List(
                      LiteralValue("concatSeq[1][0]", taggedRef, "\"foo\""),
                      LiteralValue("concatSeq[1][1]", taggedRef, "\"bar\"")
                    )
                  ),
                  ListValue(
                    name = "concatSeq[2]",
                    typeRef = ArrayRef(taggedRef),
                    valueTypeRef = taggedRef,
                    elements = List(
                      LiteralValue("concatSeq[2][0]", taggedRef, "\"lorem\"")
                    )
                  )
                )
              ),
              MergedSetsValue(
                name = "mergedSet",
                valueTypeRef = NumberRef.int,
                children = List(
                  SelectValue(
                    name = "mergedSet[0]",
                    typeRef = ArrayRef(NumberRef.int),
                    qualifier = ThisTypeRef,
                    term = "set"
                  ),
                  SetValue(
                    name = "mergedSet[1]",
                    typeRef = ArrayRef(NumberRef.int),
                    valueTypeRef = NumberRef.int,
                    elements =
                      Set(LiteralValue("mergedSet[1][0]", NumberRef.int, "3"))
                  )
                )
              ),
              DictionaryValue(
                name = "taggedDict",
                keyTypeRef = taggedRef,
                valueTypeRef = StringRef,
                entries = Map(
                  LiteralValue(
                    "taggedDict.0",
                    taggedRef,
                    "\"foo\""
                  ) -> LiteralValue("taggedDict[0]", taggedRef, "\"bar\"")
                )
              )
            ),
            superInterface = None
          )

          emit(
            ListSet(singleton),
            config = Settings(
              typeNaming = CustomTypeNaming,
              fieldMapper = CustomFieldMapper
            )
          ) must_=== s"""# Declare singleton TSSingleton
class TSSingletonInvariantsFactory:
  @classmethod
  def _name(self) -> TS${valueClassNs}AnyValChild:
    return TS${valueClassNs}AnyValChild("Foo")

  @classmethod
  def _code(self) -> int:
    return 1

  @classmethod
  def _const(self) -> str:
    return "value"

  @classmethod
  def _foo(self) -> TS${valueClassNs}AnyValChild:
    return self._name()

  @classmethod
  def _list(self) -> typing.List[str]:
    return [TS${valueClassNs}AnyValChild("first"), self._name()]

  @classmethod
  def _set(self) -> typing.List[int]:
    return {self._code(), 2}

  @classmethod
  def _mapping(self) -> typing.Dict[str, str]:
    return {"foo": TS${valueClassNs}AnyValChild("bar"), "lorem": self._name()}

  @classmethod
  def _dictOfList(self) -> typing.Dict[str, typing.List[TS${valueClassNs}AnyValChild]]:
    return {"excludes": [TS${valueClassNs}AnyValChild("*.txt"), TS${valueClassNs}AnyValChild(".gitignore")], "includes": [TS${valueClassNs}AnyValChild("images/**"), TS${valueClassNs}AnyValChild("*.jpg"), TS${valueClassNs}AnyValChild("*.png")]}

  @classmethod
  def _concatSeq(self) -> typing.List[TS${valueClassNs}AnyValChild]:
    return self._list() + [TS${valueClassNs}AnyValChild("foo"), TS${valueClassNs}AnyValChild("bar")] + [TS${valueClassNs}AnyValChild("lorem")]

  @classmethod
  def _mergedSet(self) -> typing.List[int]:
    return self._set().union({3})

  @classmethod
  def _taggedDict(self) -> typing.Dict[TS${valueClassNs}AnyValChild, str]:
    return {TS${valueClassNs}AnyValChild("foo"): TS${valueClassNs}AnyValChild("bar")}


TSSingletonInvariants = {
  '_name': TSSingletonInvariantsFactory._name(),
  '_code': TSSingletonInvariantsFactory._code(),
  '_const': TSSingletonInvariantsFactory._const(),
  '_foo': TSSingletonInvariantsFactory._foo(),
  '_list': TSSingletonInvariantsFactory._list(),
  '_set': TSSingletonInvariantsFactory._set(),
  '_mapping': TSSingletonInvariantsFactory._mapping(),
  '_dictOfList': TSSingletonInvariantsFactory._dictOfList(),
  '_concatSeq': TSSingletonInvariantsFactory._concatSeq(),
  '_mergedSet': TSSingletonInvariantsFactory._mergedSet(),
  '_taggedDict': TSSingletonInvariantsFactory._taggedDict(),
}
"""
        }

        "with value class as tagged type" in {
          val singleton2WithTagged = SingletonDeclaration(
            s"${valueClassNs}TestObject2",
            ListSet(
              LiteralValue("name", StringRef, "\"Foo\""),
              LiteralValue(
                "const",
                taggedRef,
                "\"value\""
              ),
              SelectValue("foo", taggedRef, ThisTypeRef, "const"),
              LiteralValue("code", NumberRef.int, "1")
            ),
            superInterface = None
          )

          emit(
            ListSet(singleton2WithTagged)
          ) must_=== s"""# Declare singleton ${valueClassNs}TestObject2
class ${valueClassNs}TestObject2InvariantsFactory:
  @classmethod
  def name(self) -> str:
    return "Foo"

  @classmethod
  def const(self) -> ${valueClassNs}AnyValChild:
    return ${valueClassNs}AnyValChild("value")

  @classmethod
  def foo(self) -> ${valueClassNs}AnyValChild:
    return self.const()

  @classmethod
  def code(self) -> int:
    return 1


${valueClassNs}TestObject2Invariants = {
  'name': ${valueClassNs}TestObject2InvariantsFactory.name(),
  'const': ${valueClassNs}TestObject2InvariantsFactory.const(),
  'foo': ${valueClassNs}TestObject2InvariantsFactory.foo(),
  'code': ${valueClassNs}TestObject2InvariantsFactory.code(),
}
"""
        }
      }

      "emit class #3" in {
        emit(
          ListSet(unionMember2Singleton)
        ) must_=== s"""# Declare singleton ${ns}FamilyMember2
${ns}FamilyMember2 = typing.Literal["bar"]
${ns}FamilyMember2Inhabitant: ${ns}FamilyMember2 = "bar"


class ${ns}FamilyMember2InvariantsFactory:
  @classmethod
  def foo(self) -> str:
    return "bar"


${ns}FamilyMember2Invariants = {
  'foo': ${ns}FamilyMember2InvariantsFactory.foo(),
}
"""
      }

      "emit literal types" >> {
        val barVal =
          LiteralValue(
            name = "bar",
            typeRef = StringRef,
            rawValue = "\"lorem\""
          )

        "using singleton name" in {
          val obj = SingletonDeclaration(
            name = "Foo",
            values = ListSet.empty,
            superInterface = Option.empty
          )

          emit(ListSet(obj)) must_=== """# Declare singleton Foo
"""
        }

        "with string value" in {
          val obj = SingletonDeclaration(
            name = "Foo",
            values = ListSet(barVal),
            superInterface = Option.empty
          )

          emit(ListSet(obj)) must_=== """# Declare singleton Foo
class FooInvariantsFactory:
  @classmethod
  def bar(self) -> str:
    return "lorem"


FooInvariants = {
  'bar': FooInvariantsFactory.bar(),
}
"""
        }

        "with object value" in {
          val obj = SingletonDeclaration(
            name = "Foo",
            values = ListSet(
              barVal,
              LiteralValue(
                name = "ipsum",
                typeRef = NumberRef.int,
                rawValue = "2"
              )
            ),
            superInterface = Option.empty
          )

          emit(ListSet(obj)) must_=== """# Declare singleton Foo
class FooInvariantsFactory:
  @classmethod
  def bar(self) -> str:
    return "lorem"

  @classmethod
  def ipsum(self) -> int:
    return 2


FooInvariants = {
  'bar': FooInvariantsFactory.bar(),
  'ipsum': FooInvariantsFactory.ipsum(),
}
"""
        }
      }
    }

    "emit union" in {
      emit(
        ListSet(union1)
      ) must_=== s"""# Declare union ${ns}Family
${ns}Family = typing.Union[${ns}FamilyMember1, ${ns}FamilyMember2, ${ns}FamilyMember3]


class ${ns}FamilyCompanion:
  @classmethod
  def ${ns}FamilyMember2(self) -> ${ns}Family:
    return ${ns.toLowerCase}familymember2.${ns}FamilyMember2Inhabitant

  @classmethod
  def ${ns}FamilyMember3(self) -> ${ns}Family:
    return ${ns.toLowerCase}familymember3.${ns}FamilyMember3Inhabitant

# Fields are ignored: foo
"""
    }

    "emit enumeration as union" in {
      emit(ListSet(enum1)) must beTypedEqualTo(
        s"""# Declare enum ${ns}TestEnumeration
from enum import Enum


class ${ns}TestEnumeration(Enum):
  A = 'A'
  B = 'B'
  C = 'C'
"""
      )
    }
  }

  // ---

  private val pyDeclMapper = new PythonDeclarationMapper

  private val pyTypeMapper = new PythonTypeMapper

  def emit(
      decls: ListSet[Declaration],
      config: Settings = Settings(),
      declMapper: TypeScriptDeclarationMapper = pyDeclMapper,
      importResolver: TypeScriptImportResolver =
        TypeScriptImportResolver.Defaults,
      typeMapper: TypeScriptTypeMapper = pyTypeMapper
    ): String = {
    val buf = new java.io.ByteArrayOutputStream()
    lazy val out = new java.io.PrintStream(buf)

    val emiter = new TypeScriptEmitter(
      config,
      (_, _, _, _) => out,
      importResolver,
      declMapper,
      typeMapper
    )

    try {
      emiter.emit(decls)
      out.flush()
      buf.toString
    } finally {
      out.close()
    }
  }
}

object CustomTypeNaming extends io.github.scalats.core.TypeScriptTypeNaming {

  def apply(settings: Settings, tpe: TypeRef) = {
    if (tpe.name == "this") {
      "this"
    } else {
      s"TS${tpe.name}"
    }
  }
}

object CustomFieldMapper extends io.github.scalats.core.TypeScriptFieldMapper {

  def apply(
      settings: Settings,
      ownerType: String,
      propertyName: String,
      propertyType: io.github.scalats.typescript.TypeRef
    ) =
    TypeScriptField(s"_${propertyName}", scala.collection.immutable.Set.empty)
}
