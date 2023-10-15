package io.github.scalats.idtlt

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

final class IdtltEmitterSpec extends org.specs2.mutable.Specification {
  "Idtlt TypeScript emitter".title

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
    typeof v === 'object' && Object.keys(v).length === 0
  );
}""")

    }

    "emit interface for a class with one primitive member" in {
      emit(ListSet(interface1)) must beTypedEqualTo(
        s"""// Validator for InterfaceDeclaration ${ns}TestClass1
export const idtlt${ns}TestClass1 = idtlt.object({
  name: idtlt.string,
});

// Deriving TypeScript type from ${ns}TestClass1 validator
export type ${ns}TestClass1 = typeof idtlt${ns}TestClass1.T;

export const idtltDiscriminated${ns}TestClass1 = idtlt.intersection(
  idtlt${ns}TestClass1,
  idtlt.object({
    _type: idtlt.literal('${ns}TestClass1')
  })
);

// Deriving TypeScript type from idtltDiscriminated${ns}TestClass1 validator
export type Discriminated${ns}TestClass1 = typeof idtltDiscriminated${ns}TestClass1.T;

export const discriminated${ns}TestClass1: (_: ${ns}TestClass1) => Discriminated${ns}TestClass1 = (v: ${ns}TestClass1) => ({ _type: '${ns}TestClass1', ...v });

export function is${ns}TestClass1(v: any): v is ${ns}TestClass1 {
  return (
    ((typeof v['name']) === 'string')
  );
}"""
      )
    }

    "emit interface for a class with generic member" in {
      emit(ListSet(interface2)) must beTypedEqualTo(
        s"""// Not supported: InterfaceDeclaration '${ns}TestClass2'
// - type parameters: T

export function is${ns}TestClass2(v: any): boolean {
  return v && false;
}
"""
      )
    }

    "emit interface for a class with generic array" in {
      emit(ListSet(interface3)) must beTypedEqualTo(
        s"""// Not supported: InterfaceDeclaration '${ns}TestClass3'
// - type parameters: T

export function is${ns}TestClass3(v: any): boolean {
  return v && false;
}
"""
      )
    }

    "emit interface for a generic case class with a optional member" in {
      emit(ListSet(interface5)) must beTypedEqualTo(
        s"""// Not supported: InterfaceDeclaration '${ns}TestClass5'
// - type parameters: T

export function is${ns}TestClass5(v: any): boolean {
  return v && false;
}
"""
      )
    }

    "emit interface for a generic case class with disjunction" in {
      // TODO: Add example to documentation
      emit(ListSet(interface7)) must beTypedEqualTo(
        s"""// Not supported: InterfaceDeclaration '${ns}TestClass7'
// - type parameters: T

export function is${ns}TestClass7(v: any): boolean {
  return v && false;
}
"""
      )
    }

    "emit tagged type" >> {
      "as type alias" in {
        emit(ListSet(taggedDeclaration1)) must beTypedEqualTo(
          s"""// Validator for TaggedDeclaration ${valueClassNs}AnyValChild
export type ${valueClassNs}AnyValChild = string & { __tag: '${valueClassNs}AnyValChild' };

export function ${valueClassNs}AnyValChild<T extends string>(value: T): ${valueClassNs}AnyValChild & T {
  return value as (${valueClassNs}AnyValChild & T);
}

export const idtlt${valueClassNs}AnyValChild = idtlt.string.tagged<${valueClassNs}AnyValChild>();

export function is${valueClassNs}AnyValChild(v: any): v is ${valueClassNs}AnyValChild {
  return idtlt${valueClassNs}AnyValChild.validate(v).ok;
}
"""
        )
      }

      "as tagged type" in {
        emit(ListSet(taggedDeclaration1)) must beTypedEqualTo(
          s"""// Validator for TaggedDeclaration ${valueClassNs}AnyValChild
export type ${valueClassNs}AnyValChild = string & { __tag: '${valueClassNs}AnyValChild' };

export function ${valueClassNs}AnyValChild<T extends string>(value: T): ${valueClassNs}AnyValChild & T {
  return value as (${valueClassNs}AnyValChild & T);
}

export const idtlt${valueClassNs}AnyValChild = idtlt.string.tagged<${valueClassNs}AnyValChild>();

