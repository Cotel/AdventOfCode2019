package com.cotel.adventofcode.day7

import arrow.core.ListK
import arrow.core.Option
import arrow.core.extensions.fx
import arrow.core.extensions.list.monadFilter.filterMap
import arrow.core.extensions.option.applicative.just
import arrow.core.getOrElse
import arrow.core.k
import arrow.core.toOption
import arrow.fx.IO
import arrow.fx.extensions.fx
import arrow.fx.extensions.io.unsafeRun.runBlocking
import arrow.optics.extensions.listk.index.index
import arrow.unsafe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import java.io.File

// Ampliation for Day 2

data class IntcodeInstruction(
  val opcode: Opcode,
  val input1: Int,
  val input2: Int,
  val outputPos: Int
)

sealed class ParameterMode {
  object Position : ParameterMode()
  object Immediate : ParameterMode()
}

sealed class Opcode {
  sealed class Arithmetic(val param1Mode: ParameterMode, val param2Mode: ParameterMode) : Opcode() {
    class Sum(param1Mode: ParameterMode, param2Mode: ParameterMode) : Arithmetic(param1Mode, param2Mode)
    class Multiply(param1Mode: ParameterMode, param2Mode: ParameterMode) : Arithmetic(param1Mode, param2Mode)
  }

  sealed class Jump(val param1Mode: ParameterMode, val param2Mode: ParameterMode) : Opcode() {
    class JumpIfTrue(param1Mode: ParameterMode, param2Mode: ParameterMode) : Jump(param1Mode, param2Mode)
    class JumpIfFalse(param1Mode: ParameterMode, param2Mode: ParameterMode) : Jump(param1Mode, param2Mode)
  }

  sealed class ConditionalStore(val param1Mode: ParameterMode, val param2Mode: ParameterMode) : Opcode() {
    class LessThan(param1Mode: ParameterMode, param2Mode: ParameterMode) : ConditionalStore(param1Mode, param2Mode)
    class Equals(param1Mode: ParameterMode, param2Mode: ParameterMode) : ConditionalStore(param1Mode, param2Mode)
  }

  object Store : Opcode()
  class Output(val param1Mode: ParameterMode) : Opcode()
  object Halt : Opcode()
}

typealias IntcodeProgram = List<Int>

data class ProgramState constructor(
  val program: IntcodeProgram,
  val inputs: List<Int>,
  val headerPosition: Int = 0,
  val output: Option<Int> = Option.empty()
)

fun parseInstruction(headerPosition: Int, program: IntcodeProgram): Option<IntcodeInstruction> = Option.fx {
  val rawOpcode = program.getOrNull(headerPosition).toOption().bind()
  val rawOpcodeStr = rawOpcode.toString().padStart(5, '0')

  val parseParamMode = { char: Char -> if (char == '0') ParameterMode.Position else ParameterMode.Immediate }

  val param2Mode = parseParamMode(rawOpcodeStr[1])
  val param1Mode = parseParamMode(rawOpcodeStr[2])

  val opcode = when {
    rawOpcodeStr.endsWith("99") -> Opcode.Halt.just().bind()
    rawOpcodeStr.endsWith("1") -> Opcode.Arithmetic.Sum(param1Mode, param2Mode).just().bind()
    rawOpcodeStr.endsWith("2") -> Opcode.Arithmetic.Multiply(param1Mode, param2Mode).just().bind()
    rawOpcodeStr.endsWith("3") -> Opcode.Store.just().bind()
    rawOpcodeStr.endsWith("4") -> Opcode.Output(param1Mode).just().bind()
    rawOpcodeStr.endsWith("5") -> Opcode.Jump.JumpIfTrue(param1Mode, param2Mode).just().bind()
    rawOpcodeStr.endsWith("6") -> Opcode.Jump.JumpIfFalse(param1Mode, param2Mode).just().bind()
    rawOpcodeStr.endsWith("7") -> Opcode.ConditionalStore.LessThan(param1Mode, param2Mode).just().bind()
    rawOpcodeStr.endsWith("8") -> Opcode.ConditionalStore.Equals(param1Mode, param2Mode).just().bind()
    else -> Option.empty<Opcode>().bind()
  }

  val param1 = program.getOrNull(headerPosition + 1).toOption().bind()
  val param2 = program.getOrNull(headerPosition + 2).toOption().bind()
  val param3 = program.getOrNull(headerPosition + 3).toOption().bind()

  when (opcode) {
    is Opcode.Halt -> IntcodeInstruction(opcode, 0, 0, 0)
    is Opcode.Arithmetic.Sum -> IntcodeInstruction(opcode, param1, param2, param3)
    is Opcode.Arithmetic.Multiply -> IntcodeInstruction(opcode, param1, param2, param3)
    is Opcode.Store -> IntcodeInstruction(opcode, param1, 0, 0)
    is Opcode.Output -> IntcodeInstruction(opcode, param1, 0, 0)
    is Opcode.Jump.JumpIfTrue -> IntcodeInstruction(opcode, param1, param2, 0)
    is Opcode.Jump.JumpIfFalse -> IntcodeInstruction(opcode, param1, param2, 0)
    is Opcode.ConditionalStore.LessThan -> IntcodeInstruction(opcode, param1, param2, param3)
    is Opcode.ConditionalStore.Equals -> IntcodeInstruction(opcode, param1, param2, param3)
  }
}

