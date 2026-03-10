package com.example.workdevicemanager

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.workdevicemanager.ui.theme.WorkDeviceManagerTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.analytics.FirebaseAnalytics

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            WorkDeviceManagerTheme {
                LoginScreen()
            }
        }
    }
}

@Composable
fun LoginScreen() {

    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val context = LocalContext.current
    val analytics = FirebaseAnalytics.getInstance(context)

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.Center
    ) {

        Text(text = "Work Device Login")

        Spacer(modifier = Modifier.height(20.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") }
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") }
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(onClick = {

            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->

                    if (task.isSuccessful) {

                        analytics.logEvent("login_success", null)

                        val userEmail = auth.currentUser?.email

                        val deviceModel = Build.MODEL
                        val deviceManufacturer = Build.MANUFACTURER
                        val androidVersion = Build.VERSION.RELEASE

                        val deviceId = Settings.Secure.getString(
                            context.contentResolver,
                            Settings.Secure.ANDROID_ID
                        )

                        val deviceData = hashMapOf(
                            "user_email" to userEmail,
                            "device_id" to deviceId,
                            "device_model" to deviceModel,
                            "manufacturer" to deviceManufacturer,
                            "android_version" to androidVersion
                        )

                        db.collection("devices").add(deviceData)

                        val sdf = java.text.SimpleDateFormat("dd MMM yyyy hh:mm a", java.util.Locale.getDefault())
                        val currentTime = sdf.format(java.util.Date())

                        val logData = hashMapOf(
                            "user_email" to userEmail,
                            "login_time" to currentTime
                        )

                        db.collection("login_logs").add(logData)

                        val installedApps = getInstalledApps(context)

                        val appData = hashMapOf(
                            "user_email" to userEmail,
                            "device_id" to deviceId,
                            "apps" to installedApps
                        )

                        db.collection("installed_apps").add(appData)

                        db.collection("blocked_apps")
                            .document("apps")
                            .addSnapshotListener { snapshot, _ ->

                                if (snapshot != null && snapshot.exists()) {

                                    val blockedApps =
                                        snapshot.get("apps_list") as? List<String> ?: emptyList()

                                    val foundBlockedApps =
                                        installedApps.filter { it in blockedApps }

                                    if (foundBlockedApps.isNotEmpty()) {

                                        analytics.logEvent("blocked_app_detected", null)

                                        Toast.makeText(
                                            context,
                                            "Blocked apps detected: $foundBlockedApps",
                                            Toast.LENGTH_LONG
                                        ).show()

                                    }

                                }

                            }

                        db.collection("allowed_users")
                            .whereEqualTo("email", userEmail)
                            .get()
                            .addOnSuccessListener { documents ->

                                if (!documents.isEmpty) {

                                    Toast.makeText(context, "Access Granted", Toast.LENGTH_SHORT).show()
                                    listenForAdminCommands(context, auth, db)

                                    val intent = Intent(context, HomeActivity::class.java)
                                    context.startActivity(intent)

                                } else {

                                    Toast.makeText(context, "Access Denied", Toast.LENGTH_SHORT).show()
                                    auth.signOut()

                                }

                            }

                    } else {

                        analytics.logEvent("login_failed", null)

                        Toast.makeText(context, "Login Failed", Toast.LENGTH_SHORT).show()

                    }

                }

        }) {
            Text("Login")
        }

    }
}

fun getInstalledApps(context: android.content.Context): List<String> {

    val packageManager = context.packageManager
    val packages = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)

    val appList = mutableListOf<String>()

    for (packageInfo in packages) {

        val appName = packageManager.getApplicationLabel(packageInfo).toString()

        appList.add(appName)
    }

    return appList
}

fun listenForAdminCommands(
    context: android.content.Context,
    auth: FirebaseAuth,
    db: FirebaseFirestore
) {

    db.collection("admin_commands")
        .document("command")
        .addSnapshotListener { snapshot, _ ->

            if (snapshot != null && snapshot.exists()) {

                val command = snapshot.getString("command")

                if (command == "logout_now") {

                    auth.signOut()

                    Toast.makeText(
                        context,
                        "Admin logged you out",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
}