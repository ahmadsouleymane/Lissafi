package com.lissafi.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lissafi.app.ui.theme.Green800
import com.lissafi.app.ui.theme.LissafiCream
import com.lissafi.app.ui.theme.LissafiGreen
import com.lissafi.app.ui.theme.LissafiGreenDark
import com.lissafi.app.ui.theme.LissafiOrange
import com.lissafi.app.ui.theme.LissafiWhite
import com.lissafi.app.ui.theme.Neutral200
import com.lissafi.app.ui.theme.Neutral400
import com.lissafi.app.ui.theme.Neutral500
import com.lissafi.app.ui.theme.White
import com.lissafi.app.ui.viewmodel.AuthMode
import com.lissafi.app.ui.viewmodel.AuthState
import com.lissafi.app.ui.viewmodel.AuthViewModel

@Composable
fun AuthScreen(viewModel: AuthViewModel) {
    val state by viewModel.state.collectAsState()
    var passwordVisible by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LissafiCream)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── BRANDING ──
            Spacer(Modifier.height(24.dp))
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(CircleShape)
                    .background(LissafiGreen.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Store,
                    contentDescription = null,
                    tint = LissafiGreen,
                    modifier = Modifier.size(44.dp)
                )
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = "LISSAFI",
                color = Green800,
                fontSize = 30.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 4.sp
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Ta caisse, ton commerce — simples comme un SMS.",
                color = Neutral500,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )

            Spacer(Modifier.height(28.dp))

            // ── CARTE FORMULAIRE ──
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    // Mode selector
                    AuthModeTabs(
                        mode = state.mode,
                        onSignIn = { viewModel.setMode(AuthMode.SIGN_IN) },
                        onSignUp = { viewModel.setMode(AuthMode.SIGN_UP) }
                    )

                    Spacer(Modifier.height(18.dp))

                    when (state.mode) {
                        AuthMode.SIGN_IN -> SignInForm(
                            state = state,
                            passwordVisible = passwordVisible,
                            onTogglePassword = { passwordVisible = !passwordVisible },
                            onEmailChange = { viewModel.setEmail(it) },
                            onPasswordChange = { viewModel.setPassword(it) },
                            onSubmit = { viewModel.signIn() },
                            onSwitchToReset = { viewModel.setMode(AuthMode.RESET_PASSWORD) }
                        )
                        AuthMode.SIGN_UP -> SignUpForm(
                            state = state,
                            passwordVisible = passwordVisible,
                            onTogglePassword = { passwordVisible = !passwordVisible },
                            onEmailChange = { viewModel.setEmail(it) },
                            onPasswordChange = { viewModel.setPassword(it) },
                            onConfirmPasswordChange = { viewModel.setConfirmPassword(it) },
                            onShopNameChange = { viewModel.setShopName(it) },
                            onSubmit = { viewModel.signUp() }
                        )
                        AuthMode.RESET_PASSWORD -> ResetPasswordForm(
                            state = state,
                            onEmailChange = { viewModel.setEmail(it) },
                            onSubmit = { viewModel.resetPassword() },
                            onSwitchToSignIn = { viewModel.setMode(AuthMode.SIGN_IN) }
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── PIED DE PAGE ──
            Text(
                text = "Vos données restent sur votre téléphone.",
                color = Neutral400,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

/** Onglets Connexion / Inscription — faciles à voir pour tout le monde. */
@Composable
private fun AuthModeTabs(
    mode: AuthMode,
    onSignIn: () -> Unit,
    onSignUp: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(LissafiCream)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        AuthTab(
            label = "Connexion",
            selected = mode == AuthMode.SIGN_IN,
            onClick = onSignIn,
            modifier = Modifier.weight(1f)
        )
        AuthTab(
            label = "Nouveau compte",
            selected = mode == AuthMode.SIGN_UP,
            onClick = onSignUp,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun AuthTab(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(11.dp))
            .background(if (selected) LissafiGreen else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (selected) White else Neutral500,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            fontSize = 14.sp
        )
    }
}

/** Bandeau message d'erreur ou de succès. */
@Composable
private fun AuthMessage(message: String, isError: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isError) Color(0xFFFEE2E2) else LissafiGreen.copy(alpha = 0.1f)
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (isError) Icons.Filled.Error else Icons.Filled.CheckCircle,
            contentDescription = null,
            tint = if (isError) Color(0xFFDC2626) else LissafiGreen,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = message,
            color = if (isError) Color(0xFFDC2626) else LissafiGreen,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            lineHeight = 17.sp
        )
    }
}

