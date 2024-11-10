package io.github.scalats.sbt.idtlt

import sbt._
import sbt.Keys._

import _root_.io.github.scalats.idtlt._
import _root_.io.github.scalats.sbt.ScalatsGeneratorPlugin

object ScalatsIdtltPlugin extends AutoPlugin {
  override def requires = ScalatsGeneratorPlugin
  override def trigger = noTrigger

  object autoImport {}

  import ScalatsGeneratorPlugin.autoImport._

  override lazy val projectSettings: Seq[Def.Setting[_]] =
    scalatsAddScalatsDependency(
      Manifest.groupId %% "scala-ts-idtlt" % Manifest.version
    ) ++ Seq(
      // Prelude required by the declaration mapper
      scalatsPrinterPrelude := {
        val imports = """import * as idtlt from 'idonttrustlikethat';
export const _externalDependencyModules = [idtlt];"""

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
      scalatsTypeMappers := Seq(
        // Custom type mapper
        classOf[IdtltTypeMapper]
      ),
      scalatsDeclarationMappers := Seq(
        // Custom declaration mapper (before type mapper)
        classOf[IdtltDeclarationMapper]
      ),
      Compile / scalacOptions ++= Seq(
        "-P:scalats:sys.scala-ts.printer.import-pattern=import * as ns%1$s from '%2$s'"
      ),
      scalatsImportResolvers ++= Seq(
        scalatsUnionWithLiteralSingletonImportResolvers
      ),
      scalatsAdditionalClasspath ++= {
        classOf[IdtltDeclarationMapper].getClassLoader match {
          case cls: java.net.URLClassLoader =>
            cls.getURLs.toSeq.flatMap { url =>
              val repr = url.toString

              if (
                repr.indexOf("sbt-scala-ts-idtlt") != -1 &&
                repr.startsWith("file:")
              ) {
                Seq(new File(url.toURI))
              } else {
                Seq.empty[File]
              }
            }

          case _ =>
            Seq.empty[File]
        }
      }
    )
}
