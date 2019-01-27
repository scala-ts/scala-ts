package org.scalats.core

import java.io.PrintStream

import scala.collection.immutable.ListSet

// TODO: Emit Option (space-lift?)
// TODO: Use a template engine (velocity?)
/**
 * @param out the function to select a `PrintStream` from type name
 */
final class TypeScriptEmitter(
  val config: Configuration,
  out: String => PrintStream) {

  import TypeScriptModel._
  import Internals.list
  import config.{ typescriptIndent => indent }

  // TODO: If for ClassDeclaration or SingletonDeclaration there is values
  // implementing the superInterface, then do not 'implements'
  def emit(declaration: ListSet[Declaration]): Unit =
    list(declaration).foreach { d =>
      d match {
        case decl: InterfaceDeclaration =>
          emitInterfaceDeclaration(decl)

        case decl: EnumDeclaration =>
          emitEnumDeclaration(decl, out)

        case decl: ClassDeclaration =>
          emitClassDeclaration(decl)

        case SingletonDeclaration(name, members, superInterface) =>
          emitSingletonDeclaration(name, members, superInterface)

        case UnionDeclaration(name, fields, possibilities, superInterface) =>
          emitUnionDeclaration(
            name, fields, possibilities, superInterface)
      }

      println()
    }

  // ---

  private def emitUnionDeclaration(
    name: String,
    fields: ListSet[Member],
    possibilities: ListSet[CustomTypeRef],
    superInterface: Option[InterfaceDeclaration]): Unit = {
    val o = out(name)

    // Namespace and union type
    o.println(s"export namespace $name {")
    o.println(s"""${indent}type Union = ${possibilities.map(_.name) mkString " | "};""")

    if (config.emitCodecs) {
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
        o.println(s"${indent}${indent}${indent}${indent}return ${clazz}.fromData(data);")
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
          o.println(s"${indent}${indent}${indent}const data = ${clazz}.toData(instance);")
          o.println(s"""${indent}${indent}${indent}data['$discriminatorName'] = "${naming(sub.name)}";""")
          o.println(s"${indent}${indent}${indent}return data;")
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
    list(fields).foreach { member =>
      o.println(s"${indent}${member.name}: ${getTypeRefString(member.typeRef)};")
    }

    o.println("}")
  }

  private def emitSingletonDeclaration(
    name: String,
    members: ListSet[Member],
    superInterface: Option[InterfaceDeclaration]): Unit = {
    val o = out(name)

    if (members.nonEmpty) {
      def mkString = members.map {
        case Member(nme, tpe) => s"$nme ($tpe)"
      }.mkString(", ")

      throw new IllegalStateException(
        s"Cannot emit static members for singleton values: ${mkString}")
    }

    // Class definition
    o.print(s"export class $name")

    superInterface.filter(_ => members.isEmpty).foreach { i =>
      o.print(s" implements ${i.name}")
    }

    o.println(" {")

    o.println(s"${indent}private static instance: $name;\n")

    o.println(s"${indent}private constructor() {}\n")
    o.println(s"${indent}public static getInstance() {")
    o.println(s"${indent}${indent}if (!${name}.instance) {")
    o.println(s"${indent}${indent}${indent}${name}.instance = new ${name}();")
    o.println(s"${indent}${indent}}\n")
    o.println(s"${indent}${indent}return ${name}.instance;")
    o.println(s"${indent}}")

    if (config.emitCodecs) {
      // Decoder factory: MyClass.fromData({..})
      o.println(s"\n${indent}public static fromData(data: any): ${name} {")
      o.println(s"${indent}${indent}return ${name}.instance;")
      o.println(s"${indent}}")

      // Encoder
      o.println(s"\n${indent}public static toData(instance: ${name}): any {")
      o.println(s"${indent}${indent}return instance;")
      o.println(s"${indent}}")
    }

    o.println("}")
  }

  private def emitInterfaceDeclaration(
    decl: InterfaceDeclaration): Unit = {
    val InterfaceDeclaration(name, fields, typeParams, superInterface) = decl
    val o = out(name)

    o.print(s"export interface $name${typeParameters(typeParams)}")

    superInterface.foreach { iface =>
      o.print(s" extends ${iface.name}")
    }

    o.println(" {")

    list(fields).foreach { member =>
      o.println(s"${indent}${member.name}: ${getTypeRefString(member.typeRef)};")
    }
    o.println("}")
  }

  private def emitEnumDeclaration(
    decl: EnumDeclaration,
    out: PrintStream): Unit = {

    val EnumDeclaration(name, values) = decl

    out.println(s"export enum $name {")

    list(values).foreach { value =>
      out.println(s"${indent}${value} = '${value}',")
    }
    out.println("}")
  }

  private def emitClassDeclaration(
    decl: ClassDeclaration,
    out: PrintStream): Unit = {

    val ClassDeclaration(name, ClassConstructor(parameters),
      values, typeParams, _ /*superInterface*/ ) = decl

    val o = out(name)

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

    list(values).foreach { v =>
      o.print(indent)

      if (config.emitInterfaces) {
        o.print("public ")
      }

      o.println(s"${v.name}: ${getTypeRefString(v.typeRef)};")
    }

    val params = list(parameters)

    if (!config.emitInterfaces) {
      // Class fields
      params.foreach { parameter =>
        o.print(s"${indent}public ${parameter.name}: ${getTypeRefString(parameter.typeRef)};")
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

        o.print(s"${parameter.name}: ${getTypeRefString(parameter.typeRef)}")
    }

    o.println(s"\n${indent}) {")

    params.foreach { parameter =>
      o.println(s"${indent}${indent}this.${parameter.name} = ${parameter.name};")
    }
    o.println(s"${indent}}")

    // Codecs functions
    if (config.emitCodecs) {
      emitClassCodecs(decl)
    }

    o.println("}")
  }

  private def emitClassCodecs(decl: ClassDeclaration): Unit = {
    import decl.{ constructor, name, typeParams }, constructor.parameters

    val o = out(name)
    val tparams = typeParameters(typeParams)

    if (config.fieldNaming == FieldNaming.Identity) {
      // optimized identity

      // Decoder factory: MyClass.fromData({..})
      o.println(s"\n${indent}public static fromData${tparams}(data: any): ${name}${tparams} {")
      o.println(s"${indent}${indent}return <${name}${tparams}>(data);")
      o.println(s"${indent}}")

      // Encoder
      o.println(s"\n${indent}public static toData${tparams}(instance: ${name}${tparams}): any {")
      o.println(s"${indent}${indent}return instance;")
      o.println(s"${indent}}")
    } else {
      // Decoder factory: MyClass.fromData({..})
      o.println(s"\n${indent}public static fromData${tparams}(data: any): ${name}${tparams} {")
      o.print(s"${indent}${indent}return new ${name}${tparams}(")

      val params = list(parameters).zipWithIndex

      params.foreach {
        case (parameter, index) =>
          val encoded = config.fieldNaming(parameter.name)

          if (index > 0) o.print(", ")

          o.print(s"data.${encoded}")
      }

      o.println(s");")
      o.println(s"${indent}}")

      // Encoder
      o.println(s"\n${indent}public static toData${tparams}(instance: ${name}${tparams}): any {")
      o.println(s"${indent}${indent}return {")

      params.foreach {
        case (parameter, index) =>
          val encoded = config.fieldNaming(parameter.name)

          if (index > 0) o.print(",\n")

          o.print(s"${indent}${indent}${indent}${encoded}: instance.${parameter.name}")
      }

      o.println(s"\n${indent}${indent}};")
      o.println(s"${indent}}")
    }
  }

  @inline private def typeParameters(params: ListSet[String]): String =
    if (params.isEmpty) "" else params.mkString("<", ", ", ">")

  private def getTypeRefString(typeRef: TypeRef): String = typeRef match {
    case NumberRef => "number"
    case BooleanRef => "boolean"
    case StringRef => "string"
    case DateRef | DateTimeRef => "Date"
    case ArrayRef(innerType) => s"${getTypeRefString(innerType)}[]"
    case CustomTypeRef(name, params) if params.isEmpty => name
    case CustomTypeRef(name, params) if params.nonEmpty =>
      s"$name<${params.map(getTypeRefString).mkString(", ")}>"
    case UnknownTypeRef(typeName) => typeName
    case SimpleTypeRef(param) => param

    case UnionType(possibilities) =>
      possibilities.map(getTypeRefString).mkString("(", " | ", ")")

    case MapType(keyType, valueType) => s"{ [key: ${getTypeRefString(keyType)}]: ${getTypeRefString(valueType)} }"

    case NullRef => "null"
    case UndefinedRef => "undefined"
  }
}
