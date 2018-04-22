package com.mpc.scalats.core

import java.io.PrintStream

import scala.collection.immutable.ListSet

import com.mpc.scalats.configuration.{ Config, FieldNaming }

// TODO: Emit Option (space-lift?)
// TODO: Use a template engine (velocity?)
final class TypeScriptEmitter(val config: Config) {
  import TypeScriptModel._
  import Internals.list
  import config.{ typescriptIndent => indent }

  // TODO: If for ClassDeclaration or SingletonDeclaration there is values
  // implementing the superInterface, then do not 'implements'
  def emit(declaration: ListSet[Declaration], out: PrintStream): Unit = {
    list(declaration).foreach { d =>
      d match {
        case decl: InterfaceDeclaration =>
          emitInterfaceDeclaration(decl, out)

        case decl: ClassDeclaration =>
          emitClassDeclaration(decl, out)

        case SingletonDeclaration(name, members, superInterface) =>
          emitSingletonDeclaration(name, members, superInterface, out)

        case UnionDeclaration(name, fields, possibilities, superInterface) =>
          emitUnionDeclaration(
            name, fields, possibilities, superInterface, out)
      }

      println()
    }
  }

  // ---

  private def emitUnionDeclaration(
    name: String,
    fields: ListSet[Member],
    possibilities: ListSet[CustomTypeRef],
    superInterface: Option[InterfaceDeclaration],
    out: PrintStream): Unit = {

    // Namespace and union type
    out.println(s"export namespace $name {")
    out.println(s"""${indent}type Union = ${possibilities.map(_.name) mkString " | "};""")

    if (config.emitCodecs) {
      // TODO: Discriminator naming
      val discriminatorName = "_type"
      val naming: String => String = identity[String](_)
      val children = list(possibilities)

      // Decoder factory: MyClass.fromData({..})
      out.println(s"\n${indent}public static fromData(data: any): ${name} {")
      out.println(s"${indent}${indent}switch (data.${discriminatorName}) {")

      children.foreach { sub =>
        val clazz = if (sub.name startsWith "I") sub.name.drop(1) else sub.name

        out.println(s"""${indent}${indent}${indent}case "${naming(sub.name)}": {""")
        out.println(s"${indent}${indent}${indent}${indent}return ${clazz}.fromData(data);")
        out.println(s"${indent}${indent}${indent}}")
      }

      out.println(s"${indent}${indent}}")
      out.println(s"${indent}}")

      // Encoder
      out.println(s"\n${indent}public static toData(instance: ${name}): any {")

      children.zipWithIndex.foreach {
        case (sub, index) =>
          out.print(s"${indent}${indent}")

          if (index > 0) {
            out.print("} else ")
          }

          val clazz =
            if (sub.name startsWith "I") sub.name.drop(1) else sub.name

          out.println(s"if (instance instanceof ${sub.name}) {")
          out.println(s"${indent}${indent}${indent}const data = ${clazz}.toData(instance);")
          out.println(s"""${indent}${indent}${indent}data['$discriminatorName'] = "${naming(sub.name)}";""")
          out.println(s"${indent}${indent}${indent}return data;")
      }

      out.println(s"${indent}${indent}}")
      out.println(s"${indent}}")
    }

    out.println("}")

    // Union interface
    out.print(s"\nexport interface I${name}")

    superInterface.foreach { iface =>
      out.print(s" extends I${iface.name}")
    }

    out.println(" {")

    // Abstract fields - common to all the subtypes
    list(fields).foreach { member =>
      out.println(s"${indent}${member.name}: ${getTypeRefString(member.typeRef)};")
    }

    out.println("}")
  }

  private def emitSingletonDeclaration(
    name: String,
    members: ListSet[Member],
    superInterface: Option[InterfaceDeclaration],
    out: PrintStream): Unit = {

    if (members.nonEmpty) {
      def mkString = members.map {
        case Member(nme, tpe) => s"$nme ($tpe)"
      }.mkString(", ")

      throw new IllegalStateException(
        s"Cannot emit static members for singleton values: ${mkString}")
    }

    // Class definition
    out.print(s"export class $name")

    superInterface.filter(_ => members.isEmpty).foreach { i =>
      out.print(s" implements ${i.name}")
    }

    out.println(" {")

    out.println(s"${indent}private static instance: $name;\n")

    out.println(s"${indent}private constructor() {}\n")
    out.println(s"${indent}public static getInstance() {")
    out.println(s"${indent}${indent}if (!${name}.instance) {")
    out.println(s"${indent}${indent}${indent}${name}.instance = new ${name}();")
    out.println(s"${indent}${indent}}\n")
    out.println(s"${indent}${indent}return ${name}.instance;")
    out.println(s"${indent}}")

    if (config.emitCodecs) {
      // Decoder factory: MyClass.fromData({..})
      out.println(s"\n${indent}public static fromData(data: any): ${name} {")
      out.println(s"${indent}${indent}return ${name}.instance;")
      out.println(s"${indent}}")

      // Encoder
      out.println(s"\n${indent}public static toData(instance: ${name}): any {")
      out.println(s"${indent}${indent}return instance;")
      out.println(s"${indent}}")
    }

    out.println("}")
  }

