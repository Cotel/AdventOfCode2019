package com.cotel.adventofcode.day2

import arrow.core.Option
import arrow.core.Tuple2
import arrow.core.k
import io.kotlintest.properties.forAll
import io.kotlintest.properties.Gen
import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec

class Day2Test : StringSpec() {
  init {
    "Parse instruction should return empty instruction if opcode is Halt" {
      forAll(Gen.int(), Gen.int(), Gen.int()) { inputPos1, inputPos2, outputPos ->
        val listRow = listOf(99, inputPos1, inputPos2, outputPos).k()
        parseInstruction(listRow) == Option.just(IntcodeInstruction(Opcode.Halt, 0, 0, 0))
      }
    }

    "Parse instruction should return None if opcode is not recognised" {
      forAll(NotOpcodeGen(), Gen.int(), Gen.int(), Gen.int()) { opCode, inputPos1, inputPos2, outputPos ->
        val listRow = listOf(opCode, inputPos1, inputPos2, outputPos).k()
        parseInstruction(listRow) == Option.empty<IntcodeInstruction>()
      }
    }

    "Execute instruction should always return false if opcode is Halt" {
      forAll(HaltInstructionGenerator()) { instruction ->
        executeInstruction(instruction, emptyList()) == Tuple2(false, emptyList<Int>())
      }
    }

    "Execute instruction Sum should sum inputs and store them in program" {
      val sumInstruction = IntcodeInstruction(Opcode.Sum, 0, 1, 4)
      val program = listOf(1, 0, 1, 4, 99)
      executeInstruction(sumInstruction, program) shouldBe Tuple2(true, listOf(1, 0, 1, 4, 1))
    }

    "Execute instruction Multiply should multiply inputs and store them in program" {
      val sumInstruction = IntcodeInstruction(Opcode.Multiply, 0, 1, 4)
      val program = listOf(2, 0, 1, 4, 99)
      executeInstruction(sumInstruction, program) shouldBe Tuple2(true, listOf(2, 0, 1, 4, 0))
    }
  }

  class NotOpcodeGen : Gen<Int> {
    override fun constants(): Iterable<Int> = Gen.int().constants()
    override fun random(): Sequence<Int> = Gen.int().random().filter { it != 1 && it != 2 && it != 99 }
  }

  class HaltInstructionGenerator : Gen<IntcodeInstruction> {
    override fun constants(): Iterable<IntcodeInstruction> = emptyList()
    override fun random(): Sequence<IntcodeInstruction> = generateSequence {
      IntcodeInstruction(
        Opcode.Halt,
        Gen.int().random().first(),
        Gen.int().random().first(),
        Gen.int().random().first()
      )
    }
  }
}