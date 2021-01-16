package io.github.scalats.core

import scala.collection.immutable.Set

import io.github.scalats.typescript.{
  CustomTypeRef,
  InterfaceDeclaration,
  TypeRef
}

import org.specs2.specification.core.Fragments

final class TypeScriptImportResolverSpec
  extends org.specs2.mutable.Specification {
  "TypeScript import resolver" title

  "Required types of type declaration" should {
    import TranspilerResults._

    "with default resolver" >> {
      import TypeScriptImportResolver.defaultResolver

      "be resolved for union type" in {
        defaultResolver(union1) must_=== Set.empty[TypeRef]
      }

      "be resolved as an empty set for interface" >> {
        Fragments.foreach(Seq[InterfaceDeclaration](
          interface1, interface2, interface3, interface5, interface7,
          unionIface, interface10)) { i =>
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
          defaultResolver(singleton2) must_=== Set(
            CustomTypeRef("SupI", List.empty))
        }
      }
    }

    "with UnionWithLiteralSingleton" >> {
      import TypeScriptImportResolver.{ unionWithLiteralSingleton => resolve }

      "be resolved for union type" in {
        resolve(union1) must beSome[Set[TypeRef]].which {
          _ must_=== Set.empty[TypeRef] ++ union1.possibilities
        }
      }
    }
  }
}