  private def emitInterfaceDeclaration(
    decl: InterfaceDeclaration,
    out: PrintStream): Unit = {

    val InterfaceDeclaration(name, fields, typeParams, superInterface) = decl

    out.print(s"export interface $name${typeParameters(typeParams)}")

    superInterface.foreach { iface =>
      out.print(s" extends ${iface.name}")
    }

    out.println(" {")
    
    list(fields).foreach { member =>
      out.println(s"${indent}${member.name}: ${getTypeRefString(member.typeRef)};")
    }
    out.println("}")
  }

  private def emitClassDeclaration(
    decl: ClassDeclaration,
    out: PrintStream): Unit = {

    val ClassDeclaration(name, ClassConstructor(parameters),
      values, typeParams, _/*superInterface*/) = decl

    val tparams = typeParameters(typeParams)

    if (values.nonEmpty) {
      def mkString = values.map {
        case Member(nme, tpe) => s"$nme ($tpe)"
      }.mkString(", ")

      throw new IllegalStateException(
        s"Cannot emit static members for class values: ${mkString}")
    }

    // Class definition
    out.print(s"export class ${name}${tparams}")

    if (config.emitInterfaces) {
      out.print(s" implements I${name}${tparams}")
    }

    out.println(" {")

    list(values).foreach { v =>
      out.print(indent)

      if (config.emitInterfaces) {
        out.print("public ")
      }

      out.println(s"${v.name}: ${getTypeRefString(v.typeRef)};")
    }

    val params = list(parameters)

    if (!config.emitInterfaces) {
      // Class fields
      params.foreach { parameter =>
        out.print(s"${indent}public ${parameter.name}: ${getTypeRefString(parameter.typeRef)};")
      }
    }

    // Class constructor
    out.print(s"${indent}constructor(")

    params.zipWithIndex.foreach {
      case (parameter, index) =>
        if (index > 0) {
          out.println(",")
        } else {
          out.println("")
        }

        out.print(s"${indent}${indent}")

        if (config.emitInterfaces) {
          out.print("public ")
        }

        out.print(s"${parameter.name}: ${getTypeRefString(parameter.typeRef)}")
    }

    out.println(s"\n${indent}) {")

    params.foreach { parameter =>
      out.println(s"${indent}${indent}this.${parameter.name} = ${parameter.name};")
    }
    out.println(s"${indent}}")

    // Codecs functions
    if (config.emitCodecs) {
      emitClassCodecs(decl, out)
    }

    out.println("}")
  }

  private def emitClassCodecs(
    decl: ClassDeclaration,
    out: PrintStream): Unit = {
    import decl.{ constructor, name, typeParams }, constructor.parameters
    val tparams = typeParameters(typeParams)

    if (config.fieldNaming == FieldNaming.Identity) {
      // optimized identity

      // Decoder factory: MyClass.fromData({..})
      out.println(s"\n${indent}public static fromData${tparams}(data: any): ${name}${tparams} {")
      out.println(s"${indent}${indent}return <${name}${tparams}>(data);")
      out.println(s"${indent}}")

      // Encoder
      out.println(s"\n${indent}public static toData${tparams}(instance: ${name}${tparams}): any {")
      out.println(s"${indent}${indent}return instance;")
      out.println(s"${indent}}")
    } else {
      // Decoder factory: MyClass.fromData({..})
      out.println(s"\n${indent}public static fromData${tparams}(data: any): ${name}${tparams} {")
      out.print(s"${indent}${indent}return new ${name}${tparams}(")

      val params = list(parameters).zipWithIndex

      params.foreach {
        case (parameter, index) =>
          val encoded = config.fieldNaming(parameter.name)

          if (index > 0) out.print(", ")

          out.print(s"data.${encoded}")
      }

      out.println(s");")
      out.println(s"${indent}}")

      // Encoder
      out.println(s"\n${indent}public static toData${tparams}(instance: ${name}${tparams}): any {")
      out.println(s"${indent}${indent}return {")

      params.foreach {
        case (parameter, index) =>
          val encoded = config.fieldNaming(parameter.name)

          if (index > 0) out.print(",\n")

          out.print(s"${indent}${indent}${indent}${encoded}: instance.${parameter.name}")
      }

      out.println(s"\n${indent}${indent}};")
      out.println(s"${indent}}")
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
