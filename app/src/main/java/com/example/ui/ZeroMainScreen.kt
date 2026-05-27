package com.example.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.BuildConfig
import com.example.data.AppDatabase
import com.example.data.Meal
import com.example.data.Recipe
import com.example.data.WaterIntake
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.sin

@Composable
fun ZeroMainScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Collect states
    val todayMeals by viewModel.todayMeals.collectAsStateWithLifecycle()
    val todayWater by viewModel.todayWater.collectAsStateWithLifecycle()
    val allRecipes by viewModel.allRecipes.collectAsStateWithLifecycle()
    val preferences by viewModel.userPreferences.collectAsStateWithLifecycle()
    val aiLoading by viewModel.aiLoading.collectAsStateWithLifecycle()
    val lastReport by viewModel.lastAddedReport.collectAsStateWithLifecycle()

    // Custom dialog/sheet states
    var showSmartPot by remember { mutableStateOf(false) }
    var showVoiceInput by remember { mutableStateOf(false) }
    var showCameraInput by remember { mutableStateOf(false) }
    var showRecipeList by remember { mutableStateOf(false) }
    var showManualInput by remember { mutableStateOf(false) }

    // Natural text logging field
    var textQuery by remember { mutableStateOf("") }

    // Background cosmic gradient
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF141924), // Center glowing slate
                        Color(0xFF07080B)  // Dark deep edge
                    ),
                    center = Offset(500f, 400f),
                    radius = 1200f
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Core Layout Columns
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Screen Header: Brand and Profile Accent
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "ZERO",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 4.sp,
                        color = Color.White
                    )
                    Text(
                        text = "Привет, ${preferences.name}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.Gray
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Smart Recipes Drawer Toggle
                    IconButton(
                        onClick = { showRecipeList = true },
                        modifier = Modifier
                            .testTag("recipes_drawer_button")
                            .background(Color(0x1AFFFFFF), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.SoupKitchen,
                            contentDescription = "Рецепты Smart Pot",
                            tint = Color.White
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))

                    // Reset Prefs for debugging / onboarding demo
                    IconButton(
                        onClick = {
                            viewModel.resetPreferencesForTesting()
                            Toast.makeText(context, "Инициализация Onboarding", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .background(Color(0x19F43F5E), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = "Сброс",
                            tint = Color(0xFFF43F5E)
                        )
                    }
                }
            }

            // Calculation values
            val totalCalTarget = preferences.dailyCalorieTarget.toDouble()
            val totalCalConsumed = todayMeals.sumOf { it.calories }
            val totalProteinConsumed = todayMeals.sumOf { it.protein }
            val totalFatConsumed = todayMeals.sumOf { it.fat }
            val totalCarbsConsumed = todayMeals.sumOf { it.carbs }

            // 1. THE FLOW SPHERE (Visual Metaphor instead of dry boring circle charts)
            Box(
                modifier = Modifier
                    .padding(vertical = 16.dp)
                    .size(240.dp),
                contentAlignment = Alignment.Center
            ) {
                TheFlowSphere(
                    targetCalories = totalCalTarget,
                    consumedCalories = totalCalConsumed,
                    proteinRatio = totalProteinConsumed / (totalCalConsumed.coerceAtLeast(1.0) * 0.1), 
                    fatRatio = totalFatConsumed / (totalCalConsumed.coerceAtLeast(1.0) * 0.1),
                    carbsRatio = totalCarbsConsumed / (totalCalConsumed.coerceAtLeast(1.0) * 0.1),
                    isAiThinking = aiLoading
                )

                // Sphere Metrics HUD
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.offset(y = (-4).dp)
                ) {
                    val remainingCal = (totalCalTarget - totalCalConsumed).toInt()
                    val textSign = if (remainingCal >= 0) "ккал осталось" else "ккал превышено"
                    
                    Text(
                        text = "${kotlin.math.abs(remainingCal)}",
                        fontSize = 38.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        letterSpacing = (-1).sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = textSign,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (remainingCal >= 0) Color.LightGray else Color(0xFF818CF8) // Twilight mode
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    // Simple progress fraction
                    val progressFraction = (totalCalConsumed / totalCalTarget).coerceIn(0.0, 1.0)
                    Text(
                        text = "бюджет: ${totalCalConsumed.toInt()} / ${totalCalTarget.toInt()}",
                        fontSize = 10.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Normal
                    )
                }
            }

            // 2. MACRONUTRIENTS (БЖУ Pill Badges in spacious asymmetric alignment)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MacroPill(
                    label = "Белки",
                    amount = "${totalProteinConsumed.toInt()}г",
                    color = Color(0xFF818CF8), // Amethyst
                    target = "120г"
                )
                MacroPill(
                    label = "Жиры",
                    amount = "${totalFatConsumed.toInt()}г",
                    color = Color(0xFFD97706), // Amber-Gold
                    target = "70г"
                )
                MacroPill(
                    label = "Углеводы",
                    amount = "${totalCarbsConsumed.toInt()}г",
                    color = Color(0xFFFB923C), // Peach
                    target = "250г"
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // AI Instant Floating Entry Field
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0x14FFFFFF))
                    .border(1.dp, Color(0x1FFFFFFF), RoundedCornerShape(24.dp))
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Voice activation mic
                    IconButton(
                        onClick = { showVoiceInput = true },
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(0x14818CF8), CircleShape)
                            .testTag("microphone_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Mic,
                            contentDescription = "Голосовой ввод",
                            tint = Color(0xFF818CF8)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Text Field Integration
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { showManualInput = true }
                            .padding(vertical = 12.dp)
                    ) {
                        Text(
                            text = if (textQuery.isEmpty()) "«Я съел тарелку супа...»" else textQuery,
                            color = if (textQuery.isEmpty()) Color.Gray else Color.White,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Kitchen Pot Recipe Calibration
                    IconButton(
                        onClick = { showSmartPot = true },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("smart_pot_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.SoupKitchen,
                            contentDescription = "Умная кастрюля",
                            tint = Color.LightGray
                        )
                    }

                    // Smart Camera photo scan
                    IconButton(
                        onClick = { showCameraInput = true },
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(0x1406B6D4), CircleShape)
                            .testTag("camera_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PhotoCamera,
                            contentDescription = "Сканер фото",
                            tint = Color(0xFF06B6D4)
                        )
                    }
                }
            }

            // Notification of last parsed food (AI preview feedback)
            AnimatedVisibility(
                visible = lastReport != null,
                enter = slideInVertically() + fadeIn(),
                exit = slideOutVertically() + fadeOut()
            ) {
                lastReport?.let { report ->
                    Card(
                        modifier = Modifier
                            .padding(horizontal = 24.dp, vertical = 12.dp)
                            .fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B4B)), // Soothing indigo
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(1.dp, Color(0x33818CF8))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.AutoAwesome,
                                        contentDescription = "AI",
                                        tint = Color(0xFFFBBF24),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Распознано ИИ:",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFA5B4FC)
                                    )
                                }
                                IconButton(
                                    onClick = { viewModel.clearLastReport() },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = "Закрыть",
                                        tint = Color.Gray,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = report.name,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "${report.weightGrams.toInt()}г | ${report.calories.toInt()} ккал | Б:${report.protein.toInt()} Ж:${report.fat.toInt()} У:${report.carbs.toInt()}",
                                fontSize = 13.sp,
                                color = Color.LightGray
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Divider(color = Color(0x1AFFFFFF))
                            Spacer(modifier = Modifier.height(6.dp))
                            report.breakdown.forEach { item ->
                                Text(
                                    text = "• $item",
                                    fontSize = 12.sp,
                                    color = Color.Gray,
                                    modifier = Modifier.padding(vertical = 1.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Food Log Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Дневник Питания",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "${todayMeals.size} записей",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }

            // 3. FOOD LOG LIST (Minimalist stylish feedback cards)
            if (todayMeals.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Restaurant,
                        contentDescription = "Пусто",
                        tint = Color(0x1AFFFFFF),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Внесите свою первую еду",
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = "Без лишней рутины и поисков по базам",
                        fontSize = 11.sp,
                        color = Color(0x80FFFFFF)
                    )
                }
            } else {
                todayMeals.forEach { meal ->
                    FoodLogCard(
                        meal = meal,
                        onDelete = { viewModel.deleteMeal(meal.id) }
                    )
                }
            }

            // Margin space for bottom water wave
            Spacer(modifier = Modifier.height(130.dp))
        }

        // 4. WATER WAVE (A cool reactive fluid bar hugging the bottom of the screen)
        val waterDrunk = todayWater.sumOf { it.amountMl }
        val waterTarget = 2000
        val waterProgress = (waterDrunk.toFloat() / waterTarget.toFloat()).coerceIn(0f, 1f)

        WaterWaveBottom(
            progress = waterProgress,
            drunkMl = waterDrunk,
            targetMl = waterTarget,
            onAddWater = {
                viewModel.addWater(250)
                Toast.makeText(context, "+250 мл воды добавлено", Toast.LENGTH_SHORT).show()
            },
            onResetWater = {
                if (todayWater.isNotEmpty()) {
                    // Delete latest
                    viewModel.deleteWaterIntake(todayWater.first().id)
                    Toast.makeText(context, "Последний стакан убран", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        // ONBOARDING OVERLAY (Shows instantly on startup, can be connected/skipped with 1 click)
        if (!preferences.onboardingCompleted) {
            OnboardingOverlay(
                onCompleted = { calTarget, nickname, hasHealth ->
                    viewModel.completeOnboarding(calTarget, nickname, hasHealth)
                }
            )
        }

        // --- SUB-SCREEN DIALOGS (Keep app Strictly Single-view constraint!) ---

        // Manual Input Dialog
        if (showManualInput) {
            ManualInputDialog(
                onDismiss = { showManualInput = false },
                onAdd = { typedText ->
                    textQuery = typedText
                    viewModel.logFoodByText(typedText)
                    showManualInput = false
                }
            )
        }

        // Voice Dictation Simulated Dialog
        if (showVoiceInput) {
            VoiceDictationDialog(
                onDismiss = { showVoiceInput = false },
                onChoosePreset = { speech ->
                    textQuery = speech
                    viewModel.logFoodByText(speech)
                    showVoiceInput = false
                }
            )
        }

        // Camera Multimodal Scanner Dialog
        if (showCameraInput) {
            CameraScannerDialog(
                viewModel = viewModel,
                onDismiss = { showCameraInput = false },
                onIdentify = { bitmap, contextQuery ->
                    viewModel.logFoodByImage(bitmap, contextQuery)
                    showCameraInput = false
                }
            )
        }

        // Smart Pot Math Calculator Dialog
        if (showSmartPot) {
            SmartPotDialog(
                viewModel = viewModel,
                onDismiss = { showSmartPot = false }
            )
        }

        // Smart Pot Saved Recipes list
        if (showRecipeList) {
            RecipeListDialog(
                recipes = allRecipes,
                onDismiss = { showRecipeList = false },
                onSelect = { recipe, grams ->
                    // Logs proportion of that recipe
                    val proportion = grams / 100.0
                    val meal = Meal(
                        name = "${recipe.title} (Домашнее)",
                        calories = recipe.caloriesPer100g * proportion,
                        protein = recipe.totalProtein / (recipe.netCookedWeightGrams / 100.0) * proportion,
                        fat = recipe.totalFat / (recipe.netCookedWeightGrams / 100.0) * proportion,
                        carbs = recipe.totalCarbs / (recipe.netCookedWeightGrams / 100.0) * proportion,
                        weightGrams = grams,
                        timestamp = System.currentTimeMillis()
                    )
                    viewModel.logFoodByText("${meal.name}: Усвоено КБЖУ для ${grams.toInt()}г")
                    viewModel.addWater(0) // force updates logs
                    scope.launch {
                        viewModel.deleteMeal(0) // clean mock update
                        // Insert actually computed proportion
                        val appDb = AppDatabase.getDatabase(context)
                        appDb.appDao().insertMeal(meal)
                    }
                    showRecipeList = false
                },
                onDelete = { id -> viewModel.deleteRecipe(id) }
            )
        }
    }
}

// ------------------- INNER COMPOSABLES & VISUAL ACCENTS -------------------

@Composable
fun MacroPill(label: String, amount: String, color: Color, target: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0x0AFFFFFF))
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .widthIn(min = 80.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = Color.Gray,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = amount,
            fontSize = 15.sp,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(1.dp))
        Text(
            text = "цель: $target",
            fontSize = 9.sp,
            color = color.copy(alpha = 0.8f),
            fontWeight = FontWeight.Normal
        )
    }
}

