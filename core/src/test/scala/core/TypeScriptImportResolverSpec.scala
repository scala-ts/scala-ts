package io.github.scalats.core

import io.github.scalats.core.Internals.ListSet
import io.github.scalats.typescript._

import org.specs2.specification.core.Fragments

final class TypeScriptImportResolverSpec
    extends org.specs2.mutable.Specification {
  "TypeScript import resolver" title

  "Required types of type declaration" should {
    import TranspilerResults._

    "with default resolver" >> {
      import TypeScriptImportResolver.defaultResolver

      "be resolved for union type" in {
        defaultResolver(union1) must_=== ListSet.empty[TypeRef]
      }

      "be resolved as an empty set for interface" >> {
        Fragments.foreach(
          Seq[InterfaceDeclaration](
            interface1,
            interface2,
            interface3,
            interface5,
            interface7,
            unionIface,
            interface10
          )
        ) { i =>
          i.name in {
            defaultResolver(i) must beEmpty
          }
        }
      }

      "be resolved for singleton" >> {
        singleton1.name in {
          defaultResolver(singleton1) must beEmpty
        }

        singleton2.name in {
          defaultResolver(singleton2) must_=== ListSet(
            CustomTypeRef("SupI", List.empty)
          )
        }

        "more complex type" in {
          val taggedString =
            TaggedRef("ScalaRuntimeFixturesAnyValChild", StringRef)

          val taggedFoo = TaggedRef("Foo", StringRef)

          val testClass1 =
            CustomTypeRef("ScalaRuntimeFixturesTestClass1", List.empty)

          val testClass2 =
            CustomTypeRef("ScalaRuntimeFixturesTestClass2", List.empty)

          val singleton2 = SingletonDeclaration(
            "ScalaRuntimeFixturesTestObject2",
            ListSet(
              LiteralValue(
                name = "name",
                typeRef = taggedString,
                rawValue = "\"Foo\""
              ),
              SetValue(
                name = "set",
                typeRef = ArrayRef(NumberRef), // TODO: SetRef
                valueTypeRef = NumberRef,
                elements = Set(
                  SelectValue(
                    "set[0]",
                    NumberRef,
                    testClass2,
                    "code"
                  ),
                  LiteralValue("set[1]", NumberRef, "2")
                )
              ),
              ListValue(
                name = "list",
                typeRef = StringRef,
                valueTypeRef = taggedFoo,
                elements = List(
                  LiteralValue(
                    name = "list[0]",
                    typeRef = taggedFoo,
                    rawValue = "\"first\""
                  ),
                  SelectValue("list[1]", taggedFoo, ThisTypeRef, "name")
                )
              ),
              SelectValue("foo", StringRef, ThisTypeRef, "name"),
              DictionaryValue(
                name = "mapping",
                typeRef = MapType(StringRef, StringRef),
                valueTypeRef = StringRef,
                entries = Map(
                  "foo" -> LiteralValue("mapping[foo]", StringRef, "\"bar\""),
                  "lorem" -> SelectValue(
                    "mapping[lorem]",
                    StringRef,
                    testClass1,
                    "name"
                  )
                )
              ),
              LiteralValue("const", StringRef, "\"value\""),
              LiteralValue("code", NumberRef, "1")
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

          defaultResolver(singleton2) must_=== ListSet(
            taggedString,
            testClass2,
            taggedFoo,
            testClass1,
            CustomTypeRef("SupI", List.empty)
          )
        }
      }
    }

    "with UnionWithLiteralSingleton" >> {
      import TypeScriptImportResolver.{ unionWithLiteralSingleton => resolve }

      "be resolved for union type" in {
        resolve(union1) must beSome[ListSet[TypeRef]].which {
          _ must_=== ListSet.empty[TypeRef] ++ union1.possibilities
        }
      }
    }
  }
}
