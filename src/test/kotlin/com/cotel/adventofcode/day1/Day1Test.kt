package com.cotel.adventofcode.day1

import io.kotlintest.data.forall
import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec
import io.kotlintest.tables.row

class Day1Test : StringSpec({

  "Fuel required for mass example input" {
    forall(
      row(12, 2),
      row(14, 2),
      row(1969, 654),
      row(100756, 33583)
    ) { mass, result -> fuelRequiredForMass(mass) shouldBe result }
  }

  "Fuel required for fuel example input" {
    forall(
      row(2, 0),
      row(654, 312),
      row(33583, 16763)
    ) { mass, result -> fuelRequiredForFuel(mass) shouldBe result }
  }

  "Fuel required for module example input" {
    forall(
      row(12, 2),
      row(1969, 966),
      row(100756, 50346)
    ) { mass, result -> fuelRequiredForModule(mass) shouldBe result }
  }

})