export function is${valueClassNs}AnyValChild(v: any): v is ${valueClassNs}AnyValChild {
  return idtlt${valueClassNs}AnyValChild.validate(v).ok;
}
"""
        )
      }

      "as member" in {
        emit(ListSet(interface8)) must beTypedEqualTo(
          s"""// Validator for InterfaceDeclaration ${valueClassNs}TestClass8
export const idtlt${valueClassNs}TestClass8 = idtlt.object({
  name: ns${valueClassNs}AnyValChild.idtlt${valueClassNs}AnyValChild,
  aliases: idtlt.array(ns${valueClassNs}AnyValChild.idtlt${valueClassNs}AnyValChild),
});

// Deriving TypeScript type from ${valueClassNs}TestClass8 validator
export type ${valueClassNs}TestClass8 = typeof idtlt${valueClassNs}TestClass8.T;

export const idtltDiscriminated${valueClassNs}TestClass8 = idtlt.intersection(
  idtlt${valueClassNs}TestClass8,
  idtlt.object({
    _type: idtlt.literal('${valueClassNs}TestClass8')
  })
);

// Deriving TypeScript type from idtltDiscriminated${valueClassNs}TestClass8 validator
export type Discriminated${valueClassNs}TestClass8 = typeof idtltDiscriminated${valueClassNs}TestClass8.T;

export const discriminated${valueClassNs}TestClass8: (_: ${valueClassNs}TestClass8) => Discriminated${valueClassNs}TestClass8 = (v: ${valueClassNs}TestClass8) => ({ _type: '${valueClassNs}TestClass8', ...v });

export function is${valueClassNs}TestClass8(v: any): v is ${valueClassNs}TestClass8 {
  return (
    (v['name'] && ns${valueClassNs}AnyValChild.is${valueClassNs}AnyValChild(v['name'])) &&
    (Array.isArray(v['aliases']) && v['aliases'].every(elmt => elmt && ns${valueClassNs}AnyValChild.is${valueClassNs}AnyValChild(elmt)))
  );
}"""
        )
      }
    }

    "for singleton" >> {
      "emit class #1" in {
        emit(ListSet(singleton1)) must beTypedEqualTo(
          s"""export class ${ns}TestObject1 {
  private static instance: ${ns}TestObject1;

  private constructor() {}

  public static getInstance() {
    if (!${ns}TestObject1.instance) {
      ${ns}TestObject1.instance = new ${ns}TestObject1();
    }

    return ${ns}TestObject1.instance;
  }
}

export const ${ns}TestObject1Inhabitant: ${ns}TestObject1 = ${ns}TestObject1.getInstance();

export function is${ns}TestObject1(v: any): v is ${ns}TestObject1 {
  return (v instanceof ${ns}TestObject1) && (v === ${ns}TestObject1Inhabitant);
}

export const idtlt${ns}TestObject1 =
  idtlt.unknown.and(_unknown => idtlt.Err(
    'Cannot validator instance for singleton ${ns}TestObject1'));
"""
        )
      }

      "emit class #2" >> {
        "with value class as validator literal" in {
          // TODO: foo: idtlt.this.name,

          // SCALATS1: No implements SupI
          emit(
            ListSet(singleton2)
          ) must_=== s"""// Validator for SingletonDeclaration ${ns}TestObject2
