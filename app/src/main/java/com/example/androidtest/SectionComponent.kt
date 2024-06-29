package com.example.androidtest

import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.example.androidtest.SectionEvent.*
import com.example.androidtest.SectionState.*
import com.example.androidtest.core.FsmViewModel
import com.example.androidtest.core.fsm
import com.example.androidtest.core.fsmState

enum class SectionState {
    Incomplete,
    Complete,
    Valid,
    Invalid
}

enum class SectionEvent {
    NoInput,
    ValidInput,
    InvalidInput,
    ConsentGiven,
    NoConsent
}

val sectionFSM = fsm<SectionState, SectionEvent>{
    from(Incomplete) {
        ValidInput goesTo Valid
        InvalidInput goesTo Invalid
    }

    from(Complete) {
        NoConsent goesTo Valid
        InvalidInput goesTo Invalid
        NoInput goesTo Incomplete
    }

    from(Valid) {
        InvalidInput goesTo Invalid
        NoInput goesTo Incomplete
        ConsentGiven goesTo Complete
    }

    from(Invalid) {
        ValidInput goesTo Valid
        NoInput goesTo Incomplete
    }
}

@Immutable
data class SectionViewModelState (
    val text: String = "",
    val consent: Boolean = false,
    val state: SectionState = Incomplete,
)

fun SectionViewModelState.withState(newState: SectionState): SectionViewModelState = this.copy(state = newState)

class SectionViewModel(
    initialState: SectionViewModelState = SectionViewModelState(),
    val validInputPattern: String = "^[a-zA-Z]+$"
) : FsmViewModel<SectionState, SectionEvent, SectionViewModelState>(
    fsm = sectionFSM,
    initialState = initialState,
    state = fsmState(SectionViewModelState::state, SectionViewModelState::withState),
) {
    override fun onChangeState(from: SectionState, to: SectionState, cause: SectionEvent) {
        // interdependencies between state variables could be handled here
        // either by reacting to respective state transition events or to final state
        if (to == Invalid || to == Incomplete)
            setState { copy(consent = false) }
    }

    fun handleTextChange(text: String) {
        setState { copy(text = text) }
        processEvent(
            when {
                text.isEmpty() -> NoInput
                text.matches(Regex(validInputPattern)) -> ValidInput
                else -> InvalidInput
            }
        )
    }

    fun handleConsentChange(checked: Boolean) {
        setState { copy(consent = checked) }
        processEvent(if (checked) ConsentGiven else NoConsent)
    }

    fun reset() {
        setState { copy(text = "", consent = false, state = Incomplete) }
    }
}

@Composable
fun UserInfoSection(s: SectionViewModelState, vm: SectionViewModel) {
    Text("User Info Section")

    TextField(
        value = s.text,
        onValueChange = vm::handleTextChange,
        label = { Text("User Info") }
    )

    when (s.state) {
        Invalid -> Text(text = "Invalid input. Should match [a-z][A-Z]", color = Color.Red)
        Incomplete -> Text(text = "Please, provide personal info", color = Color.Gray)
        Valid -> Text(text = "Please, give consent", color = Color.Blue)
        else -> {}
    }

    Checkbox(
        checked = s.consent,
        enabled = s.state == Complete || s.state == Valid,
        onCheckedChange = vm::handleConsentChange
    )
}

@Composable
fun PaymentDetailsSection(s: SectionViewModelState, vm: SectionViewModel) {
    Text("Payment Details Section")

    TextField(
        value = s.text,
        onValueChange = vm::handleTextChange,
        label = { Text("Card Details") }
    )

    when (s.state) {
        Invalid -> Text(text = "Invalid input. Should have 16 digits", color = Color.Red)
        Incomplete -> Text(text = "Please, provide card details", color = Color.Gray)
        Valid -> Text(text = "Please, give consent", color = Color.Blue)
        else -> {}
    }

    Checkbox(
        checked = s.consent,
        enabled = s.state == Complete || s.state == Valid,
        onCheckedChange = vm::handleConsentChange
    )
}

@Composable
@Preview
fun IncompleteUserInfoSectionPreview() =
    CreateUserInfoSection(SectionViewModelState(state = Incomplete))

@Composable
@Preview
fun ValidUserInfoSectionPreview() =
    CreateUserInfoSection(SectionViewModelState(text = "zzzz", state = Valid))

@Composable
@Preview
fun InvalidUserInfoSectionPreview() =
    CreateUserInfoSection(SectionViewModelState(text = "zzzz11233", state = Invalid))

@Composable
@Preview
fun CompleteUserInfoSectionPreview() =
    CreateUserInfoSection(SectionViewModelState(text = "zzzz", consent = true, state = Complete))

@Composable
private fun CreateUserInfoSection(state: SectionViewModelState) =
    UserInfoSection(state, SectionViewModel())
