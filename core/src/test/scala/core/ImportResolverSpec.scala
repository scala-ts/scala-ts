package io.github.scalats.core

import io.github.scalats.ast._
import io.github.scalats.core.Internals.ListSet

import org.specs2.specification.core.Fragments

import TranspilerCompat.ns

final class ImportResolverSpec extends org.specs2.mutable.Specification {
  "Import resolver".title

  "Required types of type declaration" should {
    import TranspilerResults._

    "with default resolver" >> {
      import ImportResolver.defaultResolver

      "be resolved for union type" in {
        defaultResolver(union1) must_=== ListSet.empty[TypeRef] and {
          defaultResolver(unionMember2Singleton) must_=== ListSet[TypeRef](
            union1.reference
          )
        }
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
            SingletonTypeRef(
              s"${ns}TestObject2Nested1",
              ListSet.empty
            ),
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
                typeRef = ArrayRef(NumberRef.int, false), // TODO: SetRef
                valueTypeRef = NumberRef.int,
                elements = Set(
                  SelectValue(
                    "set[0]",
                    NumberRef.int,
                    testClass2,
                    "code"
                  ),
                  LiteralValue("set[1]", NumberRef.int, "2")
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
                keyTypeRef = StringRef,
                valueTypeRef = StringRef,
                entries = Map(
                  LiteralValue(
                    "mapping.foo",
                    StringRef,
                    "\"foo\""
                  ) -> LiteralValue("mapping[foo]", StringRef, "\"bar\""),
                  LiteralValue(
                    "mapping.lorem",
                    StringRef,
                    "\"lorem\""
                  ) -> SelectValue(
                    "mapping[lorem]",
                    StringRef,
                    testClass1,
                    "name"
                  )
                )
              ),
              LiteralValue("const", StringRef, "\"value\""),
              LiteralValue("code", NumberRef.int, "1")
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

        "with invariants" in {
          val singleton: SingletonDeclaration = {
            val qual = CustomTypeRef("Foo", Nil)
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
                      qual,
                      "Hello"
                    ),
                    SelectValue("start[1]", hiTypeRef, qual, "Hi")
                  )
                )
              ),
              superInterface = None
            )
          }

          defaultResolver(singleton).toSet must_=== Set(
            CustomTypeRef("Foo", Nil),
            CustomTypeRef("Greeting", Nil)
          )
        }
      }
    }

    "with UnionWithLiteralSingleton" >> {
      import ImportResolver.{ unionWithLiteralSingleton => resolve }

      "be resolved for union type" in {
        resolve(union1) must beSome[ListSet[TypeRef]].which {
          _ must_=== ListSet.empty[TypeRef] ++ union1.possibilities
        } and {
          resolve(unionMember2Singleton) must beSome[ListSet[TypeRef]].which {
            _ must beEmpty
          }
        }
      }
    }
  }
}
