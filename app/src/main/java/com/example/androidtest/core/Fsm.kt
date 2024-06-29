package com.example.androidtest.core

class FSM<S : Enum<S>, E : Enum<E>> {
    private val transitions = mutableMapOf<S, MutableMap<E, S>>()

    inner class StateTransitionBuilder(private val fromState: S) {
        infix fun E.goesTo(toState: S) {
            transitions.getOrPut(fromState) { mutableMapOf() }[this] = toState
        }
    }

    fun from(state: S, block: StateTransitionBuilder.() -> Unit) {
        StateTransitionBuilder(state).apply(block)
    }

    fun nextState(current: S, event: E): S? = transitions[current]?.get(event)
}

fun <S : Enum<S>, E : Enum<E>> fsm(build: FSM<S, E>.() -> Unit): FSM<S, E> = FSM<S, E>().apply(build)
