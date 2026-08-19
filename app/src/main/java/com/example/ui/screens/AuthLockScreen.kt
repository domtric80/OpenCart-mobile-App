package com.example.ui.screens

import android.widget.Toast
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.example.auth.AuthStatus
import com.example.auth.DeviceLockType
import com.example.auth.SecurityManager
import com.example.ui.theme.CardSurfacePure
import com.example.ui.theme.ColorSemanticGreen
import com.example.ui.theme.ColorSemanticOrange
import com.example.ui.theme.ColorSemanticRed
import com.example.ui.theme.ThemeOnPrimaryContainer
import com.example.ui.theme.ThemeOutlineVariant
import com.example.ui.theme.ThemePrimary
import com.example.ui.theme.ThemePrimaryContainer

@Composable
fun AuthLockScreen(
    authStatus: AuthStatus,
    securityManager: SecurityManager,
    onAuthSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var username by remember { mutableStateOf(securityManager.getOperatorUsername()) }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isCreatingNewPassword by remember { mutableStateOf(!authStatus.isPasswordConfigured || authStatus.isPasswordExpired) }

    val isBiometricAvailable = authStatus.lockType == DeviceLockType.STRONG_BIOMETRIC

    fun launchBiometricPrompt() {
        val activity = context as? FragmentActivity ?: return
        val executor = ContextCompat.getMainExecutor(context)
        val prompt = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                securityManager.recordSuccessfulAuth()
                onAuthSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                // Se annullato o fallito, può sempre digitare la password
            }
        })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Autenticazione CartAdmin")
            .setSubtitle("Sblocca per accedere al tuo negozio OpenCart")
            .setNegativeButtonText("Usa Password")
            .build()

        prompt.authenticate(promptInfo)
    }

    // Se la biometria è attiva e la password è già impostata, apri automaticamente il prompt biometrico
    LaunchedEffect(Unit) {
        if (isBiometricAvailable && authStatus.isPasswordConfigured && !authStatus.isPasswordExpired) {
            launchBiometricPrompt()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 24.dp, vertical = 24.dp)
            .testTag("auth_lock_screen"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Security Icon Header
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .background(ThemePrimaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isCreatingNewPassword) Icons.Default.Key else Icons.Default.Lock,
                    contentDescription = "Autenticazione Sicurezza",
                    tint = ThemePrimary,
                    modifier = Modifier.size(38.dp)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (isCreatingNewPassword) "Imposta Password di Accesso" else "Autenticazione Richiesta",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = "CartAdmin — OpenCart ITALIA by SOLO SOLUZIONI",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = ThemePrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp
                    )
                )
            }

            // Lock Type & Expiry Explanation Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(CardSurfacePure)
                    .border(1.dp, ThemeOutlineVariant, RoundedCornerShape(18.dp))
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = when (authStatus.lockType) {
                                DeviceLockType.STRONG_BIOMETRIC -> Icons.Default.Fingerprint
                                DeviceLockType.WEAK_DEVICE_CREDENTIAL -> Icons.Default.Security
                                DeviceLockType.NONE -> Icons.Default.Warning
                            },
                            contentDescription = null,
                            tint = when (authStatus.lockType) {
                                DeviceLockType.STRONG_BIOMETRIC -> ColorSemanticGreen
                                DeviceLockType.WEAK_DEVICE_CREDENTIAL -> ColorSemanticOrange
                                DeviceLockType.NONE -> ColorSemanticRed
                            },
                            modifier = Modifier.size(20.dp)
                        )

                        Text(
                            text = when (authStatus.lockType) {
                                DeviceLockType.STRONG_BIOMETRIC -> "Protezione Biometrica Attiva (Impronta/Face ID)"
                                DeviceLockType.WEAK_DEVICE_CREDENTIAL -> "Protezione Standard (PIN / Segno / Swipe)"
                                DeviceLockType.NONE -> "Nessun Blocco Schermo Rilevato"
                            },
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Text(
                        text = when (authStatus.lockType) {
                            DeviceLockType.STRONG_BIOMETRIC ->
                                "Il dispositivo dispone di biometria forte: la sessione non scade mai e l'accesso è protetto da sblocco biometrico."
                            DeviceLockType.WEAK_DEVICE_CREDENTIAL ->
                                "Dispositivo con PIN/Segno: la sessione scade dopo 72 ore di inattività e la password deve essere rinnovata ogni 3 mesi per massima sicurezza."
                            DeviceLockType.NONE ->
                                "Nessun blocco schermo impostato sul dispositivo: la password viene richiesta ad ogni apertura dell'applicazione."
                        },
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, lineHeight = 16.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (authStatus.expiryMessage != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "ℹ️ ${authStatus.expiryMessage}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = ColorSemanticOrange,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        )
                    }
                }
            }

            // Username & Password Input Fields
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (isCreatingNewPassword) {
                    OutlinedTextField(
                        value = username,
                        onValueChange = {
                            username = it
                            errorMessage = null
                        },
                        label = { Text("Nome Utente / Operatore") },
                        placeholder = { Text("es. admin o tuo nome operatore") },
                        singleLine = true,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Next
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auth_username_input"),
                        shape = RoundedCornerShape(14.dp)
                    )
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(horizontal = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = ThemePrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Operatore: ${securityManager.getOperatorUsername()}",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }
                }

                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        errorMessage = null
                    },
                    label = { Text(if (isCreatingNewPassword) "Crea Password Forte" else "Password di Accesso") },
                    placeholder = { Text("Min. 8 caratt., 1 Maiusc, 1 Num, 1 Simbolo") },
                    singleLine = true,
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = if (isCreatingNewPassword) ImeAction.Next else ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (!isCreatingNewPassword) {
                                if (securityManager.verifyPassword(password)) {
                                    onAuthSuccess()
                                } else {
                                    errorMessage = "Password errata. Riprova."
                                }
                            }
                        }
                    ),
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(
                                imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Mostra Password"
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("auth_password_input"),
                    shape = RoundedCornerShape(14.dp)
                )

                if (isCreatingNewPassword) {
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = {
                            confirmPassword = it
                            errorMessage = null
                        },
                        label = { Text("Conferma Password") },
                        placeholder = { Text("Ripeti la password digitata sopra") },
                        singleLine = true,
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auth_confirm_password_input"),
                        shape = RoundedCornerShape(14.dp)
                    )

                    // Password Rules Checklist
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        RuleCheckItem(label = "Almeno 8 caratteri", isMet = password.length >= 8)
                        RuleCheckItem(label = "Almeno una lettera MAIUSCOLA", isMet = password.any { it.isUpperCase() })
                        RuleCheckItem(label = "Almeno un numero (0-9)", isMet = password.any { it.isDigit() })
                        RuleCheckItem(label = "Almeno un simbolo speciale (!@#\$%^&*...)", isMet = password.any { "!@#$%^&*()_+-=[]{}|;:,.<>?/~`'\"".contains(it) })
                    }
                }

                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }

                // Primary Action Button
                Button(
                    onClick = {
                        if (isCreatingNewPassword) {
                            if (username.isBlank()) {
                                errorMessage = "Inserisci un nome utente o identificativo operatore."
                                return@Button
                            }
                            if (password != confirmPassword) {
                                errorMessage = "Le password inserite non coincidono."
                                return@Button
                            }
                            val check = securityManager.validatePasswordStrength(password)
                            if (!check.isValid) {
                                errorMessage = check.message
                                return@Button
                            }
                            securityManager.setCredentials(username, password)
                            Toast.makeText(context, "Profilo operatore impostato con successo!", Toast.LENGTH_SHORT).show()
                            onAuthSuccess()
                        } else {
                            if (securityManager.verifyPassword(password)) {
                                onAuthSuccess()
                            } else {
                                errorMessage = "Password non corretta. Riprova."
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("auth_submit_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ThemePrimary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(
                        imageVector = if (isCreatingNewPassword) Icons.Default.CheckCircle else Icons.Default.LockOpen,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isCreatingNewPassword) "Salva & Accedi" else "Sblocca CartAdmin",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    )
                }

                // Optional Biometric Button if available
                if (isBiometricAvailable && !isCreatingNewPassword) {
                    OutlinedButton(
                        onClick = { launchBiometricPrompt() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("auth_biometric_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.Fingerprint, contentDescription = null, tint = ThemePrimary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Accedi con Biometria / Face ID",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RuleCheckItem(label: String, isMet: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = if (isMet) Icons.Default.CheckCircle else Icons.Default.Warning,
            contentDescription = null,
            tint = if (isMet) ColorSemanticGreen else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                fontWeight = if (isMet) FontWeight.Bold else FontWeight.Normal,
                color = if (isMet) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
    }
}
