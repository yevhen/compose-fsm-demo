package com.example.androidtest.core

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.reflect.KProperty1

@Immutable
data class FsmState<State : Enum<State>, ViewModelState>(
    val getValue: (ViewModelState) -> State,
    val setValue: (ViewModelState, State) -> ViewModelState
)

fun <State : Enum<State>, ViewModelState> fsmState(
    getValue: KProperty1<ViewModelState, State>,
    setValue: ViewModelState.(State) -> ViewModelState
): FsmState<State, ViewModelState> = FsmState(
    getValue = { state -> getValue.get(state) },
    setValue = { state, mode -> state.setValue(mode) }
)

abstract class FsmViewModel<State : Enum<State>, Event : Enum<Event>, ViewModelState>(
    initialState: ViewModelState,
    private val fsm: FSM<State, Event>,
    private val state: FsmState<State, ViewModelState>
) : ViewModel() {

    private val _stateFlow: MutableStateFlow<ViewModelState> = MutableStateFlow(initialState)
    val stateFlow: StateFlow<ViewModelState> = _stateFlow.asStateFlow()

    private fun getState(s: ViewModelState): State = this.state.getValue(s)
    private fun setState(s: ViewModelState, state: State): ViewModelState = this.state.setValue(s, state)

    protected fun setState(update: ViewModelState.() -> ViewModelState) {
        _stateFlow.value = _stateFlow.value.update()
    }

    protected fun processEvent(event: Event) {
        val currentState = getState(_stateFlow.value)
        val nextState = fsm.nextState(currentState, event) ?: return

        setState { setState(this, nextState) }
        onChangeState(currentState, nextState, event)
    }

    protected open fun onChangeState(from: State, to: State, cause: Event) = Unit
}
