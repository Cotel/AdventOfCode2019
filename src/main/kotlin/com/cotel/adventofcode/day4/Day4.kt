package com.cotel.adventofcode.day4

import arrow.Kind
import arrow.core.Either
import arrow.core.EitherPartialOf
import arrow.core.Nel
import arrow.core.extensions.either.applicativeError.applicativeError
import arrow.core.extensions.either.monadError.monadError
import arrow.core.extensions.list.monadFilter.filterMap
import arrow.core.fix
import arrow.core.nel
import arrow.typeclasses.ApplicativeError
import arrow.typeclasses.MonadError
import com.cotel.adventofcode.day3.removeAdjacentDuplicates

typealias Password = Int

sealed class PasswordError {
  object NoAdjacentDigits : PasswordError()
  object LeftToRightDecrease : PasswordError()
  object NoAdjacentOrBigGroups : PasswordError()
}

sealed class Rules<F>(ME: MonadError<F, Nel<PasswordError>>) : MonadError<F, Nel<PasswordError>> by ME {
  private fun Password.leftToRightIncrease(): Kind<F, Password> {
    val charArray = this.toString().toCharArray()
    return if (charArray.sortedArray().contentEquals(charArray)) just(this)
    else raiseError(PasswordError.LeftToRightDecrease.nel())
  }

  private fun Password.adjacentDigits(): Kind<F, Password> {
    val charsList = this.toString().toCharArray().toList()
    return if (charsList.removeAdjacentDuplicates() != charsList) just(this)
    else raiseError(PasswordError.NoAdjacentDigits.nel())
  }

  private fun Password.hasAPairOfDigits(): Kind<F, Password> {
    val charsList = this.toString().toCharArray()
    return if (charsList.groupBy { it }.map { (_, v) -> v }.count { it.size == 2 } >= 1) just(this)
    else raiseError(PasswordError.NoAdjacentOrBigGroups.nel())
  }

  fun Password.validatePassword(): Kind<F, Password> = map(adjacentDigits(), leftToRightIncrease()) { this }

  fun Password.validatePasswordPart2(): Kind<F, Password> =
    leftToRightIncrease().flatMap { password ->
      password.hasAPairOfDigits()
    }

  object FailFastStrategy : Rules<EitherPartialOf<Nel<PasswordError>>>(Either.monadError())

  companion object {
    infix fun <A> failFast(f: FailFastStrategy.() -> A): A = f(FailFastStrategy)
  }
}

fun getValidPasswordsCountWithinRange(intRange: IntRange): Int {
  val validations = intRange.map { password ->
    Rules failFast { password.validatePassword().fix() }
  }

  return validations.filterMap { it.toOption() }.size
}

fun getValidPasswordsCountWithinRangePart2(intRange: IntRange): Int {
  val validations = intRange.map { password ->
    Rules failFast { password.validatePasswordPart2().fix() }
  }

  return validations.filterMap { it.toOption() }.size
}

fun main() {
    val problemInput = (165432..707912)

    val possiblePasswords = getValidPasswordsCountWithinRangePart2(problemInput)
    println(possiblePasswords)
}