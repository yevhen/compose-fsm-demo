package com.example.androidtest

import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.example.androidtest.SectionEvent.ConsentGiven
import com.example.androidtest.SectionEvent.InvalidInput
import com.example.androidtest.SectionEvent.NoConsent
import com.example.androidtest.SectionEvent.NoInput
import com.example.androidtest.SectionEvent.ValidInput
import com.example.androidtest.SectionMode.Complete
import com.example.androidtest.SectionMode.Incomplete
import com.example.androidtest.SectionMode.Invalid
import com.example.androidtest.SectionMode.Valid
import com.example.androidtest.core.FsmViewModel
import com.example.androidtest.core.fsm
import com.example.androidtest.core.fsmMode

enum class SectionMode {
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

val sectionFSM = fsm<SectionMode, SectionEvent>{
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
data class SectionState (
    val text: String = "",
    val consent: Boolean = false,
    val mode: SectionMode = Incomplete,
)

fun SectionState.withMode(newMode: SectionMode): SectionState = this.copy(mode = newMode)

class SectionViewModel : FsmViewModel<SectionMode, SectionEvent, SectionState>(
    fsm = sectionFSM,
    initialState = SectionState(),
    mode = fsmMode(SectionState::mode, SectionState::withMode)
) {
    override fun onModeChange(from: SectionMode, to: SectionMode, cause: SectionEvent) {
        // interdependencies between state variables could be handled here
        // either by reacting to respective state transition events or to final state
        if (to == Invalid)
            setState { copy(consent = false) }
    }

    fun handleTextChange(text: String) {
        setState { copy(text = text) }
        processEvent(
            when {
                text.isEmpty() -> NoInput
                text.matches(Regex("^[a-zA-Z]+$")) -> ValidInput
                else -> InvalidInput
            }
        )
    }

    fun handleConsentChange(checked: Boolean) {
        setState { copy(consent = checked) }
        processEvent(if (checked) ConsentGiven else NoConsent)
    }
}

@Composable
fun UserInfoSection(state: SectionState, vm: SectionViewModel) {
    Text("User Info Section")

    TextField(
        value = state.text,
        onValueChange = vm::handleTextChange,
        label = { Text("User Info") }
    )

    when (state.mode) {
        Invalid -> Text(text = "Invalid input. Should match [a-z][A-Z]", color = Color.Red)
        Incomplete -> Text(text = "Please, provide personal info", color = Color.Gray)
        Valid -> Text(text = "Please, give consent", color = Color.Blue)
        else -> {}
    }

    Checkbox(
        checked = state.consent,
        enabled = state.mode == Complete || state.mode == Valid,
        onCheckedChange = vm::handleConsentChange
    )
}

@Composable
fun PaymentDetailsSection(state: SectionState, vm: SectionViewModel) {
    Text("Payment Details Section")

    TextField(
        value = state.text,
        onValueChange = vm::handleTextChange,
        label = { Text("Card Details") }
    )

    when (state.mode) {
        Invalid -> Text(text = "Invalid input. Should match [a-z][A-Z]", color = Color.Red)
        Incomplete -> Text(text = "Please, provide card details", color = Color.Gray)
        Valid -> Text(text = "Please, give consent", color = Color.Blue)
        else -> {}
    }

    Checkbox(
        checked = state.consent,
        enabled = state.mode == Complete || state.mode == Valid,
        onCheckedChange = vm::handleConsentChange
    )
}