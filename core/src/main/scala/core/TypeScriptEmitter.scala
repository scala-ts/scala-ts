package io.github.scalats.core

import java.io.PrintStream

import scala.collection.immutable.ListSet

// TODO: Append on out
// TODO: Emit Option (space-lift?)
// TODO: (low priority) Use a template engine (velocity?)
/**
 * @param out the function to select a `PrintStream` from type name
 */
final class TypeScriptEmitter(
  val config: Configuration,
  out: String => PrintStream) {

  import TypeScriptModel._
  import Internals.list

  import config.{ typescriptIndent => indent }
  import config.typescriptLineSeparator.{ value => lineSeparator }

  def emit(declaration: ListSet[Declaration]): Unit =
    list(declaration).foreach {
      case decl: InterfaceDeclaration =>
        emitInterfaceDeclaration(decl)

      case decl: EnumDeclaration =>
        emitEnumDeclaration(decl)

      case decl: ClassDeclaration =>
        emitClassDeclaration(decl)

      case SingletonDeclaration(name, members, superInterface) =>
        emitSingletonDeclaration(name, members, superInterface)

      case UnionDeclaration(name, fields, possibilities, superInterface) =>
        emitUnionDeclaration(
          name, fields, possibilities, superInterface)
    }

  // ---

  private def emitUnionDeclaration(
    name: String,
    fields: ListSet[Member],
    possibilities: ListSet[CustomTypeRef],
    superInterface: Option[InterfaceDeclaration]): Unit = withOut(name) { o =>
    // Namespace and union type
    o.println(s"export namespace $name {")
    o.println(s"""${indent}type Union = ${possibilities.map(_.name) mkString " | "}${lineSeparator}""")

    if (config.emitCodecs.enabled) {
      // TODO: Discriminator naming
      val discriminatorName = "_type"
      val naming: String => String = identity[String](_)
      val children = list(possibilities)

      // Decoder factory: MyClass.fromData({..})
      o.println(s"\n${indent}public static fromData(data: any): ${name} {")
      o.println(s"${indent}${indent}switch (data.${discriminatorName}) {")

      children.foreach { sub =>
        val clazz = if (sub.name startsWith "I") sub.name.drop(1) else sub.name

        o.println(s"""${indent}${indent}${indent}case "${naming(sub.name)}": {""")
        o.println(s"${indent}${indent}${indent}${indent}return ${clazz}.fromData(data)${lineSeparator}")
        o.println(s"${indent}${indent}${indent}}")
      }

      o.println(s"${indent}${indent}}")
      o.println(s"${indent}}")

      // Encoder
      o.println(s"\n${indent}public static toData(instance: ${name}): any {")

      children.zipWithIndex.foreach {
        case (sub, index) =>
          o.print(s"${indent}${indent}")

          if (index > 0) {
            o.print("} else ")
          }

          val clazz =
            if (sub.name startsWith "I") sub.name.drop(1) else sub.name

          o.println(s"if (instance instanceof ${sub.name}) {")
          o.println(s"${indent}${indent}${indent}const data = ${clazz}.toData(instance)${lineSeparator}")
          o.println(s"""${indent}${indent}${indent}data['$discriminatorName'] = "${naming(sub.name)}"${lineSeparator}""")
          o.println(s"${indent}${indent}${indent}return data${lineSeparator}")
      }

      o.println(s"${indent}${indent}}")
      o.println(s"${indent}}")
    }

    o.println("}")

    // Union interface
    o.print(s"\nexport interface I${name}")

    superInterface.foreach { iface =>
      o.print(s" extends I${iface.name}")
    }

    o.println(" {")

    // Abstract fields - common to all the subtypes
    val fieldNaming = config.fieldNaming(name, _: String)

    list(fields).foreach { member =>
      o.println(s"${indent}${fieldNaming(member.name)}: ${getTypeRefString(member.typeRef)}${lineSeparator}")
    }

    o.println("}")
  }

  private def emitSingletonDeclaration(
    name: String,
    members: ListSet[Member],
    superInterface: Option[InterfaceDeclaration]): Unit = withOut(name) { o =>
    if (members.nonEmpty) {
      def mkString = members.map {
        case Member(nme, tpe) => s"$nme ($tpe)"
      }.mkString(", ")

      throw new IllegalStateException(s"Cannot emit static members for properties of singleton '$name': ${mkString}")
    }

    // Class definition
    o.print(s"export class $name")

    superInterface.filter(_ => members.isEmpty).foreach { i =>
      o.print(s" implements ${i.name}")
    }

    o.println(" {")

    o.println(s"${indent}private static instance: $name${lineSeparator}\n")

    o.println(s"${indent}private constructor() {}\n")
    o.println(s"${indent}public static getInstance() {")
    o.println(s"${indent}${indent}if (!${name}.instance) {")
    o.println(s"${indent}${indent}${indent}${name}.instance = new ${name}()${lineSeparator}")
    o.println(s"${indent}${indent}}\n")
    o.println(s"${indent}${indent}return ${name}.instance${lineSeparator}")
    o.println(s"${indent}}")

    if (config.emitCodecs.enabled) {
      // Decoder factory: MyClass.fromData({..})
      o.println(s"\n${indent}public static fromData(data: any): ${name} {")
      o.println(s"${indent}${indent}return ${name}.instance${lineSeparator}")
      o.println(s"${indent}}")

      // Encoder
      o.println(s"\n${indent}public static toData(instance: ${name}): any {")
      o.println(s"${indent}${indent}return instance${lineSeparator}")
      o.println(s"${indent}}")
    }

    o.println("}")
  }

  private def emitInterfaceDeclaration(
    decl: InterfaceDeclaration): Unit = {
    val InterfaceDeclaration(name, fields, typeParams, superInterface) = decl

    withOut(name) { o =>
      o.print(s"export interface $name${typeParameters(typeParams)}")

      superInterface.foreach { iface =>
        o.print(s" extends ${iface.name}")
      }

      o.println(" {")

      list(fields).foreach { member =>
        o.println(s"${indent}${config.fieldNaming(name, member.name)}: ${getTypeRefString(member.typeRef)}${lineSeparator}")
      }

      o.println("}")
    }
  }

  private def emitEnumDeclaration(decl: EnumDeclaration): Unit = {
    val EnumDeclaration(name, values) = decl

    withOut(name) { o =>
      o.println(s"export enum $name {")

      list(values).foreach { value =>
        o.println(s"${indent}${value} = '${value}',")
      }

      o.println("}")
    }
  }

  private def emitClassDeclaration(decl: ClassDeclaration): Unit = {
    val ClassDeclaration(name, ClassConstructor(parameters),
      values, typeParams, _ /*superInterface*/ ) = decl

    withOut(name) { o =>
      val tparams = typeParameters(typeParams)

      if (values.nonEmpty) {
        def mkString = values.map {
          case Member(nme, tpe) => s"$nme ($tpe)"
        }.mkString(", ")

        throw new IllegalStateException(
          s"Cannot emit static members for class values: ${mkString}")
      }

      // Class definition
      o.print(s"export class ${name}${tparams}")

      if (config.emitInterfaces) {
        o.print(s" implements I${name}${tparams}")
      }

      o.println(" {")

      val fieldNaming = config.fieldNaming(name, _: String)

      list(values).foreach { v =>
        o.print(indent)

        if (config.emitInterfaces) {
          o.print("public ")
        }

        o.println(s"${fieldNaming(v.name)}: ${getTypeRefString(v.typeRef)}${lineSeparator}")
      }

      val params = list(parameters)

      if (!config.emitInterfaces) {
        // Class fields
        params.foreach { parameter =>
          o.print(s"${indent}public ${parameter.name}: ${getTypeRefString(parameter.typeRef)}${lineSeparator}")
        }
      }

      // Class constructor
      o.print(s"${indent}constructor(")

      params.zipWithIndex.foreach {
        case (parameter, index) =>
          if (index > 0) {
            o.println(",")
          } else {
            o.println("")
          }

          o.print(s"${indent}${indent}")

          if (config.emitInterfaces) {
            o.print("public ")
          }

          o.print(s"${fieldNaming(parameter.name)}: ${getTypeRefString(parameter.typeRef)}")
      }

      o.println(s"\n${indent}) {")

      params.foreach { parameter =>
        val nme = fieldNaming(parameter.name)

        o.println(s"${indent}${indent}this.${nme} = ${nme}${lineSeparator}")
      }
      o.println(s"${indent}}")

      // Codecs functions
      if (config.emitCodecs.enabled) {
        emitClassCodecs(o, decl)
      }

      o.println("}")
    }
  }

  private def emitClassCodecs(
    o: PrintStream,
    decl: ClassDeclaration): Unit = {
    import decl.{ constructor, name, typeParams }, constructor.parameters

    val tparams = typeParameters(typeParams)

    /* TODO: Review as toJSON/fromJSON,
     - support Date as string, support other class-trait as property
     - Return type { [key: string]: any }
     */

    if (config.fieldNaming == FieldNaming.Identity) {
      // optimized identity

      // Decoder factory: MyClass.fromData({..})
      o.println(s"\n${indent}public static fromData${tparams}(data: any): ${name}${tparams} {")
      o.println(s"${indent}${indent}return <${name}${tparams}>(data)${lineSeparator}")
      o.println(s"${indent}}")

      // Encoder
      o.println(s"\n${indent}public static toData${tparams}(instance: ${name}${tparams}): any {")
      o.println(s"${indent}${indent}return instance${lineSeparator}")
      o.println(s"${indent}}")
    } else {
      // Decoder factory: MyClass.fromData({..})
      o.println(s"\n${indent}public static fromData${tparams}(data: any): ${name}${tparams} {")
      o.print(s"${indent}${indent}return new ${name}${tparams}(")

      val params = list(parameters).zipWithIndex
      val fieldNaming = config.fieldNaming(name, _: String)

      params.foreach {
        case (parameter, index) =>
          val encoded = fieldNaming(parameter.name)

          if (index > 0) o.print(", ")

          o.print(s"data.${encoded}")
      }

      o.println(s")${lineSeparator}")
      o.println(s"${indent}}")

      // Encoder
      o.println(s"\n${indent}public static toData${tparams}(instance: ${name}${tparams}): any {")
      o.println(s"${indent}${indent}return {")

      params.foreach {
        case (parameter, index) =>
          val encoded = fieldNaming(parameter.name)

          if (index > 0) o.print(",\n")

          o.print(s"${indent}${indent}${indent}${encoded}: instance.${fieldNaming(parameter.name)}")
      }

      o.println(s"\n${indent}${indent}}${lineSeparator}")
      o.println(s"${indent}}")
    }
  }

  // ---

  @inline private def typeParameters(params: List[String]): String =
    if (params.isEmpty) "" else params.mkString("<", ", ", ">")

  // TODO: Tuple ~> Update ScalaParser and model accordingly
  private def getTypeRefString(typeRef: TypeRef): String = typeRef match {
    case NumberRef => "number"

    case BooleanRef => "boolean"

    case StringRef => "string"

    case DateRef | DateTimeRef => "Date"

    case ArrayRef(innerType) => s"${getTypeRefString(innerType)}[]"

    case TupleRef(params) =>
      params.map(getTypeRefString).mkString("[", ", ", "]")

    case CustomTypeRef(name, params) if params.isEmpty => name

    case CustomTypeRef(name, params) if params.nonEmpty =>
      s"$name<${params.map(getTypeRefString).mkString(", ")}>"

    case UnknownTypeRef(typeName) => typeName

    case SimpleTypeRef(param) => param

    case UnionType(possibilities) =>
      possibilities.map(getTypeRefString).mkString("(", " | ", ")")

    case MapType(keyType, valueType) =>
      s"{ [key: ${getTypeRefString(keyType)}]: ${getTypeRefString(valueType)} }" // TODO: Unit test

    case NullRef => "null"

    case UndefinedRef => "undefined"
  }

  private def withOut[T](name: String)(f: PrintStream => T): T = {
    lazy val print = out(name)

    try {
      val res = f(print)

      print.flush()

      res
    } finally {
      try {
        print.close()
      } catch {
        case scala.util.control.NonFatal(_) =>
      }
    }
  }
}
