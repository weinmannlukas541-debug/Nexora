package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.example.data.NexoraHelpers
import com.example.data.NexoraUpdate
import com.example.data.School
import com.example.ui.components.NexoraCard
import com.example.ui.components.NexoraFieldLabel
import com.example.ui.components.NexoraLogoBadge
import com.example.ui.components.NexoraNoticeBox
import com.example.ui.components.NexoraPrimaryButton
import com.example.ui.components.NexoraSecondaryButton
import com.example.ui.components.nexoraTextFieldColors
import com.example.ui.theme.NexoraBg
import com.example.ui.theme.NexoraBlueBright
import com.example.ui.theme.NexoraBlueLight
import com.example.ui.theme.NexoraCard
import com.example.ui.theme.NexoraCardElevated
import com.example.ui.theme.NexoraMuted
import com.example.ui.theme.NexoraText

@Composable
fun LoginScreen(
    schools: List<School>,
    updates: List<NexoraUpdate>,
    isBusy: Boolean,
    onLoadUpdates: () -> Unit,
    onLoginSchoolUser: (schoolId: String, username: String, password: String) -> Unit,
    onLoginAdmin: (email: String, password: String, onSuccess: () -> Unit) -> Unit,
    onLoginDirector: (name: String, password: String, onSuccess: () -> Unit) -> Unit
) {
    var selectedSchool by remember { mutableStateOf<School?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var showAdminDialog by remember { mutableStateOf(false) }
    var showDirectorDialog by remember { mutableStateOf(false) }
    var showUpdatesDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(NexoraBg, Color(0xFF0C1220), NexoraBg)
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            NexoraCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 460.dp),
                containerColor = NexoraCard.copy(alpha = 0.92f),
                contentPadding = PaddingValues(24.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(bottom = 20.dp)
                ) {
                    NexoraLogoBadge(size = 38.dp)
                    Text(
                        text = buildAnnotatedString {
                            withStyle(SpanStyle(color = NexoraBlueBright, fontWeight = FontWeight.ExtraBold)) {
                                append("Nexora")
                            }
                            withStyle(SpanStyle(color = NexoraText, fontWeight = FontWeight.ExtraBold)) {
                                append(".")
                            }
                        },
                        style = MaterialTheme.typography.headlineMedium
                    )
                }

                val currentSchool = selectedSchool
                if (currentSchool == null) {
                    Text(
                        text = "Schule auswählen",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Wähle deine Schule aus der Liste aus.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = NexoraMuted
                    )

                    NexoraFieldLabel("Schule suchen", modifier = Modifier.padding(top = 8.dp))
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Name oder Ort...", color = NexoraMuted) },
                        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = NexoraMuted) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = nexoraTextFieldColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("school_search_input")
                    )

                    Spacer(Modifier.height(14.dp))
                    val filtered = remember(schools, searchQuery) {
                        val q = searchQuery.trim().lowercase()
                        if (q.isEmpty()) schools
                        else schools.filter { (it.name + " " + it.city).lowercase().contains(q) }
                    }

                    if (filtered.isEmpty()) {
                        NexoraNoticeBox(
                            text = if (schools.isEmpty()) "Keine Schulen verfügbar." else "Keine Schule gefunden."
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 280.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(filtered, key = { it.id }) { school ->
                                NexoraCard(
                                    onClick = { selectedSchool = school },
                                    containerColor = NexoraCardElevated.copy(alpha = 0.55f),
                                    contentPadding = PaddingValues(14.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = school.name,
                                                style = MaterialTheme.typography.titleSmall,
                                                color = Color.White
                                            )
                                            Text(
                                                text = school.city.ifBlank { "Kein Ort" },
                                                style = MaterialTheme.typography.bodySmall,
                                                color = NexoraMuted
                                            )
                                        }
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                            contentDescription = "Schule wählen",
                                            tint = NexoraBlueLight
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(20.dp))
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Admin-Anmeldung",
                            style = MaterialTheme.typography.bodySmall.copy(textDecoration = TextDecoration.Underline),
                            color = NexoraMuted,
                            modifier = Modifier
                                .testTag("admin_login_link")
                                .clickable { showAdminDialog = true }
                                .padding(vertical = 4.dp, horizontal = 12.dp)
                        )
                        Text(
                            text = "Schuldirektor-Anmeldung",
                            style = MaterialTheme.typography.bodySmall.copy(textDecoration = TextDecoration.Underline),
                            color = NexoraMuted,
                            modifier = Modifier
                                .testTag("director_login_link")
                                .clickable { showDirectorDialog = true }
                                .padding(vertical = 4.dp, horizontal = 12.dp)
                        )
                    }
                } else {
                    SchoolCredentialForm(
                        school = currentSchool,
                        isBusy = isBusy,
                        onBack = { selectedSchool = null },
                        onLogin = { user, pass -> onLoginSchoolUser(currentSchool.id, user, pass) }
                    )
                }
            }
        }

        FloatingActionButton(
            onClick = {
                onLoadUpdates()
                showUpdatesDialog = true
            },
            containerColor = NexoraCardElevated,
            contentColor = NexoraText,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .testTag("login_updates_button")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Rounded.Campaign, contentDescription = "Updates", tint = NexoraBlueLight)
                Text("Update", style = MaterialTheme.typography.labelLarge)
            }
        }
    }

    if (showAdminDialog) {
        AdminLoginDialog(
            isBusy = isBusy,
            onDismiss = { showAdminDialog = false },
            onSubmit = { email, pass ->
                onLoginAdmin(email, pass) { showAdminDialog = false }
            }
        )
    }

    if (showDirectorDialog) {
        DirectorLoginDialog(
            isBusy = isBusy,
            onDismiss = { showDirectorDialog = false },
            onSubmit = { name, pass ->
                onLoginDirector(name, pass) { showDirectorDialog = false }
            }
        )
    }

    if (showUpdatesDialog) {
        LaunchedEffect(Unit) { onLoadUpdates() }
        LoginUpdatesDialog(
            updates = updates,
            onDismiss = { showUpdatesDialog = false }
        )
    }
}

