package net.verdagon.vale.benchmark

import net.verdagon.vale.driver.FullCompilationOptions
import net.verdagon.vale.{Builtins, FileCoordinateMap, Profiler, RunCompilation, Tests}

import scala.collection.immutable.List

object Benchmark {
  def go(useOptimization: Boolean): Profiler = {
    val profiler = new Profiler()
    val compile =
      new RunCompilation(
        List("", FileCoordinateMap.TEST_MODULE),
        Builtins.getCodeMap()
          .or(FileCoordinateMap.test(Tests.loadExpected("programs/scratch.vale")))
          .or(Tests.getNamespaceToResourceResolver),
        FullCompilationOptions(
          debugOut = (_) => {},
          profiler = profiler,
          useOptimization = useOptimization))
    compile.getHamuts()
    profiler
  }

  def main(args: Array[String]): Unit = {
    println("Starting benchmarking...")
    // Do one run, since the first run always takes longer due to class loading, loading things into the instruction
    // cache, etc.
    // Eventually, we'll want a benchmarking program that does take these things into account, such as by invoking the
    // driver over and over, because class loading, icache, etc are all totally valid things that we care about.
    // For now though, this will do.
    go(true)
    go(false)
    val timesForOldAndNew = (0 until 10).map(_ => (go(false).totalNanoseconds, go(true).totalNanoseconds))
    val timesForOld = timesForOldAndNew.map(_._1)
    val timesForNew = timesForOldAndNew.map(_._2)
    val averageTimeForOld = timesForOld.sum / timesForOld.size
    val averageTimeForNew = timesForNew.sum / timesForNew.size
    println("Done benchmarking! Old: " + averageTimeForOld + ", New: " + averageTimeForNew)

//    println(go(true).assembleResults())
  }
}
