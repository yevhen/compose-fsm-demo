package com.example.androidtest

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
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
import com.example.androidtest.FormMode.*
import com.example.androidtest.FormEvent.*
import com.example.androidtest.SectionMode.Complete
import com.example.androidtest.core.FsmViewModel
import com.example.androidtest.core.fsm
import com.example.androidtest.core.fsmMode
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch

enum class FormMode {
    ReadyToSubmit,
    Submitting,
    NotReady,
}

enum class FormEvent {
    AllSectionsCompleted,
    HasUncompletedSections,
    SubmitInitiated,
}

val formFSM = fsm<FormMode, FormEvent>{
    from(NotReady) {
        AllSectionsCompleted goesTo ReadyToSubmit
    }

    from(ReadyToSubmit) {
        SubmitInitiated goesTo Submitting
        HasUncompletedSections goesTo NotReady
    }
}

@Immutable
data class FormState (
    val signature: String = "",
    val mode: FormMode = NotReady,
)

fun FormState.withMode(newMode: FormMode) = this.copy(mode = newMode)

@OptIn(ExperimentalCoroutinesApi::class)
class FormViewModel(
    initialState: FormState = FormState(),
    val userInfoViewModel: SectionViewModel = SectionViewModel(),
    val paymentDetailsViewModel: SectionViewModel = SectionViewModel(),
)
: FsmViewModel<FormMode, FormEvent, FormState>(
    fsm = formFSM,
    initialState = initialState,
    mode = fsmMode(FormState::mode, FormState::withMode),
) {
    private val sectionViewModels = MutableStateFlow(listOf(userInfoViewModel, paymentDetailsViewModel))

    init {
        viewModelScope.launch {
            sectionViewModels.flatMapLatest { sectionViewModelsList ->
                combine(sectionViewModelsList.map { it.state }) { sectionStates ->
                    if (sectionStates.all { it.mode == Complete })
                        AllSectionsCompleted else HasUncompletedSections
                }
            }.collect { processEvent(it) }
        }
    }

    fun handleSubmit() {
        processEvent(SubmitInitiated)
    }
}

@Composable
fun FormScreen() {
    val viewModel by remember { mutableStateOf(FormViewModel()) }
    val state by viewModel.state.collectAsStateWithLifecycle()

    FormComponent(state, viewModel)
}

@Composable
fun FormComponent(state: FormState, viewModel: FormViewModel) {
    val userInfoState by viewModel.userInfoViewModel.state.collectAsStateWithLifecycle()
    val paymentDetailsState by viewModel.paymentDetailsViewModel.state.collectAsStateWithLifecycle()

    Column {
        Box {
            Column {
                UserInfoSection(userInfoState, viewModel.userInfoViewModel)
                PaymentDetailsSection(paymentDetailsState, viewModel.paymentDetailsViewModel)
            }

            // overlay that captures all interactions when form is submitting
            if (state.mode == Submitting) Box(
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

        SubmitButton(state, viewModel)
    }
}

@Composable
fun SubmitButton(state: FormState, viewModel: FormViewModel) {
    Button(
        onClick = { viewModel.handleSubmit() },
        enabled = state.mode == ReadyToSubmit
    ) {
        Text("Submit Form")
    }
}

@Composable
@Preview
fun ReadyToSubmitFormPreview() {
    FormComponent(FormState(mode = ReadyToSubmit), FormViewModel())
}

@Composable
@Preview
fun NotReadyFormPreview() {
    FormComponent(FormState(mode = NotReady), FormViewModel())
}
