package com.cotel.adventofcode.day6

import arrow.core.Tuple2
import arrow.core.toT
import arrow.fx.IO
import arrow.fx.extensions.fx
import arrow.fx.extensions.io.unsafeRun.runBlocking
import arrow.unsafe
import java.io.File

data class OrbitNode(val key: String, val childs: List<OrbitNode>)

fun treeLength(root: OrbitNode): Int {
    fun step(root: OrbitNode, isFirstLevel: Boolean = true): Int =
        if (isFirstLevel) root.childs.fold(0) { acc, node -> acc + step(node, false) }
        else 1 + root.childs.fold(0) { acc, node -> acc + step(node, false) }

    return step(root)
}

fun directAndIndirectOrbitsLength(fromRoot: OrbitNode): Int =
    treeLength(fromRoot) + fromRoot.childs
        .sumBy { node -> directAndIndirectOrbitsLength(node) }

fun parseBodyDependency(str: String): Tuple2<String, String> =
    str.split(")").let { halves ->
        halves.first() toT halves.last()
    }

fun findFirstNode(bodiesDependencies: List<String>): OrbitNode {
    val allNodes = bodiesDependencies
        .map { parseBodyDependency(it).a }

    val nodesByAppearances = allNodes
        .groupBy { it }
        .mapValues { (_, v) -> v.size }

    return OrbitNode(nodesByAppearances
        .filterValues { it == 1 }
        .keys
        .first(), emptyList())
}

fun buildOrbits(initialTree: OrbitNode, bodiesDependencies: List<String>): OrbitNode {
    val parsedDependencies = bodiesDependencies.map { parseBodyDependency(it) }
    val childs = parsedDependencies
        .filter { (parent, _) -> parent == initialTree.key }
        .map { (_, key) ->
            val root = OrbitNode(key, emptyList())
            buildOrbits(root, bodiesDependencies)
        }
    return initialTree.copy(childs = childs)
}

suspend fun readProblemTestInput(): List<String> =
    File(object {}::class.java.getResource("/day6/Day6TestInput.txt").file)
        .run { readLines() }

suspend fun readProblemInput(): List<String> =
    File(object {}::class.java.getResource("/day6/Day6Input.txt").file)
        .run { readLines() }

fun main() {
    unsafe {
        runBlocking {
            IO.fx {
                val dependencies = !effect { readProblemInput() }
                val initialTree = findFirstNode(dependencies)
                val orbits = buildOrbits(initialTree, dependencies)
                !effect { println(directAndIndirectOrbitsLength(orbits)) }
            }
        }
    }
}