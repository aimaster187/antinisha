package com.example.ui.screens

import android.app.Activity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.CrimsonSoft
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.FaGlyphs
import com.example.ui.theme.FaIcon
import com.example.ui.theme.FaStyle
import com.example.ui.theme.IceBlueDeep
import com.example.ui.theme.IndigoMuted
import com.example.ui.theme.LavenderGlow
import com.example.ui.theme.NishaGlassCard
import com.example.ui.theme.NishaSoftAuraGradient
import com.example.ui.theme.NishaStatusPill
import com.example.ui.theme.ObsidianSlate
import com.example.ui.theme.PearlWhite

@Composable
fun AuthOnboardingScreen(
    onboardingCompleted: Boolean,
    authLoading: Boolean,
    authMessage: String?,
    hasApiKey: Boolean,
    apiKeyStatus: String?,
    onCompleteOnboarding: () -> Unit,
    onGoogleSignIn: (Activity) -> Unit,
    onSignUpLocal: (String, String, String, String, String, String) -> Unit,
    onSignInLocal: (String, String) -> Unit,
    onResetLocalPassword: (String, String, String) -> Unit,
    onSaveAndVerifyKey: (String) -> Unit
) {
    val context = LocalContext.current
    val scroll = rememberScrollState()
    var authTab by remember { mutableIntStateOf(0) } // 0 = Sign In, 1 = Sign Up, 2 = Forgot/Phone OTP
    var identifier by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var apiKeyInput by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NishaSoftAuraGradient)
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scroll)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(modifier = Modifier.widthIn(max = 560.dp)) {
                // Welcome Hero Card
                NishaGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Image(
                            painter = painterResource(id = R.drawable.img_welcome_hero),
                            contentDescription = "NISHA AI Maya 3D Core showcase",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(168.dp)
                                .clip(RoundedCornerShape(16.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(Modifier.height(14.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            NishaStatusPill(
                                label = "LUXURY AI OS",
                                iconGlyph = FaGlyphs.ATOM,
                                tint = ElectricViolet,
                                background = LavenderGlow
                            )
                            Spacer(Modifier.width(8.dp))
                            NishaStatusPill(
                                label = "BYOK KEYSTORE",
                                iconGlyph = FaGlyphs.SHIELD_HALVED,
                                tint = IceBlueDeep,
                                background = com.example.ui.theme.IceBlueSoft
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = "NISHA AI — THE DARK AI",
                            style = MaterialTheme.typography.headlineLarge,
                            color = ObsidianSlate
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Voice-first personal AI operating environment with Maya 3D Core, 10D Floating Orb, Four-Edge Lighting, 5-Agent Orchestrator, and Keystore-encrypted Gemini BYOK.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = IndigoMuted
                        )
                        if (!onboardingCompleted) {
                            Spacer(Modifier.height(10.dp))
                            OutlinedButton(
                                onClick = onCompleteOnboarding,
                                modifier = Modifier.testTag("complete_onboarding_button")
                            ) {
                                FaIcon(FaGlyphs.CHECK, contentDescription = null, size = 14.sp)
                                Spacer(Modifier.width(8.dp))
                                Text("Acknowledge Welcome Guide")
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Google Sign-In + Auth Card
                NishaGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "Authenticate Session",
                            style = MaterialTheme.typography.titleLarge,
                            color = ObsidianSlate
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Use Google Sign-In for cloud Firestore sync and Admin access, or use the Local PBKDF2 Vault fallback offline.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = IndigoMuted
                        )
                        Spacer(Modifier.height(14.dp))

                        Button(
                            onClick = { (context as? Activity)?.let { onGoogleSignIn(it) } },
                            enabled = !authLoading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("google_signin_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            if (authLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = PearlWhite,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                FaIcon(
                                    glyph = FaGlyphs.BRAND_GOOGLE,
                                    contentDescription = null,
                                    color = PearlWhite,
                                    size = 16.sp,
                                    style = FaStyle.BRANDS
                                )
                                Spacer(Modifier.width(10.dp))
                                Text("Sign in with Google (Cloud Sync)", fontWeight = FontWeight.SemiBold)
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("Local Sign In", "Create Vault", "Reset / OTP").forEachIndexed { idx, title ->
                                FilterChip(
                                    selected = authTab == idx,
                                    onClick = { authTab = idx },
                                    label = { Text(title, maxLines = 1) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        if (!authMessage.isNullOrBlank()) {
                            Surface(
                                color = CrimsonSoft,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    FaIcon(FaGlyphs.TRIANGLE_EXCLAMATION, contentDescription = null, color = CrimsonAlert, size = 14.sp)
                                    Spacer(Modifier.width(8.dp))
                                    Text(authMessage, style = MaterialTheme.typography.bodyMedium, color = CrimsonAlert)
                                }
                            }
                        }

                        when (authTab) {
                            0 -> {
                                OutlinedTextField(
                                    value = identifier,
                                    onValueChange = { identifier = it },
                                    label = { Text("Username or Email") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("local_login_identifier"),
                                    singleLine = true
                                )
                                Spacer(Modifier.height(10.dp))
                                OutlinedTextField(
                                    value = password,
                                    onValueChange = { password = it },
                                    label = { Text("Password") },
                                    visualTransformation = PasswordVisualTransformation(),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("local_login_password"),
                                    singleLine = true
                                )
                                Spacer(Modifier.height(12.dp))
                                OutlinedButton(
                                    onClick = { onSignInLocal(identifier, password) },
                                    enabled = !authLoading,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("local_signin_button")
                                ) {
                                    FaIcon(FaGlyphs.LOCK, contentDescription = null, size = 14.sp)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Sign In (Local Vault Fallback)")
                                }
                            }
                            1 -> {
                                OutlinedTextField(
                                    value = username,
                                    onValueChange = { username = it },
                                    label = { Text("Username") },
                                    modifier = Modifier.fillMaxWidth().testTag("signup_username"),
                                    singleLine = true
                                )
                                Spacer(Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = fullName,
                                    onValueChange = { fullName = it },
                                    label = { Text("Full Name") },
                                    modifier = Modifier.fillMaxWidth().testTag("signup_fullname"),
                                    singleLine = true
                                )
                                Spacer(Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = email,
                                    onValueChange = { email = it },
                                    label = { Text("Email Address") },
                                    modifier = Modifier.fillMaxWidth().testTag("signup_email"),
                                    singleLine = true
                                )
                                Spacer(Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = phone,
                                    onValueChange = { phone = it },
                                    label = { Text("WhatsApp / Phone Number") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                                Spacer(Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = password,
                                    onValueChange = { password = it },
                                    label = { Text("Password (8+ chars)") },
                                    visualTransformation = PasswordVisualTransformation(),
                                    modifier = Modifier.fillMaxWidth().testTag("signup_password"),
                                    singleLine = true
                                )
                                Spacer(Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = confirmPassword,
                                    onValueChange = { confirmPassword = it },
                                    label = { Text("Confirm Password") },
                                    visualTransformation = PasswordVisualTransformation(),
                                    modifier = Modifier.fillMaxWidth().testTag("signup_confirm"),
                                    singleLine = true
                                )
                                Spacer(Modifier.height(12.dp))
                                Button(
                                    onClick = { onSignUpLocal(username, fullName, email, phone, password, confirmPassword) },
                                    enabled = !authLoading,
                                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("local_signup_button"),
                                    colors = ButtonDefaults.buttonColors(containerColor = IceBlueDeep)
                                ) {
                                    FaIcon(FaGlyphs.USER, contentDescription = null, color = PearlWhite, size = 14.sp)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Create Local Encrypted Account")
                                }
                            }
                            else -> {
                                OutlinedTextField(
                                    value = identifier,
                                    onValueChange = { identifier = it },
                                    label = { Text("Username or Email for Password Reset") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                                Spacer(Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = password,
                                    onValueChange = { password = it },
                                    label = { Text("New Password (8+ chars)") },
                                    visualTransformation = PasswordVisualTransformation(),
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                                Spacer(Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = confirmPassword,
                                    onValueChange = { confirmPassword = it },
                                    label = { Text("Confirm New Password") },
                                    visualTransformation = PasswordVisualTransformation(),
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                                Spacer(Modifier.height(10.dp))
                                OutlinedButton(
                                    onClick = { onResetLocalPassword(identifier, password, confirmPassword) },
                                    modifier = Modifier.fillMaxWidth().height(48.dp)
                                ) {
                                    FaIcon(FaGlyphs.KEY, contentDescription = null, size = 14.sp)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Reset Local Vault Password")
                                }
                                Spacer(Modifier.height(10.dp))
                                Surface(
                                    color = LavenderGlow,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = "Cloud Email/Password & Phone OTP Info",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = ElectricViolet
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            text = "Cloud Phone OTP & Email/Password providers must be enabled in Firebase Console (Authentication -> Sign-in method) and username lookup requires a Firebase Cloud Function. Google Sign-In and PBKDF2 Local Vault are active right now.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = IndigoMuted
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Optional Pre-Login BYOK Gemini Key Card
                NishaGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            FaIcon(FaGlyphs.KEY, contentDescription = null, color = ElectricViolet, size = 16.sp)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Gemini API Key (Android Keystore)",
                                style = MaterialTheme.typography.titleMedium,
                                color = ObsidianSlate
                            )
                            Spacer(Modifier.weight(1f))
                            if (hasApiKey) {
                                NishaStatusPill("Ready", FaGlyphs.CIRCLE_CHECK, tint = EmeraldSuccess, background = com.example.ui.theme.EmeraldSoft)
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "NISHA AI uses your own Gemini API key encrypted via AES-256-GCM in Android Keystore. You can add or change it here or later in Settings.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = IndigoMuted
                        )
                        Spacer(Modifier.height(10.dp))
                        OutlinedTextField(
                            value = apiKeyInput,
                            onValueChange = { apiKeyInput = it },
                            label = { Text("AIza... Gemini API Key") },
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth().testTag("onboarding_api_key_input"),
                            singleLine = true
                        )
                        if (!apiKeyStatus.isNullOrBlank()) {
                            Spacer(Modifier.height(6.dp))
                            Text(apiKeyStatus, style = MaterialTheme.typography.bodyMedium, color = ElectricViolet)
                        }
                        Spacer(Modifier.height(8.dp))
                        TextButton(
                            onClick = { onSaveAndVerifyKey(apiKeyInput) },
                            enabled = apiKeyInput.isNotBlank()
                        ) {
                            FaIcon(FaGlyphs.SHIELD_HALVED, contentDescription = null, size = 14.sp)
                            Spacer(Modifier.width(6.dp))
                            Text("Verify & Encrypt Key")
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}
