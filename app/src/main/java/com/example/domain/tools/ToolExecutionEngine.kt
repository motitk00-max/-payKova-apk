package com.example.domain.tools

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.database.Cursor
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.provider.AlarmClock
import android.provider.ContactsContract
import android.provider.Settings
import android.util.Log
import androidx.core.content.ContextCompat
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ToolExecutionEngine(
    private val context: Context
) {
    companion object {
        private const val TAG = "KovaToolEngine"

        // App alias directory
        private val KNOWN_APP_PACKAGES = mapOf(
            "youtube" to "com.google.android.youtube",
            "instagram" to "com.instagram.android",
            "whatsapp" to "com.whatsapp",
            "whatsapp business" to "com.whatsapp.w4b",
            "calculator" to "com.google.android.calculator",
            "calc" to "com.google.android.calculator",
            "gmail" to "com.google.android.gm",
            "email" to "com.google.android.gm",
            "maps" to "com.google.android.apps.maps",
            "google maps" to "com.google.android.apps.maps",
            "chrome" to "com.android.chrome",
            "camera" to "com.android.camera",
            "photos" to "com.google.android.apps.photos",
            "gallery" to "com.google.android.apps.photos",
            "clock" to "com.google.android.deskclock",
            "settings" to "com.android.settings",
            "play store" to "com.android.vending",
            "spotify" to "com.spotify.music"
        )
    }

    val registeredTools: List<KovaToolDefinition> = listOf(
        KovaToolDefinition(
            name = "openApp",
            description = "Opens an installed Android application like YouTube, Instagram, WhatsApp, Calculator, Gmail, Maps, etc.",
            parameters = mapOf(
                "packageNameOrName" to ToolParam("STRING", "Package name or common name of the app (e.g., 'youtube', 'instagram', 'calculator', 'com.whatsapp')")
            ),
            required = listOf("packageNameOrName")
        ),
        KovaToolDefinition(
            name = "searchAndCallContact",
            description = "Finds a contact from user's phonebook by name and initiates a phone call. If multiple contacts match, asks user to clarify.",
            parameters = mapOf(
                "contactName" to ToolParam("STRING", "Name of the person to call (e.g., 'Rahul', 'Mummy', 'Priya')")
            ),
            required = listOf("contactName"),
            requiresPermission = Manifest.permission.CALL_PHONE,
            isSensitiveAction = true
        ),
        KovaToolDefinition(
            name = "sendWhatsAppMessage",
            description = "Prepares and opens a WhatsApp chat for a contact with pre-filled message text.",
            parameters = mapOf(
                "contactName" to ToolParam("STRING", "Name of the contact in phonebook"),
                "message" to ToolParam("STRING", "Message content to send")
            ),
            required = listOf("contactName", "message"),
            isSensitiveAction = true
        ),
        KovaToolDefinition(
            name = "sendGmail",
            description = "Opens Gmail/Email composer with recipient email, subject, and message pre-filled.",
            parameters = mapOf(
                "recipientEmail" to ToolParam("STRING", "Target email address"),
                "subject" to ToolParam("STRING", "Email subject line"),
                "body" to ToolParam("STRING", "Email body content")
            ),
            required = listOf("recipientEmail", "subject", "body")
        ),
        KovaToolDefinition(
            name = "getBatteryStatus",
            description = "Retrieves the current phone battery percentage and charging status.",
            parameters = emptyMap()
        ),
        KovaToolDefinition(
            name = "getCurrentTime",
            description = "Retrieves the current accurate time, date, and day.",
            parameters = emptyMap()
        ),
        KovaToolDefinition(
            name = "controlFlashlight",
            description = "Turns the phone flashlight (torch) ON or OFF.",
            parameters = mapOf(
                "enable" to ToolParam("BOOLEAN", "True to turn on flashlight, False to turn off")
            ),
            required = listOf("enable")
        ),
        KovaToolDefinition(
            name = "openSettings",
            description = "Opens device settings (WiFi, Bluetooth, Battery, Display, or Main Settings).",
            parameters = mapOf(
                "settingType" to ToolParam("STRING", "Type of setting: 'wifi', 'bluetooth', 'battery', 'display', or 'general'")
            )
        ),
        KovaToolDefinition(
            name = "openUrl",
            description = "Opens a web link or search query in browser.",
            parameters = mapOf(
                "url" to ToolParam("STRING", "Full URL or search query")
            ),
            required = listOf("url")
        ),
        KovaToolDefinition(
            name = "setTimer",
            description = "Sets a countdown timer in the clock app.",
            parameters = mapOf(
                "seconds" to ToolParam("INTEGER", "Timer duration in seconds"),
                "label" to ToolParam("STRING", "Timer label or purpose")
            ),
            required = listOf("seconds")
        )
    )

    fun executeTool(toolName: String, arguments: Map<String, Any?>): ToolExecutionResult {
        Log.i(TAG, "Executing tool '$toolName' with args: $arguments")
        return when (toolName) {
            "openApp" -> {
                val appQuery = arguments["packageNameOrName"]?.toString() ?: ""
                openApp(appQuery)
            }
            "searchAndCallContact" -> {
                val contactName = arguments["contactName"]?.toString() ?: ""
                searchAndCallContact(contactName)
            }
            "sendWhatsAppMessage" -> {
                val contactName = arguments["contactName"]?.toString() ?: ""
                val message = arguments["message"]?.toString() ?: ""
                sendWhatsAppMessage(contactName, message)
            }
            "sendGmail" -> {
                val email = arguments["recipientEmail"]?.toString() ?: ""
                val subject = arguments["subject"]?.toString() ?: ""
                val body = arguments["body"]?.toString() ?: ""
                sendGmail(email, subject, body)
            }
            "getBatteryStatus" -> getBatteryStatus()
            "getCurrentTime" -> getCurrentTime()
            "controlFlashlight" -> {
                val enable = arguments["enable"] as? Boolean ?: (arguments["enable"]?.toString()?.toBooleanStrictOrNull() ?: true)
                controlFlashlight(enable)
            }
            "openSettings" -> {
                val settingType = arguments["settingType"]?.toString() ?: "general"
                openSettings(settingType)
            }
            "openUrl" -> {
                val url = arguments["url"]?.toString() ?: ""
                openUrl(url)
            }
            "setTimer" -> {
                val seconds = (arguments["seconds"] as? Number)?.toInt()
                    ?: arguments["seconds"]?.toString()?.toIntOrNull() ?: 60
                val label = arguments["label"]?.toString() ?: "Timer"
                setTimer(seconds, label)
            }
            else -> ToolExecutionResult.Error(
                "Unknown tool: $toolName",
                "Boss, ye action mere system mein registered nahi hai."
            )
        }
    }

    private fun openApp(query: String): ToolExecutionResult {
        if (query.isBlank()) {
            return ToolExecutionResult.Error("Empty app name", "Kaunsa app open karna hai?")
        }

        val normalizedQuery = query.lowercase().trim()
        val targetPackage = KNOWN_APP_PACKAGES[normalizedQuery] ?: query

        val pm = context.packageManager
        var launchIntent = pm.getLaunchIntentForPackage(targetPackage)

        // If direct package failed, search installed apps by label
        if (launchIntent == null) {
            val installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            for (app in installedApps) {
                val appLabel = pm.getApplicationLabel(app).toString().lowercase()
                if (appLabel == normalizedQuery || appLabel.contains(normalizedQuery)) {
                    launchIntent = pm.getLaunchIntentForPackage(app.packageName)
                    if (launchIntent != null) break
                }
            }
        }

        // Handle Calculator special fallback intent
        if (launchIntent == null && (normalizedQuery.contains("calc") || normalizedQuery.contains("calculator"))) {
            launchIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_APP_CALCULATOR)
            }
        }

        return if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            try {
                context.startActivity(launchIntent)
                ToolExecutionResult.Success(
                    message = "Launched $targetPackage",
                    conversationalResponse = "Done. $query open kar diya!",
                    details = mapOf("package" to targetPackage)
                )
            } catch (e: Exception) {
                ToolExecutionResult.Error(
                    "Launch failed: ${e.message}",
                    "App kholne mein dikkat aayi: ${e.message}"
                )
            }
        } else {
            ToolExecutionResult.Error(
                "App not installed: $query",
                "Boss, aapke phone mein '$query' install nahi lag raha."
            )
        }
    }

    private fun searchAndCallContact(contactName: String): ToolExecutionResult {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            return ToolExecutionResult.PermissionNeeded(
                Manifest.permission.READ_CONTACTS,
                "Contacts search karne ke liye contact permission chahiye, boss."
            )
        }

        val matchingContacts = queryContacts(contactName)

        if (matchingContacts.isEmpty()) {
            return ToolExecutionResult.Error(
                "Contact not found: $contactName",
                "Mujhe '$contactName' naam ka koi contact nahi mila."
            )
        }

        if (matchingContacts.size > 1) {
            val options = matchingContacts.map {
                mapOf("id" to it.id, "name" to it.name, "phone" to it.phoneNumber)
            }
            val namesStr = matchingContacts.joinToString(" ya ") { "${it.name} (${it.phoneNumber.takeLast(4)})" }
            return ToolExecutionResult.DisambiguationNeeded(
                question = "$contactName ke multiple numbers mile: $namesStr. Kisko call lagau?",
                options = options
            )
        }

        val target = matchingContacts.first()
        return ToolExecutionResult.ConfirmationNeeded(
            prompt = "${target.name} (${target.phoneNumber}) ko call mila doon?",
            toolName = "callPhoneDirect",
            target = target.name,
            parameters = mapOf("phoneNumber" to target.phoneNumber, "name" to target.name)
        )
    }

    fun callPhoneDirect(phoneNumber: String, name: String = ""): ToolExecutionResult {
        val hasCallPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED

        val intent = if (hasCallPermission) {
            Intent(Intent.ACTION_CALL, Uri.parse("tel:$phoneNumber"))
        } else {
            Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phoneNumber"))
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        return try {
            context.startActivity(intent)
            ToolExecutionResult.Success(
                message = "Calling $phoneNumber",
                conversationalResponse = if (name.isNotBlank()) "Call mil rahi hai $name ko." else "Call lag gayi."
            )
        } catch (e: Exception) {
            ToolExecutionResult.Error("Call failed: ${e.message}", "Call lagane mein issue aaya.")
        }
    }

    private fun sendWhatsAppMessage(contactName: String, message: String): ToolExecutionResult {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            return ToolExecutionResult.PermissionNeeded(
                Manifest.permission.READ_CONTACTS,
                "WhatsApp message ke liye contact info chahiye."
            )
        }

        val contacts = queryContacts(contactName)
        if (contacts.isEmpty()) {
            // Direct phone number fallback or contact not found
            if (contactName.matches(Regex("^[+0-9]{8,15}$"))) {
                return openWhatsAppChat(contactName, message)
            }
            return ToolExecutionResult.Error(
                "Contact not found: $contactName",
                "'$contactName' phonebook mein nahi mila WhatsApp karne ke liye."
            )
        }

        if (contacts.size > 1) {
            val options = contacts.map {
                mapOf("id" to it.id, "name" to it.name, "phone" to it.phoneNumber, "message" to message)
            }
            return ToolExecutionResult.DisambiguationNeeded(
                question = "$contactName ke alag alag numbers hain. Kis number par WhatsApp karna hai?",
                options = options
            )
        }

        val target = contacts.first()
        return ToolExecutionResult.ConfirmationNeeded(
            prompt = "${target.name} ko WhatsApp message send kar doon: \"$message\"?",
            toolName = "sendWhatsAppDirect",
            target = target.name,
            parameters = mapOf("phoneNumber" to target.phoneNumber, "message" to message, "name" to target.name)
        )
    }

    fun openWhatsAppChat(phoneNumber: String, message: String): ToolExecutionResult {
        val cleanNumber = phoneNumber.replace(Regex("[^0-9+]"), "").let {
            if (!it.startsWith("+") && it.length == 10) "91$it" else it.removePrefix("+")
        }

        val encodedMessage = try {
            URLEncoder.encode(message, "UTF-8")
        } catch (e: Exception) {
            message
        }

        val uri = Uri.parse("https://api.whatsapp.com/send?phone=$cleanNumber&text=$encodedMessage")
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage("com.whatsapp")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        return try {
            context.startActivity(intent)
            ToolExecutionResult.Success(
                message = "Opened WhatsApp for $cleanNumber",
                conversationalResponse = "WhatsApp chat open ho gayi message ke saath!"
            )
        } catch (e: Exception) {
            // Fallback without package restriction
            try {
                val fallbackIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallbackIntent)
                ToolExecutionResult.Success(
                    message = "Opened WhatsApp web link",
                    conversationalResponse = "WhatsApp link open kar diya."
                )
            } catch (e2: Exception) {
                ToolExecutionResult.Error(
                    "WhatsApp not installed: ${e.message}",
                    "Phone mein WhatsApp installed nahi mil raha."
                )
            }
        }
    }

    private fun sendGmail(recipientEmail: String, subject: String, body: String): ToolExecutionResult {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:")
            putExtra(Intent.EXTRA_EMAIL, arrayOf(recipientEmail))
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        return try {
            context.startActivity(intent)
            ToolExecutionResult.Success(
                message = "Opened email client for $recipientEmail",
                conversationalResponse = "Email draft ho gaya hai $recipientEmail ke liye."
            )
        } catch (e: Exception) {
            ToolExecutionResult.Error("No email app found: ${e.message}", "Email app open nahi ho paya.")
        }
    }

    private fun getBatteryStatus(): ToolExecutionResult {
        val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1

        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL

        val batteryPct = if (level >= 0 && scale > 0) (level * 100 / scale) else 100

        val reply = if (isCharging) {
            "Battery $batteryPct% hai aur phone charging par laga hai ⚡"
        } else if (batteryPct <= 20) {
            "Battery sirf $batteryPct% bachi hai, charger dhoond lo boss!"
        } else {
            "Battery abhi $batteryPct% hai, full mast chal raha hai."
        }

        return ToolExecutionResult.Success(
            message = "Battery: $batteryPct%, Charging: $isCharging",
            conversationalResponse = reply,
            details = mapOf("percentage" to batteryPct, "isCharging" to isCharging)
        )
    }

    private fun getCurrentTime(): ToolExecutionResult {
        val sdf = SimpleDateFormat("h:mm a, EEEE, d MMMM", Locale.getDefault())
        val timeString = sdf.format(Date())
        return ToolExecutionResult.Success(
            message = "Current time is $timeString",
            conversationalResponse = "Abhi $timeString ho raha hai."
        )
    }

    private fun controlFlashlight(enable: Boolean): ToolExecutionResult {
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
        if (cameraManager == null) {
            return ToolExecutionResult.Error("CameraManager not available", "Torch control support nahi hai.")
        }

        return try {
            val cameraId = cameraManager.cameraIdList.firstOrNull { id ->
                val chars = cameraManager.getCameraCharacteristics(id)
                chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }

            if (cameraId != null) {
                cameraManager.setTorchMode(cameraId, enable)
                val reply = if (enable) "Flashlight ON kar di hai 🔦" else "Flashlight OFF ho gayi."
                ToolExecutionResult.Success(
                    message = "Flashlight set to $enable",
                    conversationalResponse = reply
                )
            } else {
                ToolExecutionResult.Error("No flash unit found", "Flashlight hardware nahi mila.")
            }
        } catch (e: CameraAccessException) {
            ToolExecutionResult.Error("Torch access error: ${e.message}", "Flashlight ON karne mein dikkat aayi.")
        }
    }

    private fun openSettings(settingType: String): ToolExecutionResult {
        val action = when (settingType.lowercase()) {
            "wifi" -> Settings.ACTION_WIFI_SETTINGS
            "bluetooth" -> Settings.ACTION_BLUETOOTH_SETTINGS
            "battery" -> Intent.ACTION_POWER_USAGE_SUMMARY
            "display" -> Settings.ACTION_DISPLAY_SETTINGS
            else -> Settings.ACTION_SETTINGS
        }

        val intent = Intent(action).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        return try {
            context.startActivity(intent)
            ToolExecutionResult.Success(
                message = "Opened settings: $settingType",
                conversationalResponse = "Settings khol diya hai."
            )
        } catch (e: Exception) {
            ToolExecutionResult.Error("Settings open failed: ${e.message}", "Settings open nahi ho paya.")
        }
    }

    private fun openUrl(url: String): ToolExecutionResult {
        val formattedUrl = if (url.startsWith("http://") || url.startsWith("https://")) {
            url
        } else {
            "https://www.google.com/search?q=" + URLEncoder.encode(url, "UTF-8")
        }

        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(formattedUrl)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        return try {
            context.startActivity(intent)
            ToolExecutionResult.Success(
                message = "Opened URL $formattedUrl",
                conversationalResponse = "Link browser mein open kar diya."
            )
        } catch (e: Exception) {
            ToolExecutionResult.Error("Browser failed: ${e.message}", "Link open nahi ho paya.")
        }
    }

    private fun setTimer(seconds: Int, label: String): ToolExecutionResult {
        val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
            putExtra(AlarmClock.EXTRA_LENGTH, seconds)
            putExtra(AlarmClock.EXTRA_MESSAGE, label)
            putExtra(AlarmClock.EXTRA_SKIP_UI, false)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        return try {
            context.startActivity(intent)
            val minutes = seconds / 60
            val remainingSecs = seconds % 60
            val timeText = if (minutes > 0) "$minutes minute" else "$remainingSecs second"
            ToolExecutionResult.Success(
                message = "Set timer for $seconds seconds",
                conversationalResponse = "Timer set ho gaya: $timeText ke liye ⏱️"
            )
        } catch (e: Exception) {
            ToolExecutionResult.Error("Timer failed: ${e.message}", "Timer set nahi ho paya.")
        }
    }

    private fun queryContacts(query: String): List<ContactMatch> {
        val results = mutableListOf<ContactMatch>()
        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )
        val selection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"
        val selectionArgs = arrayOf("%$query%")

        var cursor: Cursor? = null
        try {
            cursor = context.contentResolver.query(uri, projection, selection, selectionArgs, null)
            cursor?.let {
                val idIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

                while (it.moveToNext()) {
                    val id = if (idIndex >= 0) it.getString(idIndex) else ""
                    val name = if (nameIndex >= 0) it.getString(nameIndex) else ""
                    val number = if (numberIndex >= 0) it.getString(numberIndex) else ""
                    if (name.isNotBlank() && number.isNotBlank()) {
                        results.add(ContactMatch(id, name, number))
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to query contacts: ${e.message}")
        } finally {
            cursor?.close()
        }
        return results.distinctBy { it.name.lowercase() + it.phoneNumber }
    }
}

data class ContactMatch(
    val id: String,
    val name: String,
    val phoneNumber: String
)