@Composable
private fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: (@Composable () -> Unit)? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = {
            Text(
                text = placeholder,
                color = Neutral400,
                fontSize = 15.sp
            )
        },
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = LissafiGreen,
                modifier = Modifier.size(20.dp)
            )
        },
        trailingIcon = trailingIcon,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
        visualTransformation = visualTransformation,
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = LissafiGreen,
            unfocusedBorderColor = Neutral200,
            focusedContainerColor = White,
            unfocusedContainerColor = White,
            cursorColor = LissafiGreen
        ),
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
private fun AuthSubmitButton(
    text: String,
    onClick: () -> Unit,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        enabled = !isLoading,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = LissafiGreen,
            contentColor = White,
            disabledContainerColor = Neutral200,
            disabledContentColor = Neutral500
        )
    ) {
        if (isLoading) {
            CircularProgressIndicator(color = White, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
        } else {
            Text(
                text = text,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                letterSpacing = 0.5.sp
            )
        }
    }
}

@Composable
private fun SignInForm(
    state: AuthState,
    passwordVisible: Boolean,
    onTogglePassword: () -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onSwitchToReset: () -> Unit
) {
    Column {
        AuthTextField(
            value = state.email,
            onValueChange = onEmailChange,
            placeholder = "Ton adresse email",
            icon = Icons.Filled.Email,
            keyboardType = KeyboardType.Email
        )
        Spacer(Modifier.height(12.dp))
        AuthTextField(
            value = state.password,
            onValueChange = onPasswordChange,
            placeholder = "Ton mot de passe",
            icon = Icons.Filled.Lock,
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = onTogglePassword) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = null,
                        tint = Neutral500,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        )

        if (state.message != null) {
            Spacer(Modifier.height(12.dp))
            AuthMessage(state.message, state.isError)
        }

        Spacer(Modifier.height(16.dp))
        AuthSubmitButton(
            text = "SE CONNECTER",
            onClick = onSubmit,
            isLoading = state.isLoading
        )

        Spacer(Modifier.height(4.dp))
        TextButton(
            onClick = onSwitchToReset,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Mot de passe oublié ?",
                color = LissafiOrange,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun SignUpForm(
    state: AuthState,
    passwordVisible: Boolean,
    onTogglePassword: () -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onShopNameChange: (String) -> Unit,
    onSubmit: () -> Unit
) {
    Column {
        // Bénéfices — aide à comprendre pourquoi s'inscrire
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(LissafiGreen.copy(alpha = 0.07f))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = LissafiGreen,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = "Suis ta caisse partout : tes ventes, tes clients et tes dettes restent en sécurité.",
                color = LissafiGreenDark,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(Modifier.height(14.dp))
        AuthTextField(
            value = state.shopName,
            onValueChange = onShopNameChange,
            placeholder = "Nom de ta boutique (optionnel)",
            icon = Icons.Filled.Store
        )
        Spacer(Modifier.height(12.dp))
        AuthTextField(
            value = state.email,
            onValueChange = onEmailChange,
            placeholder = "Ton adresse email",
            icon = Icons.Filled.Email,
            keyboardType = KeyboardType.Email
        )
        Spacer(Modifier.height(12.dp))
        AuthTextField(
            value = state.password,
            onValueChange = onPasswordChange,
            placeholder = "Mot de passe (6 caractères ou plus)",
            icon = Icons.Filled.Lock,
            keyboardType = KeyboardType.Password,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = onTogglePassword) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = null,
                        tint = Neutral500,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        )
        Spacer(Modifier.height(12.dp))
        AuthTextField(
            value = state.confirmPassword,
            onValueChange = onConfirmPasswordChange,
            placeholder = "Confirmer le mot de passe",
            icon = Icons.Filled.Lock,
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation()
        )

        if (state.message != null) {
            Spacer(Modifier.height(12.dp))
            AuthMessage(state.message, state.isError)
        }

        Spacer(Modifier.height(16.dp))
        AuthSubmitButton(
            text = "CRÉER MON COMPTE",
            onClick = onSubmit,
            isLoading = state.isLoading
        )
    }
}

@Composable
private fun ResetPasswordForm(
    state: AuthState,
    onEmailChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onSwitchToSignIn: () -> Unit
) {
    Column {
        Text(
            text = "Réinitialiser le mot de passe",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = Green800
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Entre ton email : on t'enverra un lien pour créer un nouveau mot de passe.",
            color = Neutral500,
            fontSize = 13.sp,
            lineHeight = 18.sp
        )

        Spacer(Modifier.height(16.dp))
        AuthTextField(
            value = state.email,
            onValueChange = onEmailChange,
            placeholder = "Ton adresse email",
            icon = Icons.Filled.Email,
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Done
        )

        if (state.message != null) {
            Spacer(Modifier.height(12.dp))
            AuthMessage(state.message, state.isError)
        }

        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onSubmit,
            enabled = !state.isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = LissafiOrange,
                contentColor = White,
                disabledContainerColor = Neutral200,
                disabledContentColor = Neutral500
            )
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(color = White, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
            } else {
                Text(
                    text = "ENVOYER LE LIEN",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    letterSpacing = 0.5.sp
                )
            }
        }

        Spacer(Modifier.height(4.dp))
        TextButton(
            onClick = onSwitchToSignIn,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "← Retour à la connexion",
                color = LissafiGreen,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
