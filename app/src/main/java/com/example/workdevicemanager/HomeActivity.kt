package com.example.workdevicemanager

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.workdevicemanager.ui.theme.WorkDeviceManagerTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class HomeActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            WorkDeviceManagerTheme {
                HomeScreen()
            }
        }
    }
}

@Composable
fun HomeScreen() {

    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val context = LocalContext.current

    // 🔴 ADMIN COMMAND LISTENER
    LaunchedEffect(Unit) {

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

                        val intent = Intent(context, MainActivity::class.java)
                        context.startActivity(intent)
                    }
                }
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.Center
    ) {

        Text(text = "Hello Welcome")

        Spacer(modifier = Modifier.height(20.dp))

        Button(onClick = {

            val userEmail = auth.currentUser?.email ?: "admin@company.com"

            val sdf = java.text.SimpleDateFormat("dd MMM yyyy hh:mm a", java.util.Locale.getDefault())
            val currentTime = sdf.format(java.util.Date())

            val logoutData = hashMapOf(
                "user_email" to userEmail,
                "logout_time" to currentTime
            )

            db.collection("logout_logs").add(logoutData)

            auth.signOut()

            Toast.makeText(context, "Logged Out", Toast.LENGTH_SHORT).show()

            val intent = Intent(context, MainActivity::class.java)
            context.startActivity(intent)

        }) {
            Text("Logout")
        }

    }
}