package com.lissafi.app.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lissafi.app.R
import com.lissafi.app.ui.components.LissafiCard
import com.lissafi.app.ui.components.LissafiIcons
import com.lissafi.app.ui.components.SegmentedControl
import com.lissafi.app.ui.theme.Background
import com.lissafi.app.ui.theme.Border
import com.lissafi.app.ui.theme.Error
import com.lissafi.app.ui.theme.OnPrimary
import com.lissafi.app.ui.theme.Primary
import com.lissafi.app.ui.theme.Secondary
import com.lissafi.app.ui.theme.Surface
import com.lissafi.app.ui.theme.TextSecondary
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
            .background(Background)
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
            // Logo Lissafi — symbole + nom + tagline
            Image(
                painter = painterResource(id = R.drawable.logo_auth),
                contentDescription = "Lissafi",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
            )

            Spacer(Modifier.height(28.dp))

            // ── CARTE FORMULAIRE ──
            LissafiCard(cornerRadius = 26) {
                Column(modifier = Modifier.padding(20.dp)) {
                    // Onglets Connexion / Nouveau compte
                    SegmentedControl(
                        options = listOf("Connexion", "Nouveau compte"),
                        selectedIndex = if (state.mode == AuthMode.SIGN_UP) 1 else 0,
                        onSelect = { index ->
                            viewModel.setMode(if (index == 0) AuthMode.SIGN_IN else AuthMode.SIGN_UP)
                        }
                    )

                    Spacer(Modifier.height(14.dp))

                    // Bouton démo — accès rapide au compte de démonstration
                    OutlinedButton(
                        onClick = { viewModel.demoLogin() },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Secondary),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Secondary)
                    ) {
                        Text(
                            text = "🎬 Tester avec la démo",
                            color = Secondary,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp
                        )
                    }

                    Spacer(Modifier.height(16.dp))

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
                text = "Tes données restent sur ton téléphone.",
                color = TextSecondary,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

/** Bandeau message d'erreur ou de succès. */
@Composable
private fun AuthMessage(message: String, isError: Boolean) {
    val background = if (isError) Error.copy(alpha = 0.08f) else Primary.copy(alpha = 0.1f)
    val contentColor = if (isError) Error else Primary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(background)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (isError) LissafiIcons.Erreur else LissafiIcons.Succes,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = message,
            color = contentColor,
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
    icon: ImageVector,
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
                color = TextSecondary,
                fontSize = 15.sp
            )
        },
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Primary,
                modifier = Modifier.size(20.dp)
            )
        },
        trailingIcon = trailingIcon,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
        visualTransformation = visualTransformation,
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Primary,
            unfocusedBorderColor = Border,
            focusedContainerColor = Surface,
            unfocusedContainerColor = Surface,
            cursorColor = Primary
        ),
        modifier = modifier.fillMaxWidth()
    )
}

/** Champ email avec validation en temps réel (indépendante du message serveur). */
@Composable
private fun AuthEmailField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    imeAction: ImeAction = ImeAction.Next
) {
    var emailError by remember { mutableStateOf<String?>(null) }
    Column(modifier = modifier.fillMaxWidth()) {
        AuthTextField(
            value = value,
            onValueChange = { input ->
                onValueChange(input)
                emailError = if (input.isNotBlank() && !isValidEmail(input)) {
                    "Adresse email invalide"
                } else {
                    null
                }
            },
            placeholder = "Ton adresse email",
            icon = LissafiIcons.Email,
            keyboardType = KeyboardType.Email,
            imeAction = imeAction
        )
        val error = emailError
        if (error != null) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = error,
                color = Error,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }
}

/** Email valide s'il contient « @ » et un point. */
private fun isValidEmail(email: String): Boolean = email.contains("@") && email.contains(".")

/** Bouton principal plein (style PrimaryActionButton) avec état de chargement. */
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
            .height(56.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Primary,
            contentColor = OnPrimary,
            disabledContainerColor = Color(0xFF000000).copy(alpha = 0.06f),
            disabledContentColor = Color(0xFF000000).copy(alpha = 0.30f)
        )
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                color = OnPrimary,
                modifier = Modifier.size(22.dp),
                strokeWidth = 2.dp
            )
        } else {
            Text(
                text = text,
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp
            )
        }
    }
}

/** Bouton secondaire (style SecondaryActionButton) avec état de chargement. */
@Composable
private fun AuthResetButton(
    text: String,
    onClick: () -> Unit,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        enabled = !isLoading,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.5.dp, Primary),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Primary)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                color = Primary,
                modifier = Modifier.size(22.dp),
                strokeWidth = 2.dp
            )
        } else {
            Text(
                text = text,
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp
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
        AuthEmailField(
            value = state.email,
            onValueChange = onEmailChange
        )
        Spacer(Modifier.height(12.dp))
        AuthTextField(
            value = state.password,
            onValueChange = onPasswordChange,
            placeholder = "Ton mot de passe",
            icon = LissafiIcons.Motdepasse,
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = onTogglePassword) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = null,
                        tint = TextSecondary,
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
            text = "Se connecter",
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
                color = Secondary,
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
                .background(Primary.copy(alpha = 0.07f))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = LissafiIcons.Succes,
                contentDescription = null,
                tint = Primary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = "Suis ta caisse partout : tes ventes, tes clients et tes dettes restent en sécurité.",
                color = Primary,
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
            icon = LissafiIcons.Boutique
        )
        Spacer(Modifier.height(12.dp))
        AuthEmailField(
            value = state.email,
            onValueChange = onEmailChange
        )
        Spacer(Modifier.height(12.dp))
        AuthTextField(
            value = state.password,
            onValueChange = onPasswordChange,
            placeholder = "Mot de passe (6 caractères ou plus)",
            icon = LissafiIcons.Motdepasse,
            keyboardType = KeyboardType.Password,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = onTogglePassword) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = null,
                        tint = TextSecondary,
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
            icon = LissafiIcons.Motdepasse,
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
            text = "Créer mon compte",
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
            color = Primary
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Entre ton email : on t'enverra un lien pour créer un nouveau mot de passe.",
            color = TextSecondary,
            fontSize = 13.sp,
            lineHeight = 18.sp
        )

        Spacer(Modifier.height(16.dp))
        AuthEmailField(
            value = state.email,
            onValueChange = onEmailChange,
            imeAction = ImeAction.Done
        )

        if (state.message != null) {
            Spacer(Modifier.height(12.dp))
            AuthMessage(state.message, state.isError)
        }

        Spacer(Modifier.height(16.dp))
        AuthResetButton(
            text = "Envoyer le lien",
            onClick = onSubmit,
            isLoading = state.isLoading
        )

        Spacer(Modifier.height(4.dp))
        TextButton(
            onClick = onSwitchToSignIn,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "← Retour à la connexion",
                color = Primary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