@Composable
fun FoodLogCard(meal: Meal, onDelete: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0x0FFFFFFF)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0x0DFFFFFF))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = meal.name,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${meal.weightGrams.toInt()}г",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Б:${meal.protein.toInt()} Ж:${meal.fat.toInt()} У:${meal.carbs.toInt()}",
                        color = Color(0x80FFFFFF),
                        fontSize = 11.sp
                    )
                }
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${meal.calories.toInt()} ккал",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(6.dp))
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Удалить",
                        tint = Color(0xFFE11D48),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

/**
 * Stunning liquid Canvas-drawn Flow Sphere that changes colors based on macros & limit breaches.
 * Exceeding calorie targets turns it into a calming deep twilight space violet instead of stressful red.
 */
@Composable
fun TheFlowSphere(
    targetCalories: Double,
    consumedCalories: Double,
    proteinRatio: Double,
    fatRatio: Double,
    carbsRatio: Double,
    isAiThinking: Boolean
) {
    val totalProgress = (consumedCalories / targetCalories).coerceIn(0.0, 1.5)
    
    // Choose theme colors dynamically: Calm twilight indigo if calories exceed limit
    val limitBreached = consumedCalories > targetCalories
    
    val baseColorInside = animateColorAsState(
        targetValue = if (limitBreached) Color(0xFF1E1B4B) else Color(0x33FFFFFF), // soothing twilight vs dynamic white
        animationSpec = tween(500)
    )

    val glowColor = animateColorAsState(
        targetValue = if (limitBreached) Color(0xFF818CF8) else Color(0xFFFBBF24), // Twilight purple vs Golden support
        animationSpec = tween(500)
    )

    // Animated sloshing waves
    val transition = rememberInfiniteTransition(label = "sloshing")
    val waveOffset by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sloshOffset"
    )

    // Pulsing halo for AI processing state
    val aiRadiusScale by transition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (isAiThinking) 1.2f else 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isAiThinking) 1000 else 3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "aiPulse"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val radius = size.minDimension / 2.0f
        val center = Offset(size.width / 2f, size.height / 2f)

        // Draw breathing AI halo glow behind the sphere
        val glowRadius = radius * aiRadiusScale
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    if (isAiThinking) Color(0x66818CF8) else glowColor.value.copy(alpha = 0.15f),
                    Color.Transparent
                ),
                center = center,
                radius = glowRadius + 20f
            ),
            radius = glowRadius,
            center = center
        )

        // Glass sphere outline mapping
        drawCircle(
            color = glowColor.value.copy(alpha = 0.25f),
            radius = radius,
            center = center,
            style = Stroke(width = 2.dp.toPx())
        )

        // Inner Fluid drawing - Clipped within circular container boundary
        val clipPath = Path().apply {
            addOval(androidx.compose.ui.geometry.Rect(center, radius))
        }

        drawContext.canvas.save()
        drawContext.canvas.clipPath(clipPath)

        // Draw solid background color matching calorie target state
        drawCircle(
            color = baseColorInside.value,
            radius = radius,
            center = center
        )

        // Determine fluid level height relative to calorie progress
        // 0% -> bottom of the circle, 100% -> filled
        val liquidFillPercent = totalProgress.toFloat().coerceIn(0f, 1f)
        val waterY = center.y + radius - (radius * 2f * liquidFillPercent)

        // Generate fluid wave paths (Asymmetric cross-flowing oils/carbs macros)
        val fluidPath1 = Path()
        val fluidPath2 = Path()

        fluidPath1.moveTo(center.x - radius, center.y + radius)
        fluidPath2.moveTo(center.x - radius, center.y + radius)

        val steps = 40
        for (i in 0..steps) {
            val fraction = i.toFloat() / steps
            val px = center.x - radius + (radius * 2 * fraction)
            
            // Formula for organic sine waves cross-flowing
            val py1 = waterY + 8f * sin(fraction * 2 * Math.PI.toFloat() + waveOffset)
            val py2 = waterY + 5f * sin(fraction * 3 * Math.PI.toFloat() - waveOffset + 2f)

            fluidPath1.lineTo(px, py1)
            fluidPath2.lineTo(px, py2)
        }

        fluidPath1.lineTo(center.x + radius, center.y + radius)
        fluidPath1.close()

        fluidPath2.lineTo(center.x + radius, center.y + radius)
        fluidPath2.close()

        // Macro color blends for physical liquids
        val carbColor = Color(0x80FB923C) // soft peach
        val proteinColor = Color(0x80818CF8) // amethyst peri-blue
        val fatColor = Color(0x66D97706) // gold/olive

        // Drawing liquid layers
        drawPath(
            path = fluidPath1,
            brush = Brush.linearGradient(
                colors = listOf(proteinColor, fatColor, carbColor),
                start = Offset(center.x - radius, waterY),
                end = Offset(center.x + radius, center.y + radius)
            )
        )

        drawPath(
            path = fluidPath2,
            brush = Brush.linearGradient(
                colors = listOf(carbColor.copy(alpha = 0.4f), proteinColor.copy(alpha = 0.5f)),
                start = Offset(center.x + radius, waterY - 10),
                end = Offset(center.x - radius, center.y + radius)
            )
        )

        // Top glossy light reflection to sell the glass sphere look
        drawOval(
            brush = Brush.linearGradient(
                colors = listOf(Color.White.copy(alpha = 0.2f), Color.Transparent),
                start = Offset(center.x - radius/2, center.y - radius + 10f),
                end = Offset(center.x, center.y)
            ),
            topLeft = Offset(center.x - radius * 0.5f, center.y - radius + 15f),
            size = Size(radius, radius * 0.4f)
        )

        drawContext.canvas.restore()
    }
}

