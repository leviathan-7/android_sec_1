/*
Copyright 2022 Google LLC

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */

package com.example.makeitso.screens.settings

import android.content.Context
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.GetPasswordOption
import androidx.credentials.GetPublicKeyCredentialOption
import androidx.credentials.PasswordCredential
import androidx.credentials.PublicKeyCredential
import androidx.credentials.exceptions.GetCredentialException
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.makeitso.R.drawable as AppIcon
import com.example.makeitso.R.string as AppText
import com.example.makeitso.common.composable.*
import com.example.makeitso.common.ext.card
import com.example.makeitso.common.ext.spacer
import com.example.makeitso.theme.MakeItSoTheme
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest
import java.util.UUID

@ExperimentalMaterialApi
@Composable
fun SettingsScreen(
  restartApp: (String) -> Unit,
  openScreen: (String) -> Unit,
  viewModel: SettingsViewModel = hiltViewModel()
) {
  val uiState by viewModel.uiState.collectAsState(initial = SettingsUiState(false))

  SettingsScreenContent(
    uiState = uiState,
    onLoginClick = { viewModel.onLoginClick(openScreen) },
    onSignUpClick = { viewModel.onSignUpClick(openScreen) },
    onSignOutClick = { viewModel.onSignOutClick(restartApp) },
    onDeleteMyAccountClick = { viewModel.onDeleteMyAccountClick(restartApp) }
  )
}

@ExperimentalMaterialApi
@Composable
fun SettingsScreenContent(
  modifier: Modifier = Modifier,
  uiState: SettingsUiState,
  onLoginClick: () -> Unit,
  onSignUpClick: () -> Unit,
  onSignOutClick: () -> Unit,
  onDeleteMyAccountClick: () -> Unit
) {
  val coroutineScope = rememberCoroutineScope()
  val currentContext = LocalContext.current

  //val getCredentialRequest = configureGetCredentialRequest(currentContext)

  Column(
    modifier = modifier
      .fillMaxWidth()
      .fillMaxHeight()
      .verticalScroll(rememberScrollState()),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    BasicToolbar(AppText.settings)

    Spacer(modifier = Modifier.spacer())

    if (uiState.isAnonymousAccount) {
      RegularCardEditor(AppText.sign_in, AppIcon.ic_sign_in, "", Modifier.card()) {
        onLoginClick()
      }

      RegularCardEditor(AppText.create_account, AppIcon.ic_create_account, "", Modifier.card()) {
        onSignUpClick()
      }

      Button(onClick = {
        Log.e("--0","--0")

        coroutineScope.launch {
          try {
            Log.e("--1","--1")

            val result = CredentialManager.create(currentContext).getCredential(
              request = configureGetCredentialRequest(currentContext),
              context = currentContext,
            )
            Log.e("--2","--2")

            handleSignIn(result)
            Log.e("--3","--3")

          } catch (e: GetCredentialException) {
            Log.e("TAG1", e.message ?: "Unknown")
          }
        }
      }) {
        Text(text = "sign in by google")
      }

    } else {
      SignOutCard { onSignOutClick() }
      DeleteMyAccountCard { onDeleteMyAccountClick() }
      Inf()
    }
  }
}


private fun configureGetCredentialRequest(currentContext : Context): GetCredentialRequest {
  Log.e("--1.1","--1.1")
  val getPublicKeyCredentialOption =
    GetPublicKeyCredentialOption(fetchAuthJsonFromServer(currentContext), null)
  val getPasswordOption = GetPasswordOption()
  val md = MessageDigest.getInstance("SHA-1")
  val ranNonce: String = UUID.randomUUID().toString()
  val bytes: ByteArray = ranNonce.toByteArray()
  val digest: ByteArray = md.digest(bytes)
  val hashedNonce: String = digest.fold("") { str, it -> str + "%02x".format(it) }
  Log.e("--1.2","--1.3")
  val googleIdOption: GetSignInWithGoogleOption = GetSignInWithGoogleOption
    .Builder("218968592264-iispqsabu01eb5v5tjp5ht7ot8bm3rlv.apps.googleusercontent.com")
    .setNonce(hashedNonce)
    .build()
  Log.e("--1.4","--1.4")
  val getCredentialRequest = GetCredentialRequest(
    listOf(
      //getPublicKeyCredentialOption,
      //getPasswordOption
      googleIdOption
    )
  )
  Log.e("--1.5","--1.5")
  return getCredentialRequest
}