export const idtlt${ns}TestObject2 = idtlt.object({
  name: idtlt.literal("Foo \\"bar\\""),
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
export const idtltDiscriminated${ns}TestObject2 = idtlt${ns}TestObject2;

// Deriving TypeScript type from ${ns}TestObject2 validator
export type ${ns}TestObject2 = typeof idtlt${ns}TestObject2.T;

export const ${ns}TestObject2Inhabitant: ${ns}TestObject2 = {
  name: "Foo \\"bar\\"",
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

export function is${ns}TestObject2(v: any): v is ${ns}TestObject2 {
  return idtlt${ns}TestObject2.validate(v).ok;
}"""
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
          ) must_=== s"""export class TSSingleton {
  public _name: nsTS${valueClassNs}AnyValChild.TS${valueClassNs}AnyValChild & "Foo" = nsTS${valueClassNs}AnyValChild.TS${valueClassNs}AnyValChild("Foo");

  public _code: TSnumber & 1 = 1;

  public _const: TSstring & "value" = "value";

  public _foo: nsTS${valueClassNs}AnyValChild.TS${valueClassNs}AnyValChild = this._name;

  public _list: ReadonlyArray<TSstring> = [ nsTS${valueClassNs}AnyValChild.TS${valueClassNs}AnyValChild("first"), this._name ];

  public _set: ReadonlySet<TSnumber> = new Set([ this._code, 2 ]);

  public readonly _mapping: { [key: TSstring]: TSstring } = { "foo": nsTS${valueClassNs}AnyValChild.TS${valueClassNs}AnyValChild("bar"), "lorem": this._name };

  public readonly _dictOfList: { [key: TSstring]: ReadonlyArray<nsTS${valueClassNs}AnyValChild.TS${valueClassNs}AnyValChild> } = { "excludes": [ nsTS${valueClassNs}AnyValChild.TS${valueClassNs}AnyValChild("*.txt"), nsTS${valueClassNs}AnyValChild.TS${valueClassNs}AnyValChild(".gitignore") ], "includes": [ nsTS${valueClassNs}AnyValChild.TS${valueClassNs}AnyValChild("images/**"), nsTS${valueClassNs}AnyValChild.TS${valueClassNs}AnyValChild("*.jpg"), nsTS${valueClassNs}AnyValChild.TS${valueClassNs}AnyValChild("*.png") ] };

  public _concatSeq: ReadonlyArray<nsTS${valueClassNs}AnyValChild.TS${valueClassNs}AnyValChild> = [ ...this._list, ...[ nsTS${valueClassNs}AnyValChild.TS${valueClassNs}AnyValChild("foo"), nsTS${valueClassNs}AnyValChild.TS${valueClassNs}AnyValChild("bar") ], ...[ nsTS${valueClassNs}AnyValChild.TS${valueClassNs}AnyValChild("lorem") ]];

  public _mergedSet: ReadonlySet<TSnumber> = new Set([ ...this._set, ...new Set([ 3 ]) ]);

  public readonly _taggedDict: { [key: nsTS${valueClassNs}AnyValChild.TS${valueClassNs}AnyValChild]: TSstring } = (() => { const __buf1519690942: { [key: nsTS${valueClassNs}AnyValChild.TS${valueClassNs}AnyValChild]: TSstring } = {}; __buf1519690942[nsTS${valueClassNs}AnyValChild.TS${valueClassNs}AnyValChild("foo")] = nsTS${valueClassNs}AnyValChild.TS${valueClassNs}AnyValChild("bar"); return __buf1519690942 })();

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
          ) must_=== s"""export class ${valueClassNs}TestObject2 {
  public name: string & "Foo" = "Foo";

  public const: ns${valueClassNs}AnyValChild.${valueClassNs}AnyValChild & "value" = ns${valueClassNs}AnyValChild.${valueClassNs}AnyValChild("value");

  public foo: ns${valueClassNs}AnyValChild.${valueClassNs}AnyValChild = this.const;

  public code: number & 1 = 1;

  private static instance: ${valueClassNs}TestObject2;

  private constructor() {}

  public static getInstance() {
    if (!${valueClassNs}TestObject2.instance) {
      ${valueClassNs}TestObject2.instance = new ${valueClassNs}TestObject2();
    }

    return ${valueClassNs}TestObject2.instance;
  }
}

export const ${valueClassNs}TestObject2Inhabitant: ${valueClassNs}TestObject2 = ${valueClassNs}TestObject2.getInstance();

export function is${valueClassNs}TestObject2(v: any): v is ${valueClassNs}TestObject2 {
  return (v instanceof ${valueClassNs}TestObject2) && (v === ${valueClassNs}TestObject2Inhabitant);
}

export const idtlt${valueClassNs}TestObject2 =
  idtlt.unknown.and(_unknown => idtlt.Err(
    'Cannot validator instance for singleton ${valueClassNs}TestObject2'));
"""
        }
      }

      "emit class #3" in {
        emit(
          ListSet(unionMember2Singleton)
        ) must_=== s"""// Validator for SingletonDeclaration ${ns}FamilyMember2
export const idtlt${ns}FamilyMember2 = idtlt.literal("bar");

// Super-type declaration ${ns}Family is ignored
export const idtltDiscriminated${ns}FamilyMember2 = idtlt${ns}FamilyMember2;

// Deriving TypeScript type from ${ns}FamilyMember2 validator
export type ${ns}FamilyMember2 = typeof idtlt${ns}FamilyMember2.T;

export const ${ns}FamilyMember2Inhabitant: ${ns}FamilyMember2 = "bar";

export function is${ns}FamilyMember2(v: any): v is ${ns}FamilyMember2 {
  return idtlt${ns}FamilyMember2.validate(v).ok;
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
  public bar: string & "lorem" = "lorem";

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
              LiteralValue(
                name = "ipsum",
                typeRef = NumberRef.int,
                rawValue = "2"
              )
            ),
            superInterface = Option.empty
          )

          emit(ListSet(obj)) must_=== """export class Foo {
  public bar: string & "lorem" = "lorem";

  public ipsum: number & 2 = 2;

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
      emit(
        ListSet(union1)
      ) must_=== s"""// Validator for UnionDeclaration ${ns}Family
export const idtlt${ns}Family = idtlt.union(
  ns${ns}FamilyMember1.idtltDiscriminated${ns}FamilyMember1,
  ns${ns}FamilyMember2.idtltDiscriminated${ns}FamilyMember2,
  ns${ns}FamilyMember3.idtltDiscriminated${ns}FamilyMember3);

// Fields are ignored: foo

// Deriving TypeScript type from ${ns}Family validator
export type ${ns}Family = typeof idtlt${ns}Family.T;

export const idtltDiscriminated${ns}Family = idtlt.intersection(
  idtlt${ns}Family,
  idtlt.object({
    _type: idtlt.literal('${ns}Family')
  })
);

// Deriving TypeScript type from idtltDiscriminated${ns}Family validator
export type Discriminated${ns}Family = typeof idtltDiscriminated${ns}Family.T;

export const ${ns}Family = {
  "bar": ns${ns}FamilyMember2.${ns}FamilyMember2Inhabitant, 
  "lorem": ns${ns}FamilyMember3.${ns}FamilyMember3Inhabitant
} as const;

export const idtlt${ns}FamilyKnownValues: ReadonlyArray<${ns}Family> = Object.values(${ns}Family) as ReadonlyArray<${ns}Family>;

export function is${ns}Family(v: any): v is ${ns}Family {
  return (
    ns${ns}FamilyMember1.is${ns}FamilyMember1(v) ||
    ns${ns}FamilyMember2.is${ns}FamilyMember2(v) ||
    ns${ns}FamilyMember3.is${ns}FamilyMember3(v)
  );
}
"""
    }

    "emit enumeration as union" in {
      emit(ListSet(enum1)) must beTypedEqualTo(
        s"""// Validator for EnumDeclaration ${ns}TestEnumeration
export const idtlt${ns}TestEnumeration = idtlt.union(
  idtlt.literal('A'),
  idtlt.literal('B'),
  idtlt.literal('C'))

// Deriving TypeScript type from ${ns}TestEnumeration validator
export type ${ns}TestEnumeration = typeof idtlt${ns}TestEnumeration.T;

export const idtltDiscriminated${ns}TestEnumeration = idtlt.intersection(
  idtlt${ns}TestEnumeration,
  idtlt.object({
    _type: idtlt.literal('${ns}TestEnumeration')
  })
);

// Deriving TypeScript type from idtltDiscriminated${ns}TestEnumeration validator
export type Discriminated${ns}TestEnumeration = typeof idtltDiscriminated${ns}TestEnumeration.T;

export const idtlt${ns}TestEnumerationValues: Array<${ns}TestEnumeration> = [
  'A',
  'B',
  'C'
];

export function is${ns}TestEnumeration(v: any): v is ${ns}TestEnumeration {
   return idtlt${ns}TestEnumeration.validate(v).ok;
}
"""
      )
    }
  }

  // ---

  private val idtltDeclMapper = new IdtltDeclarationMapper

  private val idtltTypeMapper = new IdtltTypeMapper

  def emit(
      decls: ListSet[Declaration],
      config: Settings = Settings(),
      declMapper: DeclarationMapper = idtltDeclMapper,
      importResolver: ImportResolver = ImportResolver.Defaults,
      typeMapper: TypeMapper = idtltTypeMapper
    ): String = {
    val buf = new java.io.ByteArrayOutputStream()
    lazy val out = new java.io.PrintStream(buf)

    val emiter = new Emitter(
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