@Composable
private fun SchoolCredentialForm(
    school: School,
    isBusy: Boolean,
    onBack: () -> Unit,
    onLogin: (String, String) -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        NexoraSecondaryButton(
            text = "← Zurück",
            onClick = onBack,
            testTag = "back_to_schools_button"
        )
        Spacer(Modifier.height(14.dp))
        NexoraCard(
            containerColor = NexoraCardElevated.copy(alpha = 0.55f),
            contentPadding = PaddingValues(14.dp)
        ) {
            Text(school.name, style = MaterialTheme.typography.titleSmall, color = Color.White)
            if (school.city.isNotBlank()) {
                Text(school.city, style = MaterialTheme.typography.bodySmall, color = NexoraMuted)
            }
        }

        Spacer(Modifier.height(16.dp))
        Text("Anmeldung", style = MaterialTheme.typography.headlineMedium, color = Color.White)
        Text(
            "Gib deine Zugangsdaten (Klasse oder Lehrer) ein.",
            style = MaterialTheme.typography.bodyMedium,
            color = NexoraMuted
        )

        NexoraFieldLabel("Benutzername / Kürzel")
        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            placeholder = { Text("z.B. 10a oder MUE", color = NexoraMuted) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            shape = RoundedCornerShape(12.dp),
            colors = nexoraTextFieldColors(),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("login_username_input")
        )

        NexoraFieldLabel("Passwort")
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            placeholder = { Text("••••••••", color = NexoraMuted) },
            singleLine = true,
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { showPassword = !showPassword }) {
                    Icon(
                        imageVector = if (showPassword) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                        contentDescription = "Passwort anzeigen",
                        tint = NexoraMuted
                    )
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = {
                if (!isBusy && username.isNotBlank()) onLogin(username, password)
            }),
            shape = RoundedCornerShape(12.dp),
            colors = nexoraTextFieldColors(),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("login_password_input")
        )

        Spacer(Modifier.height(22.dp))
        NexoraPrimaryButton(
            text = if (isBusy) "Anmelden..." else "Anmelden",
            enabled = !isBusy && username.isNotBlank() && password.isNotBlank(),
            onClick = { onLogin(username, password) },
            testTag = "login_submit_button"
        )
    }
}

