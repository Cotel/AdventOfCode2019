package com.cotel.adventofcode.day4

import arrow.core.fix
import arrow.core.nel
import io.kotlintest.assertions.arrow.either.shouldBeLeft
import io.kotlintest.assertions.arrow.either.shouldBeRight
import io.kotlintest.data.suspend.forall
import io.kotlintest.properties.Gen
import io.kotlintest.properties.forAll
import io.kotlintest.specs.StringSpec
import io.kotlintest.tables.row

class Day4Test : StringSpec() {
  init {
    "If Password's digits decrease from left to right it will always fail" {
      forall(
        row(214456),
        row(124465),
        row(111121),
        row(122354)
      ) { password: Password ->
        val validationResult = Rules failFast { password.validatePassword().fix() }
        validationResult.shouldBeLeft(PasswordError.LeftToRightDecrease.nel())
      }
    }

    "If there are no adjacent repeated digits it will always fail" {
      forall(
        row(123456),
        row(121343),
        row(123451)
      ) { password ->
        val validationResult = Rules failFast { password.validatePassword().fix() }
        validationResult.shouldBeLeft(PasswordError.NoAdjacentDigits.nel())
      }
    }

    "If there are big groups of adjacent repeated digits it will always fail" {
      forall(
        row(111222),
        row(111112),
        row(123444),
        row(123334),
        row(123456)
      ) { password ->
        val validationResult = Rules failFast { password.validatePasswordPart2().fix() }
        validationResult.shouldBeLeft(PasswordError.NoAdjacentOrBigGroups.nel())
      }
    }

    "If there big groups but also a pair of repeated digits it will success" {
      forall(
        row(112222),
        row(133344),
        row(123455)
      ) { password ->
        val validationResult = Rules failFast { password.validatePasswordPart2().fix() }
        validationResult.shouldBeRight()
      }
    }
  }
}