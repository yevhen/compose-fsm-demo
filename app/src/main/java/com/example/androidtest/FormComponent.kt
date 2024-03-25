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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.androidtest.FormState.*
import com.example.androidtest.SectionMode.Complete

enum class FormState {
    ReadyToSubmit,
    Submitting,
    NotReady
}

fun formNextState(sectionStates: List<SectionState>): FormState {
    val isReadyToSubmit = sectionStates.all { it.mode == Complete }
    return if (isReadyToSubmit) ReadyToSubmit else NotReady
}

@Composable
fun FormScreen() {
    val userInfoViewModel by remember { mutableStateOf(SectionViewModel(SectionState(mode = Complete))) }
    val paymentDetailsViewModel by remember { mutableStateOf(SectionViewModel(SectionState(mode = Complete))) }

    FormComponent(userInfoViewModel, paymentDetailsViewModel)
}

@Composable
fun FormComponent(userInfoViewModel: SectionViewModel, paymentDetailsViewModel: SectionViewModel) {
    val userInfoState by userInfoViewModel.state.collectAsStateWithLifecycle()
    val paymentDetailsState by paymentDetailsViewModel.state.collectAsStateWithLifecycle()

    val formState by remember {
        derivedStateOf {
            formNextState(listOf(userInfoState, paymentDetailsState))
        }
    }

    Column {
        Box {
            Column {
                UserInfoSection(userInfoState, userInfoViewModel)
                PaymentDetailsSection(paymentDetailsState, paymentDetailsViewModel)
            }

            // overlay that captures all interactions when form is submitting
            if (formState == Submitting) Box(
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

        SubmitButton(formState)
    }
}

@Composable
fun SubmitButton(formState: FormState) {
    Button(
        onClick = { /* Submit form logic */ },
        enabled = formState == ReadyToSubmit
    ) {
        Text("Submit Form")
    }
}

@Composable
@Preview
fun ReadyToSubmitFormPreview() {
    FormComponent(
        SectionViewModel(SectionState(mode = Complete)),
        SectionViewModel(SectionState(mode = Complete))
    )
}

@Composable
@Preview
fun NotReadyFormPreview() {
    FormComponent(
        SectionViewModel(SectionState()),
        SectionViewModel(SectionState())
    )
}