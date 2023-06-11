package com.ramcosta.composedestinations.annotation

import com.ramcosta.composedestinations.animations.defaults.NavHostAnimatedDestinationStyle
import com.ramcosta.composedestinations.animations.defaults.NoTransitions
import kotlin.reflect.KClass

/**
 * Like [NavGraph] but denotes a top level nav graph, i.e one that is not nested in any other
 * nav graph (aka it doesn't have a parent).
 * These are used to pass to [com.ramcosta.composedestinations.DestinationsNavHost] call.
 *
 * [RootNavGraph] is one such graph that can be used out of the box.
 */
@Target(AnnotationTarget.ANNOTATION_CLASS)
annotation class NavHostGraph(
    val defaultTransitions: KClass<out NavHostAnimatedDestinationStyle>,
    val route: String = NavGraph.ANNOTATION_NAME,
    val default: Boolean = false
)

/**
 * Navigation graph annotation that will, by default, correspond to all Destinations that
 * don't specify a navigation graph.
 * If you're using it (i.e, you're not defining your own "NavGraph" annotation with `default = true`),
 * then you must annotate the start destination (or nav graph) with `@RootNavGraph(start = true)`.
 */
@NavHostGraph(
    defaultTransitions = NoTransitions::class,
    default = true
)
annotation class RootNavGraph(
    val start: Boolean = false
)