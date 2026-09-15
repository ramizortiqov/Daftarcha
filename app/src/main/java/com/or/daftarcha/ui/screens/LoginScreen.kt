package com.or.daftarcha.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.or.daftarcha.viewmodel.AuthViewModel

@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onLoginSuccess: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current

    // Screen mode: Login or Register Brigadier
    var isRegisteringBrigadier by remember { mutableStateOf(false) }

    // Login fields
    var loginId by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    // Register fields
    var regName by remember { mutableStateOf("") }
    var regLoginId by remember { mutableStateOf("") }
    var regPassword by remember { mutableStateOf("") }
    var regConfirmPassword by remember { mutableStateOf("") }
    var regPhone by remember { mutableStateOf("") }
    var regPasswordVisible by remember { mutableStateOf(false) }
    var regConfirmPasswordVisible by remember { mutableStateOf(false) }
    var localError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            onLoginSuccess()
        }
    }

    Scaffold { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 500.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (!isRegisteringBrigadier) {
                        // ==================== LOGIN VIEW ====================
                        Text(
                            text = "ДАФТАРЧА",
                            style = MaterialTheme.typography.displaySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Box(
                            modifier = Modifier
                                .height(4.dp)
                                .width(72.dp)
                                .background(MaterialTheme.colorScheme.primary)
                        )

                        Text(
                            text = "Логин ва паролни киритиб тизимга киринг",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (uiState.errorMessage != null) {
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ErrorOutline,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = uiState.errorMessage ?: "",
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }

                        // Login ID / Name field
                        OutlinedTextField(
                            value = loginId,
                            onValueChange = {
                                loginId = it
                                if (uiState.errorMessage != null) viewModel.clearError()
                            },
                            label = { Text("Логин") },
                            placeholder = { Text("Логин") },
                            leadingIcon = {
                                Icon(Icons.Default.Badge, contentDescription = null)
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { focusManager.moveFocus(FocusDirection.Down) }
                            ),
                            modifier = Modifier.fillMaxWidth(),
                        )

                        // Password field
                        OutlinedTextField(
                            value = password,
                            onValueChange = {
                                password = it
                                if (uiState.errorMessage != null) viewModel.clearError()
                            },
                            label = { Text("Парол") },
                            leadingIcon = {
                                Icon(Icons.Default.VpnKey, contentDescription = null)
                            },
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = if (passwordVisible) "Паролни яшириш" else "Паролни кўрсатиш"
                                    )
                                }
                            },
                            singleLine = true,
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    focusManager.clearFocus()
                                    viewModel.login(loginId, password)
                                }
                            ),
                            modifier = Modifier.fillMaxWidth(),
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Button(
                            onClick = {
                                focusManager.clearFocus()
                                viewModel.login(loginId, password)
                            },
                            enabled = !uiState.isLoading && loginId.isNotBlank() && password.isNotBlank(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = androidx.compose.ui.graphics.RectangleShape
                        ) {
                            if (uiState.isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    "КИРИШ",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Self-registration for brigadiers has been disabled on purpose:
                        // accounts are now issued only by the admin (via the management bot),
                        // which writes directly to the app_users collection in Firestore.
                        // The "isRegisteringBrigadier" branch below is intentionally kept
                        // unreachable rather than deleted, in case this is ever revisited.

                    } else {
                        // ==================== REGISTER BRIGADIER VIEW ====================
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    viewModel.clearError()
                                    localError = null
                                    isRegisteringBrigadier = false
                                }
                            ) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Орқага")
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Устони рўйхатдан ўтказиш",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Исм ва паролингизни киритинг. Сиз автоматик тарзда шериклар рўйхатига ҳам киритиласиз.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        val activeError = localError ?: uiState.errorMessage
                        if (activeError != null) {
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ErrorOutline,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = activeError,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }

                        // Name field (Required)
                        OutlinedTextField(
                            value = regName,
                            onValueChange = {
                                regName = it
                                localError = null
                                if (uiState.errorMessage != null) viewModel.clearError()
                            },
                            label = { Text("Исм (Усто исми) *") },
                            placeholder = { Text("масалан: Алишер") },
                            leadingIcon = {
                                Icon(Icons.Default.Person, contentDescription = null)
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { focusManager.moveFocus(FocusDirection.Down) }
                            ),
                            modifier = Modifier.fillMaxWidth(),
                        )

                        // Login ID field (Optional - defaults to name or brigadier)
                        OutlinedTextField(
                            value = regLoginId,
                            onValueChange = {
                                regLoginId = it
                                localError = null
                                if (uiState.errorMessage != null) viewModel.clearError()
                            },
                            label = { Text("Логин / ID (ихтиёрий)") },
                            placeholder = { Text("масалан: alisher ёки brigadier") },
                            supportingText = {
                                Text("Бўш қолдирилса, исм асосида яратилади")
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Badge, contentDescription = null)
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { focusManager.moveFocus(FocusDirection.Down) }
                            ),
                            modifier = Modifier.fillMaxWidth(),
                        )

                        // Password field
                        OutlinedTextField(
                            value = regPassword,
                            onValueChange = {
                                regPassword = it
                                localError = null
                                if (uiState.errorMessage != null) viewModel.clearError()
                            },
                            label = { Text("Парол *") },
                            leadingIcon = {
                                Icon(Icons.Default.VpnKey, contentDescription = null)
                            },
                            trailingIcon = {
                                IconButton(onClick = { regPasswordVisible = !regPasswordVisible }) {
                                    Icon(
                                        imageVector = if (regPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = if (regPasswordVisible) "Паролни яшириш" else "Паролни кўрсатиш"
                                    )
                                }
                            },
                            singleLine = true,
                            visualTransformation = if (regPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { focusManager.moveFocus(FocusDirection.Down) }
                            ),
                            modifier = Modifier.fillMaxWidth(),
                        )

                        // Confirm password field
                        OutlinedTextField(
                            value = regConfirmPassword,
                            onValueChange = {
                                regConfirmPassword = it
                                localError = null
                                if (uiState.errorMessage != null) viewModel.clearError()
                            },
                            label = { Text("Паролни тасдиқланг *") },
                            leadingIcon = {
                                Icon(Icons.Default.Lock, contentDescription = null)
                            },
                            trailingIcon = {
                                IconButton(onClick = { regConfirmPasswordVisible = !regConfirmPasswordVisible }) {
                                    Icon(
                                        imageVector = if (regConfirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = if (regConfirmPasswordVisible) "Паролни яшириш" else "Паролни кўрсатиш"
                                    )
                                }
                            },
                            singleLine = true,
                            visualTransformation = if (regConfirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    focusManager.clearFocus()
                                }
                            ),
                            modifier = Modifier.fillMaxWidth(),
                        )

                        // Phone field (Optional)
                        OutlinedTextField(
                            value = regPhone,
                            onValueChange = {
                                regPhone = it
                                localError = null
                                if (uiState.errorMessage != null) viewModel.clearError()
                            },
                            label = { Text("Телефон (ихтиёрий)") },
                            placeholder = { Text("+998 90 123 45 67") },
                            leadingIcon = {
                                Icon(Icons.Default.Phone, contentDescription = null)
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Phone,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    focusManager.clearFocus()
                                }
                            ),
                            modifier = Modifier.fillMaxWidth(),
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Button(
                            onClick = {
                                focusManager.clearFocus()
                                if (regName.isBlank()) {
                                    localError = "Исмни киритинг"
                                    return@Button
                                }
                                if (regPassword.isBlank()) {
                                    localError = "Паролни киритинг"
                                    return@Button
                                }
                                if (regPassword != regConfirmPassword) {
                                    localError = "Пароллар бир хил эмас"
                                    return@Button
                                }
                                viewModel.registerBrigadier(
                                    name = regName,
                                    loginId = regLoginId.takeIf { it.isNotBlank() },
                                    pass = regPassword,
                                    phone = regPhone.takeIf { it.isNotBlank() }
                                )
                            },
                            enabled = !uiState.isLoading && regName.isNotBlank() && regPassword.isNotBlank(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = androidx.compose.ui.graphics.RectangleShape
                        ) {
                            if (uiState.isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    "РЎЙХАТДАН ЎТИШ ВА КИРИШ",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        TextButton(
                            onClick = {
                                viewModel.clearError()
                                localError = null
                                isRegisteringBrigadier = false
                            }
                        ) {
                            Text("Кириш ойнасига қайтиш")
                        }
                    }
                }
            }
        }
    }
}
