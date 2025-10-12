package io.github.scalats.python

import io.github.scalats.ast._
import io.github.scalats.core.{
  DeclarationMapper,
  Emitter,
  Field,
  ImportResolver,
  Settings,
  TranspilerResults,
  TypeMapper
}
import io.github.scalats.core.Internals.ListSet

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

      emit(Map("Empty" -> ListSet(empty))) must beTypedEqualTo(
        """# Declare interface Empty
@dataclass
class Empty:
  pass
"""
      )
    }

    "emit interface for a class with one primitive member" in {
      emit(Map(interface1.name -> ListSet(interface1))) must beTypedEqualTo(
        s"""# Declare interface ${ns}TestClass1
@dataclass
class ${ns}TestClass1:
  name: str
"""
      )
    }

    "emit interface for a class with generic member" in {
      emit(Map(interface2.name -> ListSet(interface2))) must beTypedEqualTo(
        s"""# Declare interface ${ns}TestClass2

T = typing.TypeVar('T')


@dataclass
class ${ns}TestClass2(typing.Generic[T]):
  name: T
"""
      )
    }

    "emit interface for a class with generic array" in {
      emit(Map(interface3.name -> ListSet(interface3))) must beTypedEqualTo(
        s"""# Declare interface ${ns}TestClass3

T = typing.TypeVar('T')


@dataclass
class ${ns}TestClass3(typing.Generic[T]):
  name: typing.List[T]
"""
      )
    }

    "emit interface for a generic case class with a optional member" in {
      emit(Map(interface5.name -> ListSet(interface5))) must beTypedEqualTo(
        s"""# Declare interface ${ns}TestClass5

T = typing.TypeVar('T')


@dataclass
class ${ns}TestClass5(typing.Generic[T]):
  name: typing.Optional[T]
  counters: typing.Dict[str, complex]
  time: time.struct_time
"""
      )
    }

    "emit interface for a generic case class with disjunction" in {
      // TODO: Add example to documentation
      emit(Map(interface7.name -> ListSet(interface7))) must beTypedEqualTo(
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
        emit(
          Map(taggedDeclaration1.name -> ListSet(taggedDeclaration1))
        ) must beTypedEqualTo(
          s"""# Declare tagged type ${valueClassNs}AnyValChild
${valueClassNs}AnyValChild = typing.NewType('${valueClassNs}AnyValChild', str)
"""
        )
      }

      "as member" in {
        emit(Map(interface8.name -> ListSet(interface8))) must beTypedEqualTo(
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

          emit(Map(singleton.name -> ListSet(singleton))) must beTypedEqualTo(
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

          emit(Map(singleton.name -> ListSet(singleton))) must beTypedEqualTo(
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


@dataclass
class I${ns}TestObject1Invariants:
  name: str
  i: int
  d: float


${ns}TestObject1Invariants = I${ns}TestObject1Invariants(
  name=${ns}TestObject1InvariantsFactory.name(),
  i=${ns}TestObject1InvariantsFactory.i(),
  d=${ns}TestObject1InvariantsFactory.d(),
)
"""
          )
        }

        "as empty" in {
          singleton1.superInterface must beNone and {
            singleton1.values must beEmpty
          } and {
            emit(
              Map(singleton1.name -> ListSet(singleton1))
            ) must beTypedEqualTo(
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
            Map(singleton2.name -> ListSet(singleton2))
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
  def set(self) -> typing.Set[int]:
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

  @classmethod
  def tuple1(self) -> typing.Tuple[str, int, float]:
    return ("foo", 2, 3.0)

  @classmethod
  def tuple2(self) -> typing.Tuple[str, int]:
    return ("bar", 2)

  @classmethod
  def tuple3(self) -> typing.Tuple[str, int, float]:
    return self.tuple1()

  @classmethod
  def tuple4(self) -> typing.Tuple[str, int, int]:
    return ("lorem", 10, 20)

  @classmethod
  def Nested1(self) -> ${ns}TestObject2Nested1:
    return ${ns.toLowerCase}testobject2nested1.${ns}TestObject2Nested1Inhabitant


@dataclass
class I${ns}TestObject2Invariants:
  name: str
  code: int
  const: str
  foo: str
  list: typing.List[str]
  set: typing.Set[int]
  mapping: typing.Dict[str, str]
  dictOfList: typing.Dict[str, typing.List[str]]
  concatSeq: typing.List[str]
  concatList: typing.List[str]
  mergedSet: typing.List[int]
  tuple1: typing.Tuple[str, int, float]
  tuple2: typing.Tuple[str, int]
  tuple3: typing.Tuple[str, int, float]
  tuple4: typing.Tuple[str, int, int]
  Nested1: ${ns}TestObject2Nested1


${ns}TestObject2Invariants = I${ns}TestObject2Invariants(
  name=${ns}TestObject2InvariantsFactory.name(),
  code=${ns}TestObject2InvariantsFactory.code(),
  const=${ns}TestObject2InvariantsFactory.const(),
  foo=${ns}TestObject2InvariantsFactory.foo(),
  list=${ns}TestObject2InvariantsFactory.list(),
  set=${ns}TestObject2InvariantsFactory.set(),
  mapping=${ns}TestObject2InvariantsFactory.mapping(),
  dictOfList=${ns}TestObject2InvariantsFactory.dictOfList(),
  concatSeq=${ns}TestObject2InvariantsFactory.concatSeq(),
  concatList=${ns}TestObject2InvariantsFactory.concatList(),
  mergedSet=${ns}TestObject2InvariantsFactory.mergedSet(),
  tuple1=${ns}TestObject2InvariantsFactory.tuple1(),
  tuple2=${ns}TestObject2InvariantsFactory.tuple2(),
  tuple3=${ns}TestObject2InvariantsFactory.tuple3(),
  tuple4=${ns}TestObject2InvariantsFactory.tuple4(),
  Nested1=${ns}TestObject2InvariantsFactory.Nested1(),
)
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
            Map(singleton.name -> ListSet(singleton)),
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


@dataclass
class ITSSingletonInvariants:
  _name: TS${valueClassNs}AnyValChild
  _code: int
  _const: str
  _foo: TS${valueClassNs}AnyValChild
  _list: typing.List[str]
  _set: typing.List[int]
  _mapping: typing.Dict[str, str]
  _dictOfList: typing.Dict[str, typing.List[TS${valueClassNs}AnyValChild]]
  _concatSeq: typing.List[TS${valueClassNs}AnyValChild]
  _mergedSet: typing.List[int]
  _taggedDict: typing.Dict[TS${valueClassNs}AnyValChild, str]


TSSingletonInvariants = ITSSingletonInvariants(
  _name=TSSingletonInvariantsFactory._name(),
  _code=TSSingletonInvariantsFactory._code(),
  _const=TSSingletonInvariantsFactory._const(),
  _foo=TSSingletonInvariantsFactory._foo(),
  _list=TSSingletonInvariantsFactory._list(),
  _set=TSSingletonInvariantsFactory._set(),
  _mapping=TSSingletonInvariantsFactory._mapping(),
  _dictOfList=TSSingletonInvariantsFactory._dictOfList(),
  _concatSeq=TSSingletonInvariantsFactory._concatSeq(),
  _mergedSet=TSSingletonInvariantsFactory._mergedSet(),
  _taggedDict=TSSingletonInvariantsFactory._taggedDict(),
)
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
            Map(singleton2WithTagged.name -> ListSet(singleton2WithTagged))
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


@dataclass
class I${valueClassNs}TestObject2Invariants:
  name: str
  const: ${valueClassNs}AnyValChild
  foo: ${valueClassNs}AnyValChild
  code: int


${valueClassNs}TestObject2Invariants = I${valueClassNs}TestObject2Invariants(
  name=${valueClassNs}TestObject2InvariantsFactory.name(),
  const=${valueClassNs}TestObject2InvariantsFactory.const(),
  foo=${valueClassNs}TestObject2InvariantsFactory.foo(),
  code=${valueClassNs}TestObject2InvariantsFactory.code(),
)
"""
        }
      }

      "emit class #3" in {
        emit(
          Map(unionMember2Singleton.name -> ListSet(unionMember2Singleton))
        ) must_=== s"""# Declare singleton ${ns}FamilyMember2
${ns}FamilyMember2 = typing.Literal["bar"]
${ns}FamilyMember2Inhabitant: ${ns}FamilyMember2 = "bar"


class ${ns}FamilyMember2InvariantsFactory:
  @classmethod
  def foo(self) -> str:
    return "bar"


@dataclass
class I${ns}FamilyMember2Invariants:
  foo: str


${ns}FamilyMember2Invariants = I${ns}FamilyMember2Invariants(
  foo=${ns}FamilyMember2InvariantsFactory.foo(),
)
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

          emit(
            Map(obj.name -> ListSet(obj))
          ) must_=== """# Declare singleton Foo
"""
        }

        "with string value" in {
          val obj = SingletonDeclaration(
            name = "Foo",
            values = ListSet(barVal),
            superInterface = Option.empty
          )

          emit(
            Map(obj.name -> ListSet(obj))
          ) must_=== """# Declare singleton Foo
class FooInvariantsFactory:
  @classmethod
  def bar(self) -> str:
    return "lorem"


@dataclass
class IFooInvariants:
  bar: str


FooInvariants = IFooInvariants(
  bar=FooInvariantsFactory.bar(),
)
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

          emit(
            Map(obj.name -> ListSet(obj))
          ) must_=== """# Declare singleton Foo
class FooInvariantsFactory:
  @classmethod
  def bar(self) -> str:
    return "lorem"

  @classmethod
  def ipsum(self) -> int:
    return 2


@dataclass
class IFooInvariants:
  bar: str
  ipsum: int


FooInvariants = IFooInvariants(
  bar=FooInvariantsFactory.bar(),
  ipsum=FooInvariantsFactory.ipsum(),
)
"""
        }
      }
    }

    "emit union" in {
      emit(
        Map(union1.name -> ListSet(union1))
      ) must_=== s"""# Declare union ${ns}Family
${ns}Family = typing.Union[${ns}FamilyMember1, ${ns}FamilyMember2, ${ns}FamilyMember3]


class ${ns}FamilyCompanion:
  @classmethod
  def ${ns}FamilyMember2(self) -> ${ns}Family:
    return ${ns.toLowerCase}familymember2.${ns}FamilyMember2Inhabitant

  @classmethod
  def ${ns}FamilyMember3(self) -> ${ns}Family:
    return ${ns.toLowerCase}familymember3.${ns}FamilyMember3Inhabitant


${ns}FamilyKnownValues: typing.List[${ns}Family] = [
  ${ns}FamilyCompanion.${ns}FamilyMember2(),
  ${ns}FamilyCompanion.${ns}FamilyMember3(),
]

# Fields are ignored: foo
"""
    }

    "emit enumeration as union" in {
      emit(Map(enum1.name -> ListSet(enum1))) must beTypedEqualTo(
        s"""# Declare enum ${ns}TestEnumeration
from enum import Enum


class ${ns}TestEnumeration(Enum):
  A = 'A'
  B = 'B'
  C = 'C'
"""
      )
    }

    "emit composite" >> {
      "with empty object" in {
        emit(
          Map("Test" -> ListSet(interface1, singleton1))
        ) must_=== s"""# Declare interface ${ns}TestClass1
@dataclass
class ${ns}TestClass1:
  name: str
"""
      }

      "with non-empty object" in {
        val singleton = SingletonDeclaration(
          name = singleton1.name,
          values = ListSet(
            LiteralValue("name", StringRef, "\"Foo\""),
            LiteralValue("i", NumberRef.int, "3"),
            LiteralValue("d", NumberRef.double, "4.56")
          ),
          superInterface = None
        )

        emit(
          Map("Test" -> ListSet(interface1, singleton))
        ) must_=== s"""# Declare composite type Test

# Declare interface I${ns}TestClass1
@dataclass
class I${ns}TestClass1:
  name: str


# Declare singleton ${ns}TestObject1Singleton
class ${ns}TestObject1SingletonInvariantsFactory:
  @classmethod
  def name(self) -> str:
    return "Foo"

  @classmethod
  def i(self) -> int:
    return 3

  @classmethod
  def d(self) -> float:
    return 4.56


@dataclass
class I${ns}TestObject1SingletonInvariants:
  name: str
  i: int
  d: float


${ns}TestObject1SingletonInvariants = I${ns}TestObject1SingletonInvariants(
  name=${ns}TestObject1SingletonInvariantsFactory.name(),
  i=${ns}TestObject1SingletonInvariantsFactory.i(),
  d=${ns}TestObject1SingletonInvariantsFactory.d(),
)

Test = I${ns}TestClass1
"""
      }

      "with union" in {
        val singleton = SingletonDeclaration(
          name = singleton1.name,
          values = ListSet(
            LiteralValue("name", StringRef, "\"Foo\""),
            LiteralValue("i", NumberRef.int, "3"),
            LiteralValue("d", NumberRef.double, "4.56")
          ),
          superInterface = None
        )

        emit(
          Map("Test" -> ListSet(singleton, union1))
        ) must_=== s"""# Declare composite type Test

# Declare union ${ns}FamilyUnion
${ns}FamilyUnion = typing.Union[${ns}FamilyMember1, ${ns}FamilyMember2, ${ns}FamilyMember3]


class ${ns}FamilyUnionCompanion:
  @classmethod
  def ${ns}FamilyMember2(self) -> ${ns}FamilyUnion:
    return ${ns.toLowerCase}familymember2.${ns}FamilyMember2Inhabitant

  @classmethod
  def ${ns}FamilyMember3(self) -> ${ns}FamilyUnion:
    return ${ns.toLowerCase}familymember3.${ns}FamilyMember3Inhabitant


${ns}FamilyUnionKnownValues: typing.List[${ns}FamilyUnion] = [
  ${ns}FamilyUnionCompanion.${ns}FamilyMember2(),
  ${ns}FamilyUnionCompanion.${ns}FamilyMember3(),
]

# Fields are ignored: foo


# Declare singleton ${ns}TestObject1Singleton
class ${ns}TestObject1SingletonInvariantsFactory:
  @classmethod
  def name(self) -> str:
    return "Foo"

  @classmethod
  def i(self) -> int:
    return 3

  @classmethod
  def d(self) -> float:
    return 4.56


@dataclass
class I${ns}TestObject1SingletonInvariants:
  name: str
  i: int
  d: float


${ns}TestObject1SingletonInvariants = I${ns}TestObject1SingletonInvariants(
  name=${ns}TestObject1SingletonInvariantsFactory.name(),
  i=${ns}TestObject1SingletonInvariantsFactory.i(),
  d=${ns}TestObject1SingletonInvariantsFactory.d(),
)

Test = ${ns}FamilyUnion
TestCompanion = TestUnionCompanion
TestKnownValues: typing.List[Test] = TestUnionKnownValues
"""
      }
    }
  }

  // ---

  private val pyDeclMapper = new PythonDeclarationMapper

  private val pyTypeMapper = new PythonTypeMapper

  def emit(
      decls: Map[String, ListSet[Declaration]],
      config: Settings = Settings(),
      declMapper: DeclarationMapper = pyDeclMapper,
      importResolver: ImportResolver = ImportResolver.Defaults,
      typeMapper: TypeMapper = pyTypeMapper
    ): String = {
    val buf = new java.io.ByteArrayOutputStream()
    lazy val out = new java.io.PrintStream(buf)

    val emiter = new Emitter(
      config,
      (_, _, _, _, _) => out,
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

object CustomTypeNaming extends io.github.scalats.core.TypeNaming {

  def apply(settings: Settings, tpe: TypeRef) = {
    if (tpe.name == "this") {
      "this"
    } else {
      s"TS${tpe.name}"
    }
  }
}

object CustomFieldMapper extends io.github.scalats.core.FieldMapper {

  def apply(
      settings: Settings,
      ownerType: String,
      propertyName: String,
      propertyType: TypeRef
    ) =
    Field(s"_${propertyName}", scala.collection.immutable.Set.empty)
}
