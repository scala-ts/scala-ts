package io.github.scalats.idtlt

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

final class EmitterSpec extends org.specs2.mutable.Specification {
  "TypeScript emitter" title

  import TranspilerResults._

  "Emitter" should {
    "emit empty interface" in {
      val empty = InterfaceDeclaration(
        "Empty",
        ListSet.empty,
        typeParams = List.empty,
        superInterface = None,
        union = false
      )

      emit(ListSet(empty)) must beTypedEqualTo("""// Validator for InterfaceDeclaration Empty
export const idtltEmpty = idtlt.object({
});

// Deriving TypeScript type from Empty validator
export type Empty = typeof idtltEmpty.T;

export const idtltDiscriminatedEmpty = idtlt.intersection(
  idtltEmpty,
  idtlt.object({
    _type: idtlt.literal('Empty')
  })
);

// Deriving TypeScript type from idtltDiscriminatedEmpty validator
export type DiscriminatedEmpty = typeof idtltDiscriminatedEmpty.T;

export const discriminatedEmpty: (_: Empty) => DiscriminatedEmpty = (v: Empty) => ({ _type: 'Empty', ...v });

export function isEmpty(v: any): v is Empty {
  return (
    v === {}
  );
}""")
    }

    "emit interface for a class with one primitive member" in {
      emit(ListSet(interface1)) must beTypedEqualTo(
        """// Validator for InterfaceDeclaration ScalaRuntimeFixturesTestClass1
export const idtltScalaRuntimeFixturesTestClass1 = idtlt.object({
  name: idtlt.string,
});

// Deriving TypeScript type from ScalaRuntimeFixturesTestClass1 validator
export type ScalaRuntimeFixturesTestClass1 = typeof idtltScalaRuntimeFixturesTestClass1.T;

export const idtltDiscriminatedScalaRuntimeFixturesTestClass1 = idtlt.intersection(
  idtltScalaRuntimeFixturesTestClass1,
  idtlt.object({
    _type: idtlt.literal('ScalaRuntimeFixturesTestClass1')
  })
);

// Deriving TypeScript type from idtltDiscriminatedScalaRuntimeFixturesTestClass1 validator
export type DiscriminatedScalaRuntimeFixturesTestClass1 = typeof idtltDiscriminatedScalaRuntimeFixturesTestClass1.T;

export const discriminatedScalaRuntimeFixturesTestClass1: (_: ScalaRuntimeFixturesTestClass1) => DiscriminatedScalaRuntimeFixturesTestClass1 = (v: ScalaRuntimeFixturesTestClass1) => ({ _type: 'ScalaRuntimeFixturesTestClass1', ...v });

export function isScalaRuntimeFixturesTestClass1(v: any): v is ScalaRuntimeFixturesTestClass1 {
  return (
    ((typeof v['name']) === 'string')
  );
}"""
      )
    }

    "emit interface for a class with generic member" in {
      emit(ListSet(interface2)) must beTypedEqualTo(
        """// Not supported: InterfaceDeclaration 'ScalaRuntimeFixturesTestClass2'
// - type parameters: T

export function isScalaRuntimeFixturesTestClass2(v: any): boolean {
  return v && false;
}
"""
      )
    }

    "emit interface for a class with generic array" in {
      emit(ListSet(interface3)) must beTypedEqualTo(
        """// Not supported: InterfaceDeclaration 'ScalaRuntimeFixturesTestClass3'
// - type parameters: T

export function isScalaRuntimeFixturesTestClass3(v: any): boolean {
  return v && false;
}
"""
      )
    }

    "emit interface for a generic case class with a optional member" in {
      emit(ListSet(interface5)) must beTypedEqualTo(
        """// Not supported: InterfaceDeclaration 'ScalaRuntimeFixturesTestClass5'
// - type parameters: T

export function isScalaRuntimeFixturesTestClass5(v: any): boolean {
  return v && false;
}
"""
      )
    }

    "emit interface for a generic case class with disjunction" in {
      // TODO: Add example to documentation
      emit(ListSet(interface7)) must beTypedEqualTo(
        """// Not supported: InterfaceDeclaration 'ScalaRuntimeFixturesTestClass7'
// - type parameters: T

export function isScalaRuntimeFixturesTestClass7(v: any): boolean {
  return v && false;
}
"""
      )
    }

    "emit tagged type" >> {
      "as type alias" in {
        emit(ListSet(taggedDeclaration1)) must beTypedEqualTo(
          """// Validator for TaggedDeclaration ScalaRuntimeFixturesAnyValChild
export type ScalaRuntimeFixturesAnyValChild = string & { __tag: 'ScalaRuntimeFixturesAnyValChild' };

export function ScalaRuntimeFixturesAnyValChild(value: string): ScalaRuntimeFixturesAnyValChild {
  return value as ScalaRuntimeFixturesAnyValChild;
}

export const idtltScalaRuntimeFixturesAnyValChild = idtlt.string.tagged<ScalaRuntimeFixturesAnyValChild>();

export function isScalaRuntimeFixturesAnyValChild(v: any): v is ScalaRuntimeFixturesAnyValChild {
  return idtltScalaRuntimeFixturesAnyValChild.validate(v).ok;
}
"""
        )
      }

      "as tagged type" in {
        emit(ListSet(taggedDeclaration1)) must beTypedEqualTo(
          """// Validator for TaggedDeclaration ScalaRuntimeFixturesAnyValChild
export type ScalaRuntimeFixturesAnyValChild = string & { __tag: 'ScalaRuntimeFixturesAnyValChild' };

export function ScalaRuntimeFixturesAnyValChild(value: string): ScalaRuntimeFixturesAnyValChild {
  return value as ScalaRuntimeFixturesAnyValChild;
}

export const idtltScalaRuntimeFixturesAnyValChild = idtlt.string.tagged<ScalaRuntimeFixturesAnyValChild>();

export function isScalaRuntimeFixturesAnyValChild(v: any): v is ScalaRuntimeFixturesAnyValChild {
  return idtltScalaRuntimeFixturesAnyValChild.validate(v).ok;
}
"""
        )
      }

      "as member" in {
        emit(ListSet(interface8)) must beTypedEqualTo("""// Validator for InterfaceDeclaration ScalaRuntimeFixturesTestClass8
export const idtltScalaRuntimeFixturesTestClass8 = idtlt.object({
  name: nsScalaRuntimeFixturesAnyValChild.idtltScalaRuntimeFixturesAnyValChild,
  aliases: idtlt.array(nsScalaRuntimeFixturesAnyValChild.idtltScalaRuntimeFixturesAnyValChild),
});

// Deriving TypeScript type from ScalaRuntimeFixturesTestClass8 validator
export type ScalaRuntimeFixturesTestClass8 = typeof idtltScalaRuntimeFixturesTestClass8.T;

export const idtltDiscriminatedScalaRuntimeFixturesTestClass8 = idtlt.intersection(
  idtltScalaRuntimeFixturesTestClass8,
  idtlt.object({
    _type: idtlt.literal('ScalaRuntimeFixturesTestClass8')
  })
);

// Deriving TypeScript type from idtltDiscriminatedScalaRuntimeFixturesTestClass8 validator
export type DiscriminatedScalaRuntimeFixturesTestClass8 = typeof idtltDiscriminatedScalaRuntimeFixturesTestClass8.T;

export const discriminatedScalaRuntimeFixturesTestClass8: (_: ScalaRuntimeFixturesTestClass8) => DiscriminatedScalaRuntimeFixturesTestClass8 = (v: ScalaRuntimeFixturesTestClass8) => ({ _type: 'ScalaRuntimeFixturesTestClass8', ...v });

export function isScalaRuntimeFixturesTestClass8(v: any): v is ScalaRuntimeFixturesTestClass8 {
  return (
    (v['name'] && nsScalaRuntimeFixturesAnyValChild.isScalaRuntimeFixturesAnyValChild(v['name'])) &&
    (Array.isArray(v['aliases']) && v['aliases'].every(elmt => elmt && nsScalaRuntimeFixturesAnyValChild.isScalaRuntimeFixturesAnyValChild(elmt)))
  );
}""")
      }
    }

    "for singleton" >> {
      "emit class #1" in {
        emit(ListSet(singleton1)) must beTypedEqualTo("""export class ScalaRuntimeFixturesTestObject1 {
  private static instance: ScalaRuntimeFixturesTestObject1;

  private constructor() {}

  public static getInstance() {
    if (!ScalaRuntimeFixturesTestObject1.instance) {
      ScalaRuntimeFixturesTestObject1.instance = new ScalaRuntimeFixturesTestObject1();
    }

    return ScalaRuntimeFixturesTestObject1.instance;
  }
}

export const ScalaRuntimeFixturesTestObject1Inhabitant: ScalaRuntimeFixturesTestObject1 = ScalaRuntimeFixturesTestObject1.getInstance();

export function isScalaRuntimeFixturesTestObject1(v: any): v is ScalaRuntimeFixturesTestObject1 {
  return (v instanceof ScalaRuntimeFixturesTestObject1) && (v === ScalaRuntimeFixturesTestObject1Inhabitant);
}

export const idtltScalaRuntimeFixturesTestObject1 =
  idtlt.unknown.and(_unknown => idtlt.Err(
    'Cannot validator instance for singleton ScalaRuntimeFixturesTestObject1'));
""")
      }

      "emit class #2" >> {
        "with value class as validator literal" in {
          // TODO: foo: idtlt.this.name,

          // SCALATS1: No implements SupI
          emit(
            ListSet(singleton2)
          ) must_=== """// Validator for SingletonDeclaration ScalaRuntimeFixturesTestObject2
export const idtltScalaRuntimeFixturesTestObject2 = idtlt.object({
  name: idtlt.literal("Foo"),
  code: idtlt.literal(1),
  const: idtlt.literal("value"),
  /* Unsupported 'SelectValue': foo */
  /* Unsupported 'ListValue': list */
  /* Unsupported 'SetValue': set */
  /* Unsupported 'DictionaryValue': mapping */
  /* Unsupported 'DictionaryValue': dictOfList */
  /* Unsupported 'MergedListsValue': concatSeq */
  /* Unsupported 'MergedListsValue': concatList */
  /* Unsupported 'MergedSetsValue': mergedSet */
});

// Super-type declaration SupI is ignored
export const idtltDiscriminatedScalaRuntimeFixturesTestObject2 = idtltScalaRuntimeFixturesTestObject2;

// Deriving TypeScript type from ScalaRuntimeFixturesTestObject2 validator
export type ScalaRuntimeFixturesTestObject2 = typeof idtltScalaRuntimeFixturesTestObject2.T;

export const ScalaRuntimeFixturesTestObject2Inhabitant: ScalaRuntimeFixturesTestObject2 = {
  name: "Foo",
  code: 1,
  const: "value",
  foo: this.name,
  list: [ "first", this.name ],
  set: new Set([ this.code, 2 ]),
  mapping: { "foo": "bar", "lorem": this.name },
  dictOfList: { "excludes": [ "*.txt", ".gitignore" ], "includes": [ "images/**", "*.jpg", "*.png" ] },
  concatSeq: [ ...this.list, ...[ "foo", "bar" ], ...[ "lorem" ]],
  concatList: [ ...[ "foo" ], ...this.list],
  mergedSet: new Set([ ...this.set, ...new Set([ 3 ]) ])
};

export function isScalaRuntimeFixturesTestObject2(v: any): v is ScalaRuntimeFixturesTestObject2 {
  return idtltScalaRuntimeFixturesTestObject2.validate(v).ok;
}"""
        }

        val taggedRef =
          TaggedRef("ScalaRuntimeFixturesAnyValChild", StringRef)

        "with complex values" in {
          val singleton = SingletonDeclaration(
            "Singleton",
            ListSet(
              LiteralValue("name", taggedRef, "\"Foo\""),
              LiteralValue("code", NumberRef, "1"),
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
                    elements =
                      Set(LiteralValue("mergedSet[1][0]", NumberRef, "3"))
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
          ) must_=== """export class TSSingleton {
  public _name: nsTSScalaRuntimeFixturesAnyValChild.TSScalaRuntimeFixturesAnyValChild = nsTSScalaRuntimeFixturesAnyValChild.TSScalaRuntimeFixturesAnyValChild("Foo");

  public _code: TSnumber = 1;

  public _const: TSstring = "value";

  public _foo: nsTSScalaRuntimeFixturesAnyValChild.TSScalaRuntimeFixturesAnyValChild = this._name;

  public _list: ReadonlyArray<TSstring> = [ nsTSScalaRuntimeFixturesAnyValChild.TSScalaRuntimeFixturesAnyValChild("first"), this._name ];

  public _set: ReadonlySet<TSnumber> = new Set([ this._code, 2 ]);

  public readonly _mapping: { [key: TSstring]: TSstring } = { "foo": nsTSScalaRuntimeFixturesAnyValChild.TSScalaRuntimeFixturesAnyValChild("bar"), "lorem": this._name };

  public readonly _dictOfList: { [key: TSstring]: ReadonlyArray<nsTSScalaRuntimeFixturesAnyValChild.TSScalaRuntimeFixturesAnyValChild> } = { "excludes": [ nsTSScalaRuntimeFixturesAnyValChild.TSScalaRuntimeFixturesAnyValChild("*.txt"), nsTSScalaRuntimeFixturesAnyValChild.TSScalaRuntimeFixturesAnyValChild(".gitignore") ], "includes": [ nsTSScalaRuntimeFixturesAnyValChild.TSScalaRuntimeFixturesAnyValChild("images/**"), nsTSScalaRuntimeFixturesAnyValChild.TSScalaRuntimeFixturesAnyValChild("*.jpg"), nsTSScalaRuntimeFixturesAnyValChild.TSScalaRuntimeFixturesAnyValChild("*.png") ] };

  public _concatSeq: ReadonlyArray<nsTSScalaRuntimeFixturesAnyValChild.TSScalaRuntimeFixturesAnyValChild> = [ ...this._list, ...[ nsTSScalaRuntimeFixturesAnyValChild.TSScalaRuntimeFixturesAnyValChild("foo"), nsTSScalaRuntimeFixturesAnyValChild.TSScalaRuntimeFixturesAnyValChild("bar") ], ...[ nsTSScalaRuntimeFixturesAnyValChild.TSScalaRuntimeFixturesAnyValChild("lorem") ]];

  public _mergedSet: ReadonlySet<TSnumber> = new Set([ ...this._set, ...new Set([ 3 ]) ]);

  public readonly _taggedDict: { [key: nsTSScalaRuntimeFixturesAnyValChild.TSScalaRuntimeFixturesAnyValChild]: TSstring } = (() => { const __buf1519690942: { [key: nsTSScalaRuntimeFixturesAnyValChild.TSScalaRuntimeFixturesAnyValChild]: TSstring } = {}; __buf1519690942[nsTSScalaRuntimeFixturesAnyValChild.TSScalaRuntimeFixturesAnyValChild("foo")] = nsTSScalaRuntimeFixturesAnyValChild.TSScalaRuntimeFixturesAnyValChild("bar"); return __buf1519690942 })();

  private static instance: TSSingleton;

  private constructor() {}

  public static getInstance() {
    if (!TSSingleton.instance) {
      TSSingleton.instance = new TSSingleton();
    }

    return TSSingleton.instance;
  }
}

export const TSSingletonInhabitant: TSSingleton = TSSingleton.getInstance();

export function isTSSingleton(v: any): v is TSSingleton {
  return (v instanceof TSSingleton) && (v === TSSingletonInhabitant);
}

export const idtltTSSingleton =
  idtlt.unknown.and(_unknown => idtlt.Err(
    'Cannot validator instance for singleton TSSingleton'));
"""
        }

        "with value class as tagged type" in {
          val singleton2WithTagged = SingletonDeclaration(
            "ScalaRuntimeFixturesTestObject2",
            ListSet(
              LiteralValue("name", StringRef, "\"Foo\""),
              LiteralValue(
                "const",
                taggedRef,
                "\"value\""
              ),
              SelectValue("foo", taggedRef, ThisTypeRef, "const"),
              LiteralValue("code", NumberRef, "1")
            ),
            superInterface = None
          )

          emit(
            ListSet(singleton2WithTagged)
          ) must_=== """export class ScalaRuntimeFixturesTestObject2 {
  public name: string = "Foo";

  public const: nsScalaRuntimeFixturesAnyValChild.ScalaRuntimeFixturesAnyValChild = nsScalaRuntimeFixturesAnyValChild.ScalaRuntimeFixturesAnyValChild("value");

  public foo: nsScalaRuntimeFixturesAnyValChild.ScalaRuntimeFixturesAnyValChild = this.const;

  public code: number = 1;

  private static instance: ScalaRuntimeFixturesTestObject2;

  private constructor() {}

  public static getInstance() {
    if (!ScalaRuntimeFixturesTestObject2.instance) {
      ScalaRuntimeFixturesTestObject2.instance = new ScalaRuntimeFixturesTestObject2();
    }

    return ScalaRuntimeFixturesTestObject2.instance;
  }
}

export const ScalaRuntimeFixturesTestObject2Inhabitant: ScalaRuntimeFixturesTestObject2 = ScalaRuntimeFixturesTestObject2.getInstance();

export function isScalaRuntimeFixturesTestObject2(v: any): v is ScalaRuntimeFixturesTestObject2 {
  return (v instanceof ScalaRuntimeFixturesTestObject2) && (v === ScalaRuntimeFixturesTestObject2Inhabitant);
}

export const idtltScalaRuntimeFixturesTestObject2 =
  idtlt.unknown.and(_unknown => idtlt.Err(
    'Cannot validator instance for singleton ScalaRuntimeFixturesTestObject2'));
"""
        }
      }

      "emit class #3" in {
        emit(
          ListSet(unionMember2Singleton)
        ) must_=== """// Validator for SingletonDeclaration ScalaRuntimeFixturesFamilyMember2
export const idtltScalaRuntimeFixturesFamilyMember2 = idtlt.literal("bar");

// Super-type declaration ScalaRuntimeFixturesFamily is ignored
export const idtltDiscriminatedScalaRuntimeFixturesFamilyMember2 = idtltScalaRuntimeFixturesFamilyMember2;

// Deriving TypeScript type from ScalaRuntimeFixturesFamilyMember2 validator
export type ScalaRuntimeFixturesFamilyMember2 = typeof idtltScalaRuntimeFixturesFamilyMember2.T;

export const ScalaRuntimeFixturesFamilyMember2Inhabitant: ScalaRuntimeFixturesFamilyMember2 = "bar";

export function isScalaRuntimeFixturesFamilyMember2(v: any): v is ScalaRuntimeFixturesFamilyMember2 {
  return idtltScalaRuntimeFixturesFamilyMember2.validate(v).ok;
}"""
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

          emit(ListSet(obj)) must_=== """export class Foo {
  private static instance: Foo;

  private constructor() {}

  public static getInstance() {
    if (!Foo.instance) {
      Foo.instance = new Foo();
    }

    return Foo.instance;
  }
}

export const FooInhabitant: Foo = Foo.getInstance();

export function isFoo(v: any): v is Foo {
  return (v instanceof Foo) && (v === FooInhabitant);
}

export const idtltFoo =
  idtlt.unknown.and(_unknown => idtlt.Err(
    'Cannot validator instance for singleton Foo'));
"""
        }

        "with string value" in {
          val obj = SingletonDeclaration(
            name = "Foo",
            values = ListSet(barVal),
            superInterface = Option.empty
          )

          emit(ListSet(obj)) must_=== """export class Foo {
  public bar: string = "lorem";

  private static instance: Foo;

  private constructor() {}

  public static getInstance() {
    if (!Foo.instance) {
      Foo.instance = new Foo();
    }

    return Foo.instance;
  }
}

export const FooInhabitant: Foo = Foo.getInstance();

export function isFoo(v: any): v is Foo {
  return (v instanceof Foo) && (v === FooInhabitant);
}

export const idtltFoo =
  idtlt.unknown.and(_unknown => idtlt.Err(
    'Cannot validator instance for singleton Foo'));
"""
        }

        "with object value" in {
          val obj = SingletonDeclaration(
            name = "Foo",
            values = ListSet(
              barVal,
              LiteralValue(name = "ipsum", typeRef = NumberRef, rawValue = "2")
            ),
            superInterface = Option.empty
          )

          emit(ListSet(obj)) must_=== """export class Foo {
  public bar: string = "lorem";

  public ipsum: number = 2;

  private static instance: Foo;

  private constructor() {}

  public static getInstance() {
    if (!Foo.instance) {
      Foo.instance = new Foo();
    }

    return Foo.instance;
  }
}

export const FooInhabitant: Foo = Foo.getInstance();

export function isFoo(v: any): v is Foo {
  return (v instanceof Foo) && (v === FooInhabitant);
}

export const idtltFoo =
  idtlt.unknown.and(_unknown => idtlt.Err(
    'Cannot validator instance for singleton Foo'));
"""
        }
      }
    }

    "emit union" in {
      emit(ListSet(union1)) must_=== """// Validator for UnionDeclaration ScalaRuntimeFixturesFamily
export const idtltScalaRuntimeFixturesFamily = idtlt.union(
  nsScalaRuntimeFixturesFamilyMember1.idtltDiscriminatedScalaRuntimeFixturesFamilyMember1,
  nsScalaRuntimeFixturesFamilyMember2.idtltDiscriminatedScalaRuntimeFixturesFamilyMember2,
  nsScalaRuntimeFixturesFamilyMember3.idtltDiscriminatedScalaRuntimeFixturesFamilyMember3);

// Fields are ignored: foo

// Deriving TypeScript type from ScalaRuntimeFixturesFamily validator
export type ScalaRuntimeFixturesFamily = typeof idtltScalaRuntimeFixturesFamily.T;

export const idtltDiscriminatedScalaRuntimeFixturesFamily = idtlt.intersection(
  idtltScalaRuntimeFixturesFamily,
  idtlt.object({
    _type: idtlt.literal('ScalaRuntimeFixturesFamily')
  })
);

// Deriving TypeScript type from idtltDiscriminatedScalaRuntimeFixturesFamily validator
export type DiscriminatedScalaRuntimeFixturesFamily = typeof idtltDiscriminatedScalaRuntimeFixturesFamily.T;

export const ScalaRuntimeFixturesFamily = {
  "bar": nsScalaRuntimeFixturesFamilyMember2.ScalaRuntimeFixturesFamilyMember2Inhabitant, 
  "lorem": nsScalaRuntimeFixturesFamilyMember3.ScalaRuntimeFixturesFamilyMember3Inhabitant
} as const;

export const idtltScalaRuntimeFixturesFamilyKnownValues: ReadonlyArray<ScalaRuntimeFixturesFamily> = Object.values(ScalaRuntimeFixturesFamily) as ReadonlyArray<ScalaRuntimeFixturesFamily>;

export function isScalaRuntimeFixturesFamily(v: any): v is ScalaRuntimeFixturesFamily {
  return (
    nsScalaRuntimeFixturesFamilyMember1.isScalaRuntimeFixturesFamilyMember1(v) ||
    nsScalaRuntimeFixturesFamilyMember2.isScalaRuntimeFixturesFamilyMember2(v) ||
    nsScalaRuntimeFixturesFamilyMember3.isScalaRuntimeFixturesFamilyMember3(v)
  );
}
"""
    }

    "emit enumeration as union" in {
      emit(ListSet(enum1)) must beTypedEqualTo(
        """// Validator for EnumDeclaration ScalaRuntimeFixturesTestEnumeration
export const idtltScalaRuntimeFixturesTestEnumeration = idtlt.union(
  idtlt.literal('A'),
  idtlt.literal('B'),
  idtlt.literal('C'))

// Deriving TypeScript type from ScalaRuntimeFixturesTestEnumeration validator
export type ScalaRuntimeFixturesTestEnumeration = typeof idtltScalaRuntimeFixturesTestEnumeration.T;

export const idtltDiscriminatedScalaRuntimeFixturesTestEnumeration = idtlt.intersection(
  idtltScalaRuntimeFixturesTestEnumeration,
  idtlt.object({
    _type: idtlt.literal('ScalaRuntimeFixturesTestEnumeration')
  })
);

// Deriving TypeScript type from idtltDiscriminatedScalaRuntimeFixturesTestEnumeration validator
export type DiscriminatedScalaRuntimeFixturesTestEnumeration = typeof idtltDiscriminatedScalaRuntimeFixturesTestEnumeration.T;

export const idtltScalaRuntimeFixturesTestEnumerationValues: Array<ScalaRuntimeFixturesTestEnumeration> = [
  'A',
  'B',
  'C'
];

export function isScalaRuntimeFixturesTestEnumeration(v: any): v is ScalaRuntimeFixturesTestEnumeration {
   return idtltScalaRuntimeFixturesTestEnumeration.validate(v).ok;
}
"""
      )
    }
  }

  // ---

  private val idtltDeclMapper = new DeclarationMapper

  private val idtltTypeMapper = new TypeMapper

  def emit(
      decls: ListSet[Declaration],
      config: Settings = Settings(),
      declMapper: TypeScriptDeclarationMapper = idtltDeclMapper,
      importResolver: TypeScriptImportResolver =
        TypeScriptImportResolver.Defaults,
      typeMapper: TypeScriptTypeMapper = idtltTypeMapper
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
