package com.example.androidtest.core

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.reflect.KProperty1

@Immutable
data class FsmMode<S : Enum<S>, State>(
    val getValue: (State) -> S,
    val setValue: (State, S) -> State
)

fun <S : Enum<S>, State> fsmMode(
    getValue: KProperty1<State, S>,
    setValue: State.(S) -> State
): FsmMode<S, State> = FsmMode(
    getValue = { state -> getValue.get(state) },
    setValue = { state, mode -> state.setValue(mode) }
)

abstract class FsmViewModel<S : Enum<S>, E : Enum<E>, State>(
    initialState: State,
    private val fsm: FSM<S, E>,
    private val mode: FsmMode<S, State>
) : ViewModel() {
    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<State> = _state.asStateFlow()

    private fun getMode(state: State): S = this.mode.getValue(state)
    private fun setMode(state: State, mode: S): State = this.mode.setValue(state, mode)

    protected fun setState(update: State.() -> State) {
        _state.value = _state.value.update()
    }

    protected fun processEvent(event: E) {
        val currentMode = getMode(_state.value)

        val nextMode = fsm.nextState(currentMode, event)
        if (nextMode == currentMode)
            return

        setState { setMode(this, nextMode) }
        onModeChange(currentMode, nextMode, event)
    }

    protected open fun onModeChange(from: S, to: S, cause: E) = Unit
}