package io.github.scalats.sbt.idtlt

import sbt._
import sbt.Keys._

import _root_.io.github.scalats.idtlt.{ DeclarationMapper, TypeMapper }
import _root_.io.github.scalats.sbt.TypeScriptGeneratorPlugin

object TypeScriptIdtltPlugin extends AutoPlugin {
  override def requires = TypeScriptGeneratorPlugin
  override def trigger = noTrigger

  object autoImport {
  }

  import TypeScriptGeneratorPlugin.autoImport._

  override lazy val projectSettings: Seq[Def.Setting[_]] =
    scalatsAddScalatsDependency(
      Manifest.groupId %% "scala-ts-idtlt" % Manifest.version) ++ Seq(
        // Prelude required by the declaration mapper
        scalatsPrinterPrelude := {
          val imports = "import * as idtlt from 'idonttrustlikethat';"

          scalatsPrinterPrelude.value.map {
            case Left(prelude) =>
              Left(prelude :+ imports)

            case prelude @ Right(url) => {
              println(s"Scala-TS prelude is set to ${url}; Make sure it include the required imports:\r\n\t${imports}")

              prelude
            }
          }.orElse {
            scalatsPrinterInMemoryPrelude(imports)
          }
        },
        scalatsTypeScriptTypeMappers := Seq(
          // Custom type mapper
          classOf[TypeMapper]),
        scalatsTypeScriptDeclarationMappers := Seq(
          // Custom declaration mapper (before type mapper)
          classOf[DeclarationMapper]),
        scalacOptions in Compile ++= Seq(
          "-P:scalats:sys.scala-ts.printer.import-pattern=* as ns%1$s"),
        scalatsTypeScriptImportResolvers ++= Seq(
          scalatsUnionWithLiteralSingletonImportResolvers),
        scalatsAdditionalClasspath ++= {
          classOf[DeclarationMapper].getClassLoader match {
            case cls: java.net.URLClassLoader =>
              cls.getURLs.toSeq.flatMap { url =>
                val repr = url.toString

                if (repr.indexOf("sbt-scala-ts-idtlt") != -1 &&
                  repr.startsWith("file:")) {
                  Seq(new File(url.toURI))
                } else {
                  Seq.empty[File]
                }
              }

            case _ =>
              Seq.empty[File]
          }
        })
}
