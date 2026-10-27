package com.example.dmuonecard

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

// ───────────────────────── Design tokens ─────────────────────────
private val Navy = Color(0xFF0B1F3A)
private val NavyDeep = Color(0xFF071529)
private val Blue = Color(0xFF1D4ED8)
private val BlueSoft = Color(0xFF3B82F6)
private val PageBg = Color(0xFFF1F5FB)
private val Green = Color(0xFF0F8A5F)
private val GreenLight = Color(0xFF34D399)
private val Red = Color(0xFFC62828)
private val Orange = Color(0xFFC2700A)
private val Gold = Color(0xFFE0B64A)
private val InkMuted = Color(0xFF64748B)
private val CardBorder = Color(0xFFE2E8F0)
private val Divider = Color(0xFFEDF1F7)

private val HeaderBrush = Brush.verticalGradient(
    colors = listOf(NavyDeep, Navy)
)
private val ScannerBrush = Brush.verticalGradient(
    colors = listOf(Color(0xFF10294B), Navy)
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = PageBg
                ) {
                    OneCardScreen()
                }
            }
        }
    }
}

@Composable
fun NetworkImage(
    url: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit
) {
    var bitmap by remember(url) {
        mutableStateOf<ImageBitmap?>(null)
    }

    LaunchedEffect(url) {
        bitmap = withContext(Dispatchers.IO) {
            try {
                val connection =
                    URL(url).openConnection() as HttpURLConnection

                connection.connectTimeout = 5000
                connection.readTimeout = 5000

                try {
                    connection.inputStream.use { stream ->
                        BitmapFactory.decodeStream(stream)?.asImageBitmap()
                    }
                } finally {
                    connection.disconnect()
                }
            } catch (_: Exception) {
                null
            }
        }
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap!!,
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale
        )
    } else {
        Box(
            modifier = modifier.background(
                color = Color(0xFFE2E8F0),
                shape = CircleShape
            ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No Photo",
                color = InkMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/** Flashlight icon drawn with Canvas (no extra icon dependency needed). */
@Composable
private fun FlashIcon(
    on: Boolean,
    modifier: Modifier = Modifier
) {
    val tint = if (on) Navy else Color.White
    Canvas(modifier = modifier) {
        val s = size.minDimension / 24f
        val bolt = Path().apply {
            moveTo(13f * s, 2f * s)
            lineTo(5f * s, 13f * s)
            lineTo(11f * s, 13f * s)
            lineTo(10f * s, 22f * s)
            lineTo(19f * s, 10f * s)
            lineTo(13f * s, 10f * s)
            close()
        }
        drawPath(path = bolt, color = tint)
    }
}

/** Big check mark (approved) or cross (rejected) drawn with Canvas. */
@Composable
private fun StatusIcon(
    approved: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(
            width = w * 0.09f,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
        if (approved) {
            val check = Path().apply {
                moveTo(w * 0.27f, h * 0.53f)
                lineTo(w * 0.43f, h * 0.69f)
                lineTo(w * 0.74f, h * 0.35f)
            }
            drawPath(path = check, color = Color.White, style = stroke)
        } else {
            drawLine(
                color = Color.White,
                start = Offset(w * 0.32f, h * 0.32f),
                end = Offset(w * 0.68f, h * 0.68f),
                strokeWidth = stroke.width,
                cap = StrokeCap.Round
            )
            drawLine(
                color = Color.White,
                start = Offset(w * 0.68f, h * 0.32f),
                end = Offset(w * 0.32f, h * 0.68f),
                strokeWidth = stroke.width,
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
fun OneCardScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var cameraGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { granted ->
            cameraGranted = granted
        }

    var studentId by remember { mutableStateOf("") }
    var response by remember { mutableStateOf<ScanResponse?>(null) }
    var loading by remember { mutableStateOf(false) }

    var dashboardStatus by remember {
        mutableStateOf<MealStatusResponse?>(null)
    }
    var dashboardError by remember {
        mutableStateOf<String?>(null)
    }

    // Flashlight state (phone torch)
    var torchOn by remember { mutableStateOf(false) }

    fun refreshMealStatus() {
        scope.launch {
            try {
                dashboardStatus = ApiClient.api.getMealStatus()
                dashboardError = null
            } catch (e: Exception) {
                dashboardError = "Cannot load meal status. Check Django connection."
            }
        }
    }

    LaunchedEffect(Unit) {
        refreshMealStatus()
    }

    var scanArmed by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun checkStudent(rawId: String) {
        val id = rawId.trim()

        if (id.isBlank() || loading) return

        // Never submit a scan when the meal is known to be closed.
        when (dashboardStatus?.status) {
            "CLOSED" -> {
                studentId = id
                response = ScanResponse(
                    status = "MEAL_SERVICE_NOT_OPEN",
                    message = "Meal service is closed. Please wait for the next meal."
                )
                errorMessage = null
                scanArmed = false
                return
            }

            "OPEN" -> {
                // Continue to the normal server verification below.
            }

            else -> {
                dashboardError =
                    "Meal status is not available. Refresh the status before scanning."
                return
            }
        }

        studentId = id
        loading = true
        scanArmed = false
        response = null
        errorMessage = null

        scope.launch {
            try {
                val result = ApiClient.api.scanStudent(
                    ScanRequest(student_id = id)
                )

                val bodyText = if (result.isSuccessful) {
                    result.body()?.let { Gson().toJson(it) }
                } else {
                    result.errorBody()?.string()
                }

                response = try {
                    bodyText?.let {
                        Gson().fromJson(it, ScanResponse::class.java)
                    } ?: ScanResponse(
                        status = "REQUEST_FAILED",
                        message = "The server returned an empty response (HTTP ${result.code()})."
                    )
                } catch (_: Exception) {
                    ScanResponse(
                        status = "REQUEST_FAILED",
                        message = "The server returned HTTP ${result.code()}, but its response could not be read."
                    )
                }

                if (response?.status.isNullOrBlank()) {
                    response = ScanResponse(
                        status = "REQUEST_FAILED",
                        message = "The server response did not contain a status."
                    )
                }
            } catch (e: Exception) {
                errorMessage = when (e) {
                    is java.net.SocketTimeoutException ->
                        "The server took too long to respond. Check Django and your network."

                    else ->
                        "Cannot connect to Django. Check that the server is running and the API URL is correct. Details: ${e.localizedMessage ?: "Network error"}"
                }
            } finally {
                try {
                    dashboardStatus = ApiClient.api.getMealStatus()
                    dashboardError = null
                } catch (e: Exception) {
                    dashboardError = "Unable to refresh meal counts."
                }
                loading = false
            }
        }
    }

    fun prepareNextStudent() {
        studentId = ""
        response = null
        errorMessage = null
        scanArmed = true
    }

    val currentStatus = response?.status
    val statusColor = when (currentStatus) {
        "MEAL_APPROVED" -> Green
        "ALREADY_TAKEN" -> Red
        "MEAL_SERVICE_NOT_OPEN" -> Color(0xFFF9A825)
        "STUDENT_INACTIVE",
        "STUDENT_NOT_FOUND" -> Orange
        else -> Orange
    }

    val showResult = response != null || errorMessage != null

    // System back closes the result screen and re-arms the scanner
    BackHandler(enabled = showResult) {
        prepareNextStudent()
    }

    Box(modifier = Modifier.fillMaxSize()) {

        // ═══════════ Main screen: header + scanner, no scrolling ═══════════
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val dashboard = dashboardStatus
            val isOpen = dashboard?.status == "OPEN"

            // ───────────── Compact header with live totals ─────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = HeaderBrush,
                        shape = RoundedCornerShape(
                            bottomStart = 28.dp,
                            bottomEnd = 28.dp
                        )
                    )
                    .statusBarsPadding()
                    .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(58.dp)
                            .border(1.5.dp, Gold, CircleShape)
                            .padding(3.dp)
                            .clip(CircleShape)
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.dmu_logo),
                            contentDescription = "Debre Markos University Logo",
                            modifier = Modifier.size(46.dp),
                            contentScale = ContentScale.Fit
                        )
                    }

                    Spacer(Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "DMU ONE CARD",
                            fontSize = 21.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp,
                            color = Color.White
                        )
                        Text(
                            text = "UNIVERSITY CAFETERIA",
                            fontSize = 10.sp,
                            letterSpacing = 2.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Gold
                        )
                        Text(
                            text = "Meal Verification System",
                            fontSize = 12.sp,
                            color = Color(0xFFB8C7DE)
                        )
                    }

                    // Service status pill
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (isOpen) {
                            Color(0x3334D399)
                        } else {
                            Color(0x33F9A825)
                        },
                        border = BorderStroke(
                            1.dp,
                            if (isOpen) {
                                GreenLight.copy(alpha = 0.6f)
                            } else {
                                Color(0xFFF9A825).copy(alpha = 0.8f)
                            }
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .background(
                                        if (isOpen) GreenLight else Color(0xFFF87171),
                                        CircleShape
                                    )
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = if (isOpen) "OPEN" else "CLOSED",
                                color = Color.White,
                                fontSize = 11.sp,
                                letterSpacing = 1.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Totals strip (tap to refresh)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0x1AFFFFFF), RoundedCornerShape(16.dp))
                        .border(1.dp, Color(0x26FFFFFF), RoundedCornerShape(16.dp))
                        .clickable { refreshMealStatus() }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "SERVED",
                            fontSize = 10.sp,
                            letterSpacing = 1.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFB8C7DE)
                        )
                        Text(
                            text = "${dashboard?.served_count ?: 0}",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = GreenLight
                        )
                    }

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(32.dp)
                            .background(Color(0x33FFFFFF))
                    )

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 14.dp)
                    ) {
                        Text(
                            text = "TODAY'S TOTAL",
                            fontSize = 10.sp,
                            letterSpacing = 1.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFB8C7DE)
                        )
                        Text(
                            text = "${dashboard?.todays_total ?: 0}",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = if (isOpen) {
                                dashboard?.meal_display ?: "Meal service"
                            } else {
                                dashboard?.next_meal?.let { "Next: $it" } ?: "Service closed"
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Gold
                        )
                        Text(
                            text = if (isOpen) {
                                "${dashboard?.start_time ?: "--:--"} – ${dashboard?.end_time ?: "--:--"}"
                            } else {
                                dashboard?.next_opening ?: "Tap to refresh"
                            },
                            fontSize = 11.sp,
                            color = Color(0xFFB8C7DE)
                        )
                    }
                }

                dashboardError?.let {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = it,
                        color = Color(0xFFFCA5A5),
                        fontSize = 11.sp
                    )
                }
            }

            // ───────────── Scanner fills the rest of the screen ─────────────
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Navy
                ),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(ScannerBrush)
                        .padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "STUDENT CARD SCANNER",
                        fontSize = 13.sp,
                        letterSpacing = 2.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Spacer(Modifier.height(10.dp))

                    if (!cameraGranted) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Camera permission is needed to scan student cards.",
                                color = Color(0xFFD8E5F8),
                                textAlign = TextAlign.Center
                            )

                            Spacer(Modifier.height(10.dp))

                            Button(
                                onClick = {
                                    permissionLauncher.launch(Manifest.permission.CAMERA)
                                },
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = BlueSoft
                                )
                            ) {
                                Text("ENABLE CAMERA", fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .border(
                                    width = 2.dp,
                                    color = if (scanArmed && !loading) Gold
                                    else Color(0xFF42536D),
                                    shape = RoundedCornerShape(18.dp)
                                )
                                .padding(3.dp)
                                .clip(RoundedCornerShape(15.dp))
                                .background(Color.Black)
                        ) {
                            CameraScanner(
                                enabled = scanArmed && !loading,
                                onBarcodeDetected = { value ->
                                    if (scanArmed && !loading && value.isNotBlank()) {
                                        checkStudent(value.trim())
                                    }
                                },
                                torchOn = torchOn
                            )
//                            CameraScanner(
//                                enabled = scanArmed &&
//                                        !loading &&
//                                        dashboardStatus?.status == "OPEN",
//
//                                onBarcodeDetected = { value ->
//                                    if (
//                                        scanArmed &&
//                                        !loading &&
//                                        dashboardStatus?.status == "OPEN" &&
//                                        value.isNotBlank()
//                                    ) {
//                                        checkStudent(value.trim())
//                                    }
//                                },
//
//                                torchOn = torchOn
//                            )

                            // Flashlight toggle
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(10.dp)
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (torchOn) Gold else Color(0x99000000)
                                    )
                                    .border(
                                        1.dp,
                                        if (torchOn) Gold else Color(0x66FFFFFF),
                                        CircleShape
                                    )
                                    .clickable { torchOn = !torchOn },
                                contentAlignment = Alignment.Center
                            ) {
                                FlashIcon(
                                    on = torchOn,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            // Scan status label
                            Surface(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 10.dp),
                                shape = RoundedCornerShape(50),
                                color = Color(0x99000000)
                            ) {
                                Text(
                                    text = when {
                                        loading -> "Checking student..."

                                        dashboardStatus?.status == "CLOSED" ->
                                            "Meal service closed · Scanner disabled"

                                        dashboardStatus?.status != "OPEN" ->
                                            "Checking meal service..."

                                        scanArmed ->
                                            if (torchOn) "Light on · Scanning"
                                            else "Point at a QR code or barcode"

                                        else -> "Scanner paused"
                                    },
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(Modifier.height(10.dp))

                        Button(
                            onClick = {
                                if (!loading) {
                                    prepareNextStudent()
                                }
                            },
                            enabled = !loading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = BlueSoft
                            )
                        ) {
                            Text(
                                text = if (scanArmed && response == null && errorMessage == null) {
                                    "SCANNER READY"
                                } else {
                                    "START CAMERA SCAN"
                                },
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                fontSize = 13.sp
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = studentId,
                            onValueChange = { studentId = it },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            enabled = !loading,
                            label = { Text("Student ID") },
                            placeholder = { Text("e.g. DMU1001") },
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                disabledTextColor = Color(0xFF8391A7),
                                focusedBorderColor = Gold,
                                unfocusedBorderColor = Color(0xFF50648A),
                                focusedLabelColor = Gold,
                                unfocusedLabelColor = Color(0xFFB8C7DE),
                                focusedPlaceholderColor = Color(0xFF7F90AD),
                                unfocusedPlaceholderColor = Color(0xFF7F90AD),
                                focusedContainerColor = Color(0x14FFFFFF),
                                unfocusedContainerColor = Color(0x0AFFFFFF),
                                cursorColor = Gold
                            )
                        )

                        Button(
                            onClick = {
                                checkStudent(studentId)
                            },
                            modifier = Modifier.height(56.dp),
                            enabled = !loading && studentId.isNotBlank(),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = BlueSoft,
                                disabledContainerColor = Color(0xFF2A4166),
                                disabledContentColor = Color(0xFF8FA3C4)
                            )
                        ) {
                            if (loading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("CHECKING...", letterSpacing = 0.5.sp, fontSize = 12.sp)
                            } else {
                                Text(
                                    "CHECK",
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // ═══════════ Full-screen student result (own screen) ═══════════
        AnimatedVisibility(
            visible = showResult,
            enter = fadeIn(tween(220)) + scaleIn(
                initialScale = 0.92f,
                animationSpec = tween(260)
            ),
            exit = fadeOut(tween(160))
        ) {
            ResultScreen(
                result = response,
                errorMessage = errorMessage,
                statusColor = statusColor,
                fallbackStudentId = studentId,
                loading = loading,
                nextOpening = dashboardStatus?.next_opening,
                nextMeal = dashboardStatus?.next_meal,
                onNext = { prepareNextStudent() }
            )
        }
    }
}

/**
 * Full-screen result:
 *  - MEAL_APPROVED (unused meal)  → green "WELCOME" screen with student name
 *  - ALREADY_TAKEN (used meal)    → red "ALREADY USED" screen
 *  - everything else              → red / orange status screen
 */
@Composable
private fun ResultScreen(
    result: ScanResponse?,
    errorMessage: String?,
    statusColor: Color,
    fallbackStudentId: String,
    loading: Boolean,
    nextOpening: String?,
    nextMeal: String?,
    onNext: () -> Unit
){
    val approved = result?.status == "MEAL_APPROVED"
    val used = result?.status == "ALREADY_TAKEN"
    val student = result?.student

    val title = when {
        errorMessage != null -> "CONNECTION ERROR"
        approved -> "WELCOME"
        used -> "ALREADY USED"
        result?.status == "MEAL_SERVICE_NOT_OPEN" -> "SERVICE CLOSED"
        result?.status == "STUDENT_NOT_FOUND" -> "STUDENT NOT FOUND"
        result?.status == "STUDENT_INACTIVE" -> "CARD INACTIVE"
        else -> result?.status ?: "REQUEST FAILED"
    }

    val message = errorMessage ?: (result?.message ?: "No message returned.")

    // Entrance animation
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }

    val iconScale by animateFloatAsState(
        targetValue = if (shown) 1f else 0.3f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "iconScale"
    )
    val contentAlpha by animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        animationSpec = tween(450, delayMillis = 120),
        label = "contentAlpha"
    )

    val resultColor = if (errorMessage != null) Red else statusColor

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        lerp(resultColor, Color.Black, 0.45f),
                        resultColor
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Animated status icon
            Box(
                modifier = Modifier
                    .scale(iconScale)
                    .size(96.dp)
                    .background(Color(0x33FFFFFF), CircleShape)
                    .border(2.dp, Color(0x80FFFFFF), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                StatusIcon(
                    approved = approved,
                    modifier = Modifier.size(62.dp)
                )
            }

            Spacer(Modifier.height(16.dp))

            Column(
                modifier = Modifier.alpha(contentAlpha),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "VERIFICATION RESULT",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 11.sp,
                    letterSpacing = 3.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(6.dp))

                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp,
                    textAlign = TextAlign.Center
                )

                if (approved) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = student?.full_name ?: "Student",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(Modifier.height(8.dp))

                Text(
                    text = message,
                    color = Color.White.copy(alpha = 0.92f),
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                    textAlign = TextAlign.Center
                )
                if (result?.status == "MEAL_SERVICE_NOT_OPEN") {
                    Spacer(Modifier.height(12.dp))

                    Text(
                        text = "Please wait for the next meal.",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    nextMeal?.let {
                        Text(
                            text = "Next meal: $it",
                            color = Color.White,
                            fontSize = 15.sp,
                            textAlign = TextAlign.Center
                        )
                    }

                    nextOpening?.let {
                        Text(
                            text = "Opening time: $it",
                            color = Color.White,
                            fontSize = 15.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                if (student != null) {
                    Spacer(Modifier.height(20.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(26.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White
                        ),
                        elevation = CardDefaults.cardElevation(10.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "STUDENT PROFILE",
                                fontSize = 11.sp,
                                letterSpacing = 2.sp,
                                fontWeight = FontWeight.Bold,
                                color = Blue
                            )

                            Spacer(Modifier.height(14.dp))

                            if (!student.photo_url.isNullOrBlank()) {
                                NetworkImage(
                                    url = student.photo_url,
                                    contentDescription = "Student photo",
                                    modifier = Modifier
                                        .size(132.dp)
                                        .border(4.dp, resultColor, CircleShape)
                                        .padding(6.dp)
                                        .clip(CircleShape)
                                        .background(PageBg),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(132.dp)
                                        .border(4.dp, resultColor, CircleShape)
                                        .padding(6.dp)
                                        .clip(CircleShape)
                                        .background(PageBg),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("No Photo", color = InkMuted)
                                }
                            }

                            Spacer(Modifier.height(14.dp))

                            Text(
                                text = student.full_name ?: "Unknown student",
                                fontSize = 21.sp,
                                fontWeight = FontWeight.Bold,
                                color = Navy,
                                textAlign = TextAlign.Center
                            )

                            Spacer(Modifier.height(12.dp))

                            ProfileRow(
                                label = "Student ID",
                                value = student.student_id ?: fallbackStudentId
                            )

                            ProfileRow(
                                label = "Department",
                                value = student.department ?: "Not available"
                            )

                            result?.meal?.let {
                                ProfileRow(
                                    label = "Meal",
                                    value = it.replaceFirstChar { c ->
                                        c.uppercase()
                                    }
                                )
                            }

                            result?.service_date?.let {
                                ProfileRow(
                                    label = "Service date",
                                    value = it
                                )
                            }

                            result?.served_at?.let {
                                ProfileRow(
                                    label = if (used) "Used at" else "Served at",
                                    value = it
                                )
                            }

                            Spacer(Modifier.height(8.dp))

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = when {
                                    approved -> Color(0xFFE8F6EE)
                                    result?.status == "MEAL_SERVICE_NOT_OPEN" ->
                                        Color(0xFFFFF8E1)
                                    used -> Color(0xFFFDEDEC)
                                    else -> Color(0xFFFFF3E0)
                                }
                            ) {
                                Text(
                                    text = if (approved) {
                                        "This meal has been recorded."
                                    } else {
                                        "Please follow the cafeteria staff's instructions."
                                    },
                                    color = if (approved) Green else Red,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                Button(
                    onClick = onNext,
                    enabled = !loading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = lerp(resultColor, Color.Black, 0.25f)
                    )
                ) {
                    Text(
                        "SCAN NEXT STUDENT",
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.5.sp
                    )
                }

                Spacer(Modifier.height(10.dp))
            }
        }
    }
}

@Composable
private fun ProfileRow(
    label: String,
    value: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Text(
            text = label.uppercase(),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp,
            color = Color(0xFF8795AB)
        )

        Spacer(Modifier.height(3.dp))

        Text(
            text = value,
            fontSize = 16.sp,
            color = Navy,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(Modifier.height(6.dp))

        HorizontalDivider(color = Divider)
    }
}