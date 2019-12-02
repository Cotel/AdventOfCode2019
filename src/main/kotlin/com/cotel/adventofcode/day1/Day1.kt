package com.cotel.adventofcode.day1

import arrow.fx.IO
import arrow.fx.extensions.fx
import arrow.fx.extensions.io.unsafeRun.runBlocking
import arrow.unsafe
import java.io.File
import kotlin.math.floor
import kotlin.math.max

typealias Mass = Int
typealias Fuel = Int
typealias Spacecraft = List<Mass>

// ------- PART 1 -------

fun fuelRequiredForMass(mass: Mass): Fuel =
  (floor(mass / 3.0).toInt()) - 2

fun fuelForSpacecraft(spacecraft: Spacecraft): Fuel =
  spacecraft.sumBy { mass -> fuelRequiredForMass(mass) }

// ------- PART 2 -------

fun fuelRequiredForFuel(fuel: Int): Fuel =
  when (val newFuel = max(fuelRequiredForMass(fuel), 0)) {
    0 -> newFuel
    else -> newFuel + fuelRequiredForFuel(newFuel)
  }

fun fuelRequiredForModule(module: Mass): Fuel {
  val fuelForMass = fuelRequiredForMass(module)
  val fuelForFuel = fuelRequiredForFuel(fuelForMass)
  return fuelForMass + fuelForFuel
}

fun fuelForSpacecraftIncludingFuel(spacecraft: Spacecraft): Fuel =
  spacecraft.sumBy(::fuelRequiredForModule)

// ------- INFRASTRUCTURE -------

suspend fun readMass(str: String): Mass = str.toInt()

suspend fun readProblemInput(): Spacecraft {
  val file = File(object {}.javaClass.getResource("/day1/Day1Input.txt").file)
  return file.readLines().map { str -> readMass(str) }
}

fun main() = unsafe {
  runBlocking {
    IO.fx {
      val spacecraft = !effect { readProblemInput() }
      val fuelForSpaceCraft = fuelForSpacecraftIncludingFuel(spacecraft)
      !effect { println(fuelForSpaceCraft) }
    }
  }
}