fun fetchAuthJsonFromServer(currentContext : Context): String {
  return currentContext.readFromAsset("AuthFromServer")
}
fun Context.readFromAsset(fileName: String): String {
  var data = ""
  this.assets.open(fileName).bufferedReader().use {
    data = it.readText()
  }
  return data
}
fun handleSignIn(result: GetCredentialResponse) {
  Log.e("--2.0","--2.0")
  // Handle the successfully returned credential.
  val credential = result.credential
  Log.e("--2.1","--2.1")
  when (credential) {
    // GoogleIdToken credential
    is CustomCredential -> {
      Log.e("--2.2","--2.2")

      if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
        try {
          // Use googleIdTokenCredential and extract the ID to validate and
          // authenticate on your server.
          val googleIdTokenCredential = GoogleIdTokenCredential
            .createFrom(credential.data)

          val authCredential =
            GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
          val firebaseAuth = FirebaseAuth.getInstance()
          val authResult = firebaseAuth.signInWithCredential(authCredential)
          //trySend(Result.success(authResult))

          // You can use the members of googleIdTokenCredential directly for UX
          // purposes, but don't use them to store or control access to user
          // data. For that you first need to validate the token:
          // pass googleIdTokenCredential.getIdToken() to the backend server.

          //GoogleIdTokenVerifier verifier = ... // see validation instructions
          //GoogleIdToken idToken = verifier.verify(idTokenString);

          // To get a stable account identifier (e.g. for storing user data),
          // use the subject ID:
          val idToken = googleIdTokenCredential.idToken
          Log.e("--2.3","--2.3")


        } catch (e: GoogleIdTokenParsingException) {
          Log.e("TAG2", "Received an invalid google id token response", e)
        }
      } else {
        // Catch any unrecognized custom credential type here.
        Log.e("TAG3", "Unexpected type of credential")
      }
    }

    else -> {
      // Catch any unrecognized credential type here.
      Log.e("TAG4", "Unexpected type of credential")
    }
  }
}

@ExperimentalMaterialApi
@Composable
private fun Inf() {
  
}

@ExperimentalMaterialApi
@Composable
private fun SignOutCard(signOut: () -> Unit) {
  var showWarningDialog by remember { mutableStateOf(false) }

  RegularCardEditor(AppText.sign_out, AppIcon.ic_exit, "", Modifier.card()) {
    showWarningDialog = true
  }

  if (showWarningDialog) {
    AlertDialog(
      title = { Text(stringResource(AppText.sign_out_title)) },
      text = { Text(stringResource(AppText.sign_out_description)) },
      dismissButton = { DialogCancelButton(AppText.cancel) { showWarningDialog = false } },
      confirmButton = {
        DialogConfirmButton(AppText.sign_out) {
          signOut()
          showWarningDialog = false
        }
      },
      onDismissRequest = { showWarningDialog = false }
    )
  }
}

@ExperimentalMaterialApi
@Composable
private fun DeleteMyAccountCard(deleteMyAccount: () -> Unit) {
  var showWarningDialog by remember { mutableStateOf(false) }

  DangerousCardEditor(
    AppText.delete_my_account,
    AppIcon.ic_delete_my_account,
    "",
    Modifier.card()
  ) {
    showWarningDialog = true
  }

  if (showWarningDialog) {
    AlertDialog(
      title = { Text(stringResource(AppText.delete_account_title)) },
      text = { Text(stringResource(AppText.delete_account_description)) },
      dismissButton = { DialogCancelButton(AppText.cancel) { showWarningDialog = false } },
      confirmButton = {
        DialogConfirmButton(AppText.delete_my_account) {
          deleteMyAccount()
          showWarningDialog = false
        }
      },
      onDismissRequest = { showWarningDialog = false }
    )
  }
}

@Preview(showBackground = true)
@ExperimentalMaterialApi
@Composable
fun SettingsScreenPreview() {
  val uiState = SettingsUiState(isAnonymousAccount = false)

  MakeItSoTheme {
    SettingsScreenContent(
      uiState = uiState,
      onLoginClick = { },
      onSignUpClick = { },
      onSignOutClick = { },
      onDeleteMyAccountClick = { }
    )
  }
}
