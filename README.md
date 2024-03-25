This is the demo for using FSM-based control flow with Compose and Kotlin.

The demo app showcases a simple (but overly bureaucratic) form with two different sections to collect user information and card details.
Each section has an input for a user to enter required data and a checkbox to get the user's consent about data usage.

The rules are like this:
- if there is no input (an empty textbox), then suggest entering data
- if the data is invalid, then show an error message
- the consent checkbox may only be enabled if data is not empty and valid
- if the user has changed the data after the consent was given, then require consent again
- the form can be submitted only when all sections are complete
- the section is considered to be complete only when data is valid, and consent is given