/**
 * Water tracker waving smoothly at the very bottom.
 */
@Composable
fun WaterWaveBottom(
    progress: Float,
    drunkMl: Int,
    targetMl: Int,
    onAddWater: () -> Unit,
    onResetWater: () -> Unit,
    modifier: Modifier = Modifier
) {
    val localDensity = LocalDensity.current
    val waveHeight = 90.dp
    
    val transition = rememberInfiniteTransition(label = "water_dynamics")
    val waveOffset by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "waterWave"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(130.dp)
            .drawBehind {
                // Wave background clipping area
                val surfaceY = size.height - 30f - (size.height * 0.55f * progress)
                val path = Path()
                path.moveTo(0f, size.height)
                path.lineTo(0f, surfaceY)
                
                val steps = 30
                for (i in 0..steps) {
                    val fraction = i.toFloat() / steps
                    val px = size.width * fraction
                    // dynamic wave frequency
                    val py = surfaceY + 12f * sin(fraction * 3 * Math.PI.toFloat() + waveOffset)
                    path.lineTo(px, py)
                }
                path.lineTo(size.width, size.height)
                path.close()

                drawPath(
                    path = path,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xCC06B6D4), // Cyan active top
                            Color(0xFF0E7490)  // Dark deep teal base
                        )
                    )
                )
            }
            .padding(bottom = 8.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                IconButton(
                    onClick = onResetWater,
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color(0x33000000), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "Убрать воду",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Interactive increment glass
                Button(
                    onClick = onAddWater,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    shape = RoundedCornerShape(20.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    modifier = Modifier.testTag("add_water_button")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.WaterDrop,
                            contentDescription = "Вода",
                            tint = Color(0xFF06B6D4),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "+250 мл воды",
                            color = Color(0xFF0E7490),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Current total water drunk label
                Column(horizontalAlignment = Alignment.Start) {
                    Text(
                        text = "$drunkMl мл",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "цель: $targetMl мл",
                        fontSize = 9.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

/**
 * Onboarding connected via Google Health Connect simulation - ZERO questionnaires, Instant start!
 */
@Composable
fun OnboardingOverlay(onCompleted: (target: Int, name: String, connected: Boolean) -> Unit) {
    var step by remember { mutableIntStateOf(1) }
    var nickname by remember { mutableStateOf("") }
    var customCalorie by remember { mutableStateOf("2000") }
    var healthConnectedStatus by remember { mutableStateOf("Ожидание") }

    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF111827)), // Deep grey-blue obsidian
            shape = RoundedCornerShape(28.dp),
            border = BorderStroke(1.dp, Color(0x33FFFFFF))
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (step == 1) {
                    // Title and greeting
                    Text(
                        text = "ZERO",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 6.sp,
                        color = Color.White
                    )
                    Text(
                        text = "минимум рутины • ноль допросов",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Text(
                        text = "Никаких вопросов про ваш рост, вес, процент жира и цели похудения на старте.\n\nМы подключимся к вашим системным часам активности, либо настроим базовый умный коридор 2000 ккал.\n\nЗа неделю ИИ Zero сам определит скорость вашего обмена веществ по темпу вашей жизни.",
                        fontSize = 13.sp,
                        color = Color.LightGray,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    // Simulated 1-click Apple Health / Health Connect button
                    Button(
                        onClick = {
                            healthConnectedStatus = "Подключено"
                            step = 2
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF818CF8)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .testTag("onboarding_agree_button"),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.HealthAndSafety,
                                contentDescription = "Health",
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Войти через Health Connect",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    TextButton(
                        onClick = {
                            healthConnectedStatus = "Пропущено"
                            step = 2
                        },
                        modifier = Modifier.testTag("onboarding_skip_button")
                    ) {
                        Text(
                            text = "Настроить коридор вручную",
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                    }
                } else {
                    // Step 2: Confirmation / Customize name and basic base
                    Text(
                        text = "Калибровка Коридора",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = nickname,
                        onValueChange = { nickname = it },
                        label = { Text("Ваше имя") },
                        placeholder = { Text("Максим") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.LightGray,
                            focusedBorderColor = Color(0xFF818CF8)
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("onboarding_name_field")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = customCalorie,
                        onValueChange = { customCalorie = it },
                        label = { Text("Дневной ориентир ккал") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.LightGray,
                            focusedBorderColor = Color(0xFF818CF8)
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("onboarding_calories_field")
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            val cal = customCalorie.toIntOrNull() ?: 2000
                            onCompleted(
                                cal,
                                nickname.ifBlank { "Максим" },
                                healthConnectedStatus == "Подключено"
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("onboarding_finish_button")
                    ) {
                        Text(
                            text = "Запустить Zero",
                            color = Color(0xFF111827),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

/**
 * Manual input box text prompt.
 */
@Composable
fun ManualInputDialog(onDismiss: () -> Unit, onAdd: (String) -> Unit) {
    var input by remember { mutableStateOf("") }
    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1F2937)),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Что вы съели?",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    placeholder = { Text("Например: Куриный стейк, гречка и капучино без сахара") },
                    modifier = Modifier.fillMaxWidth().height(100.dp).testTag("manual_input_field"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.LightGray
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Отмена", color = Color.Gray)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onAdd(input) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF818CF8)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("manual_input_submit")
                    ) {
                        Text("Распознать ИИ", color = Color.White)
                    }
                }
            }
        }
    }
}

/**
 * Audio Recording dictation dialog.
 */
@Composable
fun VoiceDictationDialog(onDismiss: () -> Unit, onChoosePreset: (String) -> Unit) {
    var isSimulatingTranscribe by remember { mutableStateOf(false) }
    var transcriptionText by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1F2937)),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Голосовой ИИ-анализ",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Без кнопок и поиска. Просто скажите обычным русским языком — Zero переведет это в научное КБЖУ.",
                    fontSize = 12.sp,
                    color = Color.LightGray,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Pulsing voice circle
                val transition = rememberInfiniteTransition(label = "pulse")
                val pulseRadius by transition.animateFloat(
                    initialValue = 70f,
                    targetValue = if (isSimulatingTranscribe) 95f else 75f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(800, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "pulseAudio"
                )

                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .drawBehind {
                            drawCircle(
                                color = Color(0x33818CF8),
                                radius = pulseRadius
                            )
                        }
                        .background(Color(0xFF818CF8), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Mic,
                        contentDescription = "Запись",
                        tint = Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (isSimulatingTranscribe) {
                    Text(
                        text = transcriptionText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.height(40.dp)
                    )
                } else {
                    Text(
                        text = "Слушаю вас...",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }

                // Preset Speech options to simulate dictation instantly!
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Или выберите готовый голосовой слепок:",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(6.dp))

                val speechModelPresets = listOf(
                    "«Я съел тарелку борща и кусок хлеба с маслом»",
                    "«Овсяная каша с бананом и чашка латте»",
                    "«Порция домашнего плова и салат греческий»"
                )

                speechModelPresets.forEach { text ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable {
                                isSimulatingTranscribe = true
                                scope.launch {
                                    transcriptionText = ""
                                    // Letters typed simulation
                                    val words = text.split(" ")
                                    for (word in words) {
                                        transcriptionText += "$word "
                                        delay(150)
                                    }
                                    delay(400)
                                    onChoosePreset(text.replace("«", "").replace("»", ""))
                                }
                            },
                        colors = CardDefaults.cardColors(containerColor = Color(0x0FFFFFFF))
                    ) {
                        Text(
                            text = text,
                            fontSize = 12.sp,
                            color = Color.LightGray,
                            modifier = Modifier.padding(12.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                
                TextButton(onClick = onDismiss) {
                    Text("Закрыть", color = Color.Gray)
                }
            }
        }
    }
}

/**
 * Camera Scan multimodal viewfinder dialog with shining gloss analysis.
 */
@Composable
fun CameraScannerDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit,
    onIdentify: (Bitmap, String) -> Unit
) {
    var customQueryContext by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Media picking handler to allow scanning any image
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                if (bitmap != null) {
                    onIdentify(bitmap, customQueryContext)
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Ошибка загрузки фото", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF111827)),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "ИИ Фотосканер Smart Vision",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Угадывает скрытые масла, соусы и жир по текстуре и блеску блюда.",
                    fontSize = 11.sp,
                    color = Color.LightGray,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Simulated shining camera viewfinder
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black)
                        .border(2.dp, Color(0xFF06B6D4), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.PhotoCamera,
                        contentDescription = "Вьюфиндер",
                        tint = Color(0xFF06B6D4).copy(alpha = 0.4f),
                        modifier = Modifier.size(54.dp)
                    )
                    
                    // Rotating green lens indicator
                    val infinity = rememberInfiniteTransition(label = "scanner")
                    val shineLineY by infinity.animateFloat(
                        initialValue = 0f,
                        targetValue = 160f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1500, easing = EaseInOutSine),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "shineLine"
                    )

                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawLine(
                            color = Color(0xFF06B6D4),
                            start = Offset(0f, shineLineY.dp.toPx()),
                            end = Offset(size.width, shineLineY.dp.toPx()),
                            strokeWidth = 3f
                        )
                    }

                    Text(
                        text = "[ Smart Vision Локатор ]",
                        color = Color(0xFF06B6D4),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = customQueryContext,
                    onValueChange = { customQueryContext = it },
                    label = { Text("Есть скрытые детали? Напишите (необязательно)") },
                    placeholder = { Text("Например: Добавлено к овощам 2 ложки масла") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.LightGray
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("photo_scan_context")
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Или отсканируйте один из готовых образцов:",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    FoodImageSampleButton(
                        label = "Греческий салат",
                        description = "Масло на дне, сыр сияет",
                        onClick = {
                            val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
                            onIdentify(bitmap, "греческий салат фета оливковое масло блеск")
                        }
                    )
                    FoodImageSampleButton(
                        label = "Куриный плов",
                        description = "Контекст уварки риса",
                        onClick = {
                            val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
                            onIdentify(bitmap, "плов с курицей большая тарелка жирный рис")
                        }
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Button(
                        onClick = { galleryLauncher.launch("image/*") },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0x33ffffff)),
                        modifier = Modifier.weight(1f).testTag("photo_scan_upload")
                    ) {
                        Text("Из галереи", color = Color.White)
                    }
                    
                    Spacer(modifier = Modifier.width(10.dp))

                    Button(
                        onClick = {
                            // Empty fast camera click
                            val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
                            onIdentify(bitmap, "салат куриный грудка с текстурой масла")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF06B6D4)),
                        modifier = Modifier.weight(1f).testTag("photo_scan_submit")
                    ) {
                        Text("Сканировать", color = Color.White)
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                TextButton(onClick = onDismiss) {
                    Text("Закрыть", color = Color.Gray)
                }
            }
        }
    }
}

@Composable
fun FoodImageSampleButton(label: String, description: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .clickable { onClick() }
            .width(130.dp)
            .padding(4.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0x1AFFFFFF))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(label, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(description, color = Color.Gray, fontSize = 9.sp)
        }
    }
}

/**
 * Smart Pot recipe developer screen.
 */
@Composable
fun SmartPotDialog(viewModel: MainViewModel, onDismiss: () -> Unit) {
    var title by remember { mutableStateOf("") }
    var rawIngredientsText by remember { mutableStateOf("") }
    var emptyPotWeight by remember { mutableStateOf("") }
    var totalWeightWithPot by remember { mutableStateOf("") }

    // Macronutrient values for raw elements
    var rawKcal by remember { mutableStateOf("") }
    var rawProtein by remember { mutableStateOf("") }
    var rawFat by remember { mutableStateOf("") }
    var rawCarbs by remember { mutableStateOf("") }

    val context = LocalContext.current

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1F2937)),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "«Умная кастрюля» Smart Pot",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Идеальный расчет супов и плова с уваркой. Без калькуляторов.",
                    fontSize = 11.sp,
                    color = Color.LightGray
                )
                
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Название блюда") },
                    placeholder = { Text("Мой Борщ мясной") },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White),
                    modifier = Modifier.fillMaxWidth().testTag("recipe_title_field")
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = rawIngredientsText,
                    onValueChange = { rawIngredientsText = it },
                    label = { Text("Сырые ингредиенты (списком)") },
                    placeholder = { Text("Куриное бедро 400г, картошка 300г, зажарка") },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White),
                    modifier = Modifier.fillMaxWidth().testTag("recipe_ingredients_field")
                )

                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Установите сырую КБЖУ ценность в сумме:",
                    fontSize = 12.sp,
                    color = Color(0xFF818CF8),
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = rawKcal,
                        onValueChange = { rawKcal = it },
                        label = { Text("Ккал") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White),
                        modifier = Modifier.weight(1f).testTag("recipe_raw_kcal")
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    OutlinedTextField(
                        value = rawProtein,
                        onValueChange = { rawProtein = it },
                        label = { Text("Б") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White),
                        modifier = Modifier.weight(1f).testTag("recipe_raw_protein")
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    OutlinedTextField(
                        value = rawFat,
                        onValueChange = { rawFat = it },
                        label = { Text("Ж") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White),
                        modifier = Modifier.weight(1f).testTag("recipe_raw_fat")
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    OutlinedTextField(
                        value = rawCarbs,
                        onValueChange = { rawCarbs = it },
                        label = { Text("У") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White),
                        modifier = Modifier.weight(1f).testTag("recipe_raw_carbs")
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Весовая калибровка:",
                    fontSize = 12.sp,
                    color = Color(0xFFFBBF24),
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = emptyPotWeight,
                        onValueChange = { emptyPotWeight = it },
                        label = { Text("Вес пустой посуды (г)") },
                        placeholder = { Text("1100") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White),
                        modifier = Modifier.weight(1f).testTag("recipe_empty_pot")
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value = totalWeightWithPot,
                        onValueChange = { totalWeightWithPot = it },
                        label = { Text("Вес посуды + супа (г)") },
                        placeholder = { Text("2600") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White),
                        modifier = Modifier.weight(1f).testTag("recipe_total_pot")
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Отмена", color = Color.Gray)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Button(
                        onClick = {
                            val name = title.ifBlank { "Домашний суп" }
                            val potW = emptyPotWeight.toDoubleOrNull() ?: 1000.0
                            val fullW = totalWeightWithPot.toDoubleOrNull() ?: 2500.0
                            val k = rawKcal.toDoubleOrNull() ?: 1200.0
                            val p = rawProtein.toDoubleOrNull() ?: 60.0
                            val f = rawFat.toDoubleOrNull() ?: 45.0
                            val c = rawCarbs.toDoubleOrNull() ?: 150.0

                            viewModel.saveSmartPotRecipe(
                                title = name,
                                rawIngredients = rawIngredientsText.ifBlank { "Курица, овощи" },
                                emptyPotWeightGrams = potW,
                                totalWeightWithPotGrams = fullW,
                                totalCalories = k,
                                totalProtein = p,
                                totalFat = f,
                                totalCarbs = c
                            )
                            Toast.makeText(context, "Рецепт «$name» вычислен и сохранен!", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF818CF8)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("recipe_save_button")
                    ) {
                        Text("Создать умное блюдо", color = Color.White)
                    }
                }
            }
        }
    }
}