@Composable
private fun AdminLoginDialog(
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (String, String) -> Unit
) {
    var email by remember { mutableStateOf("admin@untas.de") }
    var password by remember { mutableStateOf("") }
    var showPass by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NexoraCard,
        title = { Text("Admin-Anmeldung", style = MaterialTheme.typography.titleLarge, color = Color.White) },
        text = {
            Column {
                Text("Mit Supabase-Zugangsdaten einloggen.", style = MaterialTheme.typography.bodySmall, color = NexoraMuted)
                NexoraFieldLabel("E-Mail")
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                    shape = RoundedCornerShape(12.dp),
                    colors = nexoraTextFieldColors(),
                    modifier = Modifier.fillMaxWidth().testTag("admin_email_input")
                )
                NexoraFieldLabel("Passwort")
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    singleLine = true,
                    visualTransformation = if (showPass) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showPass = !showPass }) {
                            Icon(if (showPass) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility, contentDescription = "Passwort anzeigen", tint = NexoraMuted)
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { if (!isBusy) onSubmit(email, password) }),
                    shape = RoundedCornerShape(12.dp),
                    colors = nexoraTextFieldColors(),
                    modifier = Modifier.fillMaxWidth().testTag("admin_password_input")
                )
            }
        },
        confirmButton = {
            NexoraPrimaryButton(
                text = if (isBusy) "Prüfe..." else "Anmelden",
                enabled = !isBusy && email.isNotBlank() && password.isNotBlank(),
                onClick = { onSubmit(email, password) },
                modifier = Modifier.width(140.dp),
                testTag = "admin_login_submit"
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Abbrechen", color = NexoraMuted) }
        }
    )
}

@Composable
private fun DirectorLoginDialog(
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPass by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NexoraCard,
        title = { Text("Schuldirektor-Anmeldung", style = MaterialTheme.typography.titleLarge, color = Color.White) },
        text = {
            Column {
                Text("Melde dich mit deinem Direktor-Namen und Passwort an.", style = MaterialTheme.typography.bodySmall, color = NexoraMuted)
                NexoraFieldLabel("Direktor-Name")
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { Text("z. B. Max Mustermann", color = NexoraMuted) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = nexoraTextFieldColors(),
                    modifier = Modifier.fillMaxWidth().testTag("director_name_input")
                )
                NexoraFieldLabel("Passwort")
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    singleLine = true,
                    visualTransformation = if (showPass) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showPass = !showPass }) {
                            Icon(if (showPass) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility, contentDescription = "Passwort anzeigen", tint = NexoraMuted)
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { if (!isBusy) onSubmit(name, password) }),
                    shape = RoundedCornerShape(12.dp),
                    colors = nexoraTextFieldColors(),
                    modifier = Modifier.fillMaxWidth().testTag("director_password_input")
                )
            }
        },
        confirmButton = {
            NexoraPrimaryButton(
                text = if (isBusy) "Prüfe..." else "Anmelden",
                enabled = !isBusy && name.isNotBlank() && password.isNotBlank(),
                onClick = { onSubmit(name, password) },
                modifier = Modifier.width(140.dp),
                testTag = "director_login_submit"
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Abbrechen", color = NexoraMuted) }
        }
    )
}

@Composable
private fun LoginUpdatesDialog(
    updates: List<NexoraUpdate>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NexoraCard,
        title = {
            Column {
                Text("Updates", style = MaterialTheme.typography.titleLarge, color = Color.White)
                Text("Aktuelle Neuigkeiten und Änderungen an Nexora.", style = MaterialTheme.typography.bodySmall, color = NexoraMuted)
            }
        },
        text = {
            if (updates.isEmpty()) {
                NexoraNoticeBox("Noch keine Updates veröffentlicht.")
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 360.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(updates, key = { it.id }) { update ->
                        NexoraCard(containerColor = NexoraCardElevated, contentPadding = PaddingValues(14.dp)) {
                            Text(update.title, style = MaterialTheme.typography.titleMedium, color = Color.White)
                            if (update.createdAt.isNotBlank()) {
                                Text(
                                    NexoraHelpers.formatDateTime(update.createdAt),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = NexoraMuted
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(update.description, style = MaterialTheme.typography.bodyMedium, color = NexoraText)
                        }
                    }
                }
            }
        },
        confirmButton = {
            NexoraPrimaryButton(
                text = "Schließen",
                onClick = onDismiss,
                modifier = Modifier.width(130.dp)
            )
        }
    )
}