private inline fun executeArithmeticInstruction(
  instruction: IntcodeInstruction,
  headerPosition: Int,
  state: ProgramState,
  op: (Int, Int) -> Int
): ProgramState {
  require(instruction.opcode is Opcode.Arithmetic)
  val param1 = if (instruction.opcode.param1Mode == ParameterMode.Immediate) instruction.input1
  else state.program[instruction.input1]
  val param2 = if (instruction.opcode.param2Mode == ParameterMode.Immediate) instruction.input2
  else state.program[instruction.input2]

  val result = op(param1, param2)
  val listPositionUpdate = ListK.index<Int>().index(instruction.outputPos)

  return state.copy(
    headerPosition = headerPosition + 4,
    program = listPositionUpdate.set(state.program.k(), result)
  )
}

private inline fun executeJumpInstruction(
  instruction: IntcodeInstruction,
  headerPosition: Int,
  state: ProgramState,
  comparison: (Int) -> Boolean
): ProgramState {
  require(instruction.opcode is Opcode.Jump)
  val param1 = if (instruction.opcode.param1Mode == ParameterMode.Immediate) instruction.input1
  else state.program[instruction.input1]
  val param2 = if (instruction.opcode.param2Mode == ParameterMode.Immediate) instruction.input2
  else state.program[instruction.input2]

  return if (comparison(param1))
    state.copy(headerPosition = param2)
  else
    state.copy(headerPosition = headerPosition + 3)
}

private inline fun executeConditionalStoreInstruction(
  instruction: IntcodeInstruction,
  headerPosition: Int,
  state: ProgramState,
  comparison: (Int, Int) -> Boolean
): ProgramState {
  require(instruction.opcode is Opcode.ConditionalStore)
  val param1 = if (instruction.opcode.param1Mode == ParameterMode.Immediate) instruction.input1
  else state.program[instruction.input1]
  val param2 = if (instruction.opcode.param2Mode == ParameterMode.Immediate) instruction.input2
  else state.program[instruction.input2]

  val listPositionUpdate = ListK.index<Int>().index(instruction.outputPos)
  val storingValue = if (comparison(param1, param2)) 1 else 0

  return state.copy(
    headerPosition = headerPosition + 4,
    program = listPositionUpdate.set(state.program.k(), storingValue)
  )
}

suspend fun executeInstruction(
  instruction: IntcodeInstruction,
  headerPosition: Int,
  state: ProgramState
): ProgramState =
  when (instruction.opcode) {
    is Opcode.Halt -> state.copy(headerPosition = -1)
    is Opcode.Arithmetic -> {
      if (instruction.opcode is Opcode.Arithmetic.Sum)
        executeArithmeticInstruction(instruction, headerPosition, state, Int::plus)
      else
        executeArithmeticInstruction(instruction, headerPosition, state, Int::times)
    }
    is Opcode.Jump -> {
      if (instruction.opcode is Opcode.Jump.JumpIfTrue)
        executeJumpInstruction(instruction, headerPosition, state) { it != 0 }
      else
        executeJumpInstruction(instruction, headerPosition, state) { it == 0 }
    }
    is Opcode.ConditionalStore -> {
      if (instruction.opcode is Opcode.ConditionalStore.LessThan)
        executeConditionalStoreInstruction(instruction, headerPosition, state) { a, b -> a < b }
      else
        executeConditionalStoreInstruction(instruction, headerPosition, state) { a, b -> a == b }
    }
    is Opcode.Store -> {
      val listPositionUpdate = ListK.index<Int>().index(instruction.input1)
      val input = state.inputs.firstOrNull() ?: 0
      val rest = state.inputs.drop(1) + 0

      state.copy(
        headerPosition = headerPosition + 2,
        inputs = rest,
        program = listPositionUpdate.set(state.program.k(), input)
      )
    }
    is Opcode.Output -> {
      val param1 = if (instruction.opcode.param1Mode == ParameterMode.Immediate) instruction.input1
      else state.program[instruction.input1]

      state.copy(
        headerPosition = headerPosition + 2,
        output = param1.just()
      )
    }
  }

tailrec suspend fun executeProgram(state: ProgramState, headerPosition: Int = 0): ProgramState {
  val (program, inputs, _, output) = state

  val modifiedInputsState = output
    .map { output -> state.copy(
      inputs = inputs.apply { toMutableList().add(1, output) },
      output = Option.empty())
    }
    .getOrElse { state }

  val instruction = parseInstruction(headerPosition, program)
    .getOrElse { IntcodeInstruction(Opcode.Halt, 0, 0, 0) }

  val newState = executeInstruction(instruction, headerPosition, modifiedInputsState)

  return if (newState.headerPosition == -1) newState
  else executeProgram(newState, newState.headerPosition)
}

private suspend fun readProblemInput(): IntcodeProgram {
  val file = File(object {}::class.java.getResource("/day7/Day7Input.txt").file)
  return file.readText().split(",").map { it.toInt() }
}

private fun generateInputSignals(): List<List<Int>> =
  (0..99999).map { i ->
    i.toString()
      .padStart(5, '0')
      .toCharArray()
      .map { it.toString().toInt() }
  }

suspend fun executeProgramsForInputs(inputs: List<List<Int>>, program: IntcodeProgram): List<ProgramState> =
  inputs.map { input ->
    val state = ProgramState(program, input)
    executeProgram(state)
  }

fun getMaxOutputFromPrograms(states: List<ProgramState>): Int =
  states
    .filterMap { it.output }
    .max() ?: 0

fun main(): Unit = unsafe {
  runBlocking {
    IO.fx {
      val program = !effect { readProblemInput() }

      val generatedInputs = generateInputSignals()
      val programsOutputs = !effect { executeProgramsForInputs(generatedInputs, program) }
      val reduceOutputs = getMaxOutputFromPrograms(programsOutputs)

      !effect { println(reduceOutputs) }

      Unit
    }
  }
}