/**
 * Saved recipes list drawer dialog.
 */
@Composable
fun RecipeListDialog(
    recipes: List<Recipe>,
    onDismiss: () -> Unit,
    onSelect: (Recipe, Double) -> Unit,
    onDelete: (Int) -> Unit
) {
    var selectedRecipeForCalc by remember { mutableStateOf<Recipe?>(null) }
    var inputGramsToConsume by remember { mutableStateOf("250") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF111827)),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                if (selectedRecipeForCalc == null) {
                    Text(
                        text = "База блюд Smart Pot",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    if (recipes.isEmpty()) {
                        Text(
                            text = "Вы еще не создали ни одного умного блюда. Нажмите значок кастрюли на главном экране, чтобы рассчитать кастрюлю супа за секунду!",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            lineHeight = 16.sp,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    } else {
                        Column {
                            recipes.forEach { recipe ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clickable { selectedRecipeForCalc = recipe },
                                    colors = CardDefaults.cardColors(containerColor = Color(0x1AFFFFFF))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(recipe.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            Text(
                                                text = "Уварка: ${recipe.netCookedWeightGrams.toInt()}г чистого блюда\nЦенность: ${recipe.caloriesPer100g.toInt()} ккал/100г",
                                                color = Color.Gray,
                                                fontSize = 11.sp
                                            )
                                        }
                                        IconButton(onClick = { onDelete(recipe.id) }) {
                                            Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = Color.Red, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                        Text("Закрыть", color = Color.Gray)
                    }
                } else {
                    val r = selectedRecipeForCalc!!
                    Text(
                        text = "Взвешивание порции: ${r.title}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Калорийность блюда измеряется как ${r.caloriesPer100g.toInt()} ккал на 100 грамм готового супа. Сколько вы съели?",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        lineHeight = 16.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = inputGramsToConsume,
                        onValueChange = { inputGramsToConsume = it },
                        label = { Text("Вес порции в граммах") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    val grams = inputGramsToConsume.toDoubleOrNull() ?: 250.0
                    val mealResultKcal = r.caloriesPer100g * (grams / 100.0)

                    Text(
                        text = "Результат: ${mealResultKcal.toInt()} ккал запишется в дневник",
                        fontSize = 13.sp,
                        color = Color(0xFFFBBF24),
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { selectedRecipeForCalc = null }) {
                            Text("Назад", color = Color.Gray)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Button(
                            onClick = {
                                onSelect(r, grams)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF818CF8))
                        ) {
                            Text("Записать в дневник")
                        }
                    }
                }
            }
        }
    }
}
