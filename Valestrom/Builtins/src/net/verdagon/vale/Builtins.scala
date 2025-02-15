package net.verdagon.vale

import scala.io.Source

object Builtins {
  val NAMESPACE_COORD = NamespaceCoordinate("", List())

  val moduleToFilename =
    Map(
      "arith" -> "arith.vale",
      "logic" -> "logic.vale",
      "str" -> "str.vale",
      "main" -> "main.vale",
      "as" -> "as.vale",
      "print" -> "print.vale",
      "panic" -> "panic.vale",
      "opt" -> "opt.vale",
      "sameinstance" -> "sameinstance.vale")

  def load(resourceFilename: String): String = {
    val stream = getClass().getClassLoader().getResourceAsStream(resourceFilename)
    vassert(stream != null)
    val source = Source.fromInputStream(stream)
    vassert(source != null)
    source.mkString("")
  }

  // Modulized is a made up word, it means we're pretending the builtins are in different modules.
  // This lets tests import only certain kinds of builtins.
  // The more basic foundational tests will choose not to import any builtins, so they can test the
  // bare minimum. For example, the most basic test is `fn main() int { 42 }`, and we don't want it
  // to fail just because the builtin-yet-unused `fn as<T, X>(x X) Opt<T> { ... }` doesn't want to
  // work right now.
  def getModulizedCodeMap(): FileCoordinateMap[String] = {
    moduleToFilename.foldLeft(FileCoordinateMap[String](Map()))({
      case (prev, (moduleName, filename)) => {
        prev.add("v", List("builtins", moduleName), filename, load(filename))
      }
    })
  }

  // Add an empty v.builtins.whatever so that the aforementioned imports still work.
  // But load the actual files all inside the root namespace.
  def getCodeMap(): FileCoordinateMap[String] = {
    moduleToFilename.foldLeft(FileCoordinateMap[String](Map()))({
      case (prev, (moduleName, filename)) => {
        prev
          .add("v", List("builtins", moduleName), filename, "")
          .add(NAMESPACE_COORD.module, NAMESPACE_COORD.namespaces, filename, load(filename))
      }
    })
  }
}
