# scala-ts

Fork of scala-ts

Proposes an alternative way of doing thing in the scala parsing phase to scala-ts, notably for descending sealed trait hierarchies.

It's tailored internally for Elium usage, notably interfaces will be prefixed with IElium.

If this fork gains popularity, we can make it more configurable for general usage.

### Usage

Usage should look like this (in build.sbt) :

```scala
lazy val tsTask = inputKey[Unit]("Typings generation")
//Sealed trait FIRST, non reached classes afterwards. Only need to add root traits and classes.
val generatedClasses = Seq(
    "com.mycompany.MySealedTrait1",
    "com.mycompany.MyClass1"
  )
  
val tsTaskSettings = Seq(
 outputStream in generateTypeScript := Some(new PrintStream(new File("folderForOutput", "fileToOutput.ts"))),
  tsTask := {
      generateTypeScript.fullInput(generatedClasses.mkString(" ", " ", "")).evaluated
    }
)

val myProject = project.settings(/*allOtherSettings,*/ tsTaskSettings)
```
  


### Config options

- `interfacePrefix: String = "IElium"`, - how to prefix interface names : MyCaseClass will be emitted as s"${interfacePrefix}MyCaseClass". Can be "" for no prefix.
- `optionToNullable: Boolean = true`, - emit optional types as type | null
- `optionToUndefined: Boolean = false`, - emit optional types as type?
- `outputStream: Option[PrintStream] = None`, - where to print the result
- `customNameMap: Map[String, String] = Map("Metric"->"string")`  - Custom name mappings for specific class names. Doesn't allow to override standard types like Int, String, ... . Fully override the interfacePrefix setting.
- `leafTypes: Set[String] = Set.empty` Forced leaf types for parsing. The parser won't explore involved types for those types, and won't emit them. They'll still be used as member types where they are involved.
