package com.example.androidtest

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.example.androidtest.FormState.*
import com.example.androidtest.FormEvent.*
import com.example.androidtest.SectionState.Complete
import com.example.androidtest.core.FsmViewModel
import com.example.androidtest.core.fsm
import com.example.androidtest.core.fsmState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch

enum class FormState {
    ReadyToSubmit,
    Submitting,
    NotReady,
}

enum class FormEvent {
    AllSectionsCompleted,
    HasUncompletedSections,
    SubmitInitiated,
    Submitted
}

val formFSM = fsm<FormState, FormEvent>{
    from(NotReady) {
        AllSectionsCompleted goesTo ReadyToSubmit
    }

    from(ReadyToSubmit) {
        SubmitInitiated goesTo Submitting
        HasUncompletedSections goesTo NotReady
    }

    from(Submitting) {
        Submitted goesTo NotReady
    }
}

@Immutable
data class FormViewModelState (
    val signature: String = "",
    val state: FormState = NotReady,
)

fun FormViewModelState.withState(newState: FormState) = this.copy(state = newState)

@OptIn(ExperimentalCoroutinesApi::class)
class FormViewModel(
    initialState: FormViewModelState = FormViewModelState(),
    val userInfoViewModel: SectionViewModel = SectionViewModel(validInputPattern = "^[a-zA-Z]+$"),
    val paymentDetailsViewModel: SectionViewModel = SectionViewModel(validInputPattern = "^[0-9]+(?:\\d{16})$"),
)
: FsmViewModel<FormState, FormEvent, FormViewModelState>(
    fsm = formFSM,
    initialState = initialState,
    state = fsmState(FormViewModelState::state, FormViewModelState::withState),
) {
    private val sectionViewModels = MutableStateFlow(listOf(userInfoViewModel, paymentDetailsViewModel))

    init {
        viewModelScope.launch {
            sectionViewModels.flatMapLatest { sectionViewModelsList ->
                combine(sectionViewModelsList.map { it.stateFlow }) { sectionStates ->
                    if (sectionStates.all { it.state == Complete })
                        AllSectionsCompleted else HasUncompletedSections
                }
            }.collect { processEvent(it) }
        }
    }

    override fun onChangeState(from: FormState, to: FormState, cause: FormEvent) {
        // when from is Submitting and to is NotReady,
        // it means the form has been submitted
        // and we should reset the form (state of all sections)
        if (from == Submitting && to == NotReady) {
            sectionViewModels.value.forEach { it.reset() }
        }
    }

    fun handleSubmit() {
        processEvent(SubmitInitiated)

        viewModelScope.launch {
            delay(2000)
            processEvent(Submitted)
        }
    }
}

@Composable
fun FormScreen() {
    val formViewModel by remember { mutableStateOf(FormViewModel()) }

    FormComponent(formViewModel)
}

@Composable
fun FormComponent(formViewModel: FormViewModel) {
    val userInfoState by formViewModel.userInfoViewModel.stateFlow.collectAsStateWithLifecycle()
    val paymentDetailsState by formViewModel.paymentDetailsViewModel.stateFlow.collectAsStateWithLifecycle()
    val formState by formViewModel.stateFlow.collectAsStateWithLifecycle()

    Column {
        Box {
            Column {
                UserInfoSection(userInfoState, formViewModel.userInfoViewModel)
                PaymentDetailsSection(paymentDetailsState, formViewModel.paymentDetailsViewModel)
            }

            // overlay that captures all interactions when form is submitting
            SubmittingOverlay(formState)
        }

        SubmitButton(formState, formViewModel)
    }
}

@Composable
fun BoxScope.SubmittingOverlay(s: FormViewModelState) {
    if (s.state == Submitting) Box(
        modifier = Modifier
            .matchParentSize()
            .background(Color.LightGray.copy(alpha = 0.5f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {},
        contentAlignment = Alignment.Center
    ) { CircularProgressIndicator() }
}

@Composable
fun SubmitButton(s: FormViewModelState, vm: FormViewModel) {
    Button(
        onClick = vm::handleSubmit,
        enabled = s.state == ReadyToSubmit
    ) {
        Text("Submit Form")
    }
}

@Composable
@Preview
fun ReadyToSubmitFormPreview() {
    FormComponent(FormViewModel(FormViewModelState(state = ReadyToSubmit)))
}

@Composable
@Preview
fun NotReadyFormPreview() {
    FormComponent(FormViewModel(FormViewModelState(state = NotReady)))
}
