package com.cotel.adventofcode.day3

import arrow.core.Option
import arrow.core.Tuple2
import arrow.core.extensions.fx
import arrow.core.extensions.list.traverse.sequence
import arrow.core.extensions.option.applicative.applicative
import arrow.core.extensions.option.applicative.just
import arrow.core.fix
import arrow.core.toOption
import arrow.core.toT
import arrow.fx.IO
import arrow.fx.extensions.fx
import arrow.fx.extensions.io.unsafeRun.runBlocking
import arrow.unsafe
import java.io.File
import kotlin.math.abs

fun <A, B> List<A>.scan(init: B, f: (B, A) -> B): List<B> {
  return listOf(init) + (if (isEmpty()) emptyList() else {
    val x = first()
    val xs = drop(1)
    xs.scan(f(init, x), f)
  })
}

fun <A> List<A>.removeAdjacentDuplicates(): List<A> =
  fold(emptyList()) { result, value ->
    if (result.isNotEmpty() && result.last() == value) result
    else result + value
  }

fun generateIntRange(start: Int, end: Int): IntProgression =
  if (start <= end) (start..end) else (start downTo end)

typealias Point = Tuple2<Int, Int>

fun Point.manhattanDistanceTo(point: Point): Int =
  abs(this.a - point.a) + abs(this.b - point.b)

sealed class WireLine(val length: Int) {
  class Right(length: Int) : WireLine(length)
  class Up(length: Int) : WireLine(length)
  class Left(length: Int) : WireLine(length)
  class Down(length: Int) : WireLine(length)
}

// ------- PART 1 -------

fun parseWireLine(str: String): Option<WireLine> {
  val direction = str.getOrNull(0).toOption()
  val length = str.drop(1).toOption()

  return direction.flatMap { char ->
    length.map(String::toInt).flatMap { length ->
      when (char) {
        'R' -> WireLine.Right(length).just()
        'U' -> WireLine.Up(length).just()
        'L' -> WireLine.Left(length).just()
        'D' -> WireLine.Down(length).just()
        else -> Option.empty()
      }
    }
  }
}

fun getWireSteps(fromPoint: Point, lines: List<WireLine>): List<Point> =
  lines.scan(fromPoint) { lastPoint, wireline ->
    val (x, y) = lastPoint
    when (wireline) {
      is WireLine.Right -> (x + wireline.length) toT y
      is WireLine.Up -> x toT (y + wireline.length)
      is WireLine.Left -> (x - wireline.length) toT y
      is WireLine.Down -> x toT (y - wireline.length)
    }
  }

fun getIntermediateSteps(stepA: Point, stepB: Point): List<Point> {
  val (qx, qy) = stepA
  val (px, py) = stepB
  return generateIntRange(qx, px)
    .flatMap { x ->
      generateIntRange(qy, py)
        .map { y -> x toT y }
    }
}

fun expandWireSteps(steps: List<Point>): List<Point> =
  steps.foldIndexed(emptyList()) { index, acc, current ->
    if (index == 0) acc + listOf(current)
    else {
      val previous = steps[index - 1]
      val intermediateSteps = getIntermediateSteps(previous, current)
      acc + intermediateSteps
    }
  }

fun getIntersectionsBetweenWireSteps(xs: List<Point>, ys: List<Point>): Set<Point> =
  xs.intersect(ys)

fun getClosestIntersectionDistanceToPoint(point: Point, intersections: Set<Point>): Option<Int> =
  intersections
    .map(point::manhattanDistanceTo)
    .min()
    .toOption()

fun parseWireLines(str: String): Option<List<WireLine>> =
  str.split(",").map(::parseWireLine).sequence(Option.applicative()).fix().map { it.fix() }

fun getIntersectionsBetweenWires(wireA: String, wireB: String, startingPoint: Point): Option<Set<Point>> = Option.fx {
  val wireAWireLines = parseWireLines(wireA).bind()
  val wireBWireLines = parseWireLines(wireB).bind()

  val wireASteps = expandWireSteps(getWireSteps(startingPoint, wireAWireLines)).removeAdjacentDuplicates()
  val wireBSteps = expandWireSteps(getWireSteps(startingPoint, wireBWireLines)).removeAdjacentDuplicates()

  getIntersectionsBetweenWireSteps(wireASteps, wireBSteps)
}

fun solvePart1(wireA: String, wireB: String, startingPoint: Point): Option<Int> =
  getIntersectionsBetweenWires(wireA, wireB, startingPoint)
    .flatMap { getClosestIntersectionDistanceToPoint(startingPoint, it) }

// ------- PART 2 -------

fun getStepsRequiredForIntersection(
  intersection: Point,
  wireASteps: List<Point>,
  wireBSteps: List<Point>
): Tuple2<Int, Int> {
  val indexOfIntersectionAtWireA = wireASteps.indexOf(intersection)
  val indexOfIntersectionAtWireB = wireBSteps.indexOf(intersection)
  return indexOfIntersectionAtWireA toT indexOfIntersectionAtWireB
}

fun solvePart2(wireA: String, wireB: String): Option<Int> = Option.fx {
  val wireAWireLines = parseWireLines(wireA).bind()
  val wireBWireLines = parseWireLines(wireB).bind()
  val startingPoint = Tuple2(0, 0)

  val wireASteps = expandWireSteps(getWireSteps(startingPoint, wireAWireLines)).removeAdjacentDuplicates()
  val wireBSteps = expandWireSteps(getWireSteps(startingPoint, wireBWireLines)).removeAdjacentDuplicates()

  val intersections = getIntersectionsBetweenWireSteps(wireASteps, wireBSteps).drop(1)
  intersections.map { intersection ->
    val (stepsInA, stepsInB) = getStepsRequiredForIntersection(intersection, wireASteps, wireBSteps)
    stepsInA + stepsInB
  }.min().toOption().bind()
}

// ------- INFRASTRUCTURE -------

suspend fun readProblemInput(): Tuple2<String, String> {
  val file = File(object {}::class.java.getResource("/day3/Day3Input.txt").file)
  val lines = file.readLines()
  return lines[0] toT lines[1]
}

fun main() = unsafe {
  runBlocking {
    IO.fx {
      val (wireA, wireB) = !effect { readProblemInput() }
      val closestDistance = solvePart2(wireA, wireB)

      !effect { println(closestDistance) }
    }
  }
}