package com.cotel.adventofcode.day2

import arrow.core.ListK
import arrow.core.Option
import arrow.core.Tuple2
import arrow.core.extensions.fx
import arrow.core.extensions.option.applicative.just
import arrow.core.getOrElse
import arrow.core.k
import arrow.core.toT
import arrow.fx.IO
import arrow.fx.extensions.fx
import arrow.fx.extensions.io.unsafeRun.runBlocking
import arrow.optics.extensions.listk.index.index
import arrow.unsafe
import java.io.File

data class IntcodeInstruction(
  val opcode: Opcode,
  val inputPos1: Int,
  val inputPos2: Int,
  val outputPos: Int
)

sealed class Opcode {
  object Sum : Opcode()
  object Multiply: Opcode()
  object Halt : Opcode()
}

typealias IntcodeProgram = List<Int>

// ------ PART 1 ------

fun parseInstruction(listRow: ListK<Int>): Option<IntcodeInstruction> = Option.fx {
  val parsedOpCode = when (Option.fromNullable(listRow.getOrNull(0)).bind()) {
    1 -> Opcode.Sum.just().bind()
    2 -> Opcode.Multiply.just().bind()
    99 -> Opcode.Halt.just().bind()
    else -> Option.empty<Opcode>().bind()
  }

  if (parsedOpCode == Opcode.Halt) IntcodeInstruction(parsedOpCode, 0, 0, 0)
  else {
    val inputPos1 = Option.fromNullable(listRow.getOrNull(1)).bind()
    val inputPos2 = Option.fromNullable(listRow.getOrNull(2)).bind()
    val outputPos = Option.fromNullable(listRow.getOrNull(3)).bind()

    IntcodeInstruction(parsedOpCode, inputPos1, inputPos2, outputPos)
  }
}

fun executeInstruction(instruction: IntcodeInstruction, program: IntcodeProgram): Tuple2<Boolean, IntcodeProgram> {
  val (opcode, inputPos1, inputPos2, outputPos) = instruction
  if (opcode == Opcode.Halt) {
    return false toT program
  }

  val input1 = program[inputPos1]
  val input2 = program[inputPos2]

  val output = if (opcode == Opcode.Sum) input1 + input2 else input1 * input2
  val outputPosListItem = ListK.index<Int>().index(outputPos)

  val newProgram = outputPosListItem.set(program.k(), output)
  return true toT newProgram
}

fun processProgram(program: IntcodeProgram): Option<IntcodeProgram> {
  fun step(program: IntcodeProgram, index: Int = 0): Option<IntcodeProgram> = Option.fx {
    val nextProgramRow = Option.fromNullable(program.chunked(4).getOrNull(index)).bind().k()
    val instruction = parseInstruction(nextProgramRow).bind()
    val (shouldContinue, newProgram) = executeInstruction(instruction, program)
    if (shouldContinue) step(newProgram, index + 1).bind()
    else program
  }

  return step(program)
}

// ------ PART 2 ------

private const val VALUE_TO_FIND = 19_690_720
fun findNounVerbCombination(originalProgram: IntcodeProgram): Option<Tuple2<Int, Int>> {
  (0..99).forEach { noun ->
    (0..99).forEach { verb ->
      val listPos1 = ListK.index<Int>().index(1)
      val listPos2 = ListK.index<Int>().index(2)

      val alteredProgram = listPos2.set(listPos1.set(originalProgram.k(), noun), verb)
      val programResult = processProgram(alteredProgram).getOrElse { emptyList() }
      if (programResult.firstOrNull() == VALUE_TO_FIND) return (noun toT verb).just()
    }
  }

  return Option.empty()
}

// ------ INFRASTRUCTURE ------

suspend fun readProblemInput(): IntcodeProgram {
  val file = File(object {}.javaClass.getResource("/day2/Day2Input.txt").file)
  return file.readText().split(",").map { str -> str.toInt() }
}

fun main() = unsafe {
  runBlocking { IO.fx {
    val program = !effect { readProblemInput().k() }
    val (noun, verb) = findNounVerbCombination(program).getOrElse { 0 toT 0 }
    println(100 * noun + verb)
  } }
}