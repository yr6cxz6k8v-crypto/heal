package com.example.ui

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val geminiService = GeminiService()
    private val repository = AppRepository(database.appDao(), geminiService)

    // Calculate start of today (00:00)
    val startOfToday: Long
        get() = getStartOfTodayTimestamp()

    // Reactive states
    val todayMeals: StateFlow<List<Meal>> = repository.getTodayMeals(startOfToday)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todayWater: StateFlow<List<WaterIntake>> = repository.getTodayWaterIntakes(startOfToday)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allRecipes: StateFlow<List<Recipe>> = repository.allRecipes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userPreferences: StateFlow<UserPreferences> = repository.userPreferences
        .map { it ?: UserPreferences() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserPreferences())

    // AI thinking state
    private val _aiLoading = MutableStateFlow(false)
    val aiLoading: StateFlow<Boolean> = _aiLoading.asStateFlow()

    // Last parsed meal notification
    private val _lastAddedReport = MutableStateFlow<FoodKcalReport?>(null)
    val lastAddedReport: StateFlow<FoodKcalReport?> = _lastAddedReport.asStateFlow()

    init {
        // Pre-initialize preferences in database at launch
        viewModelScope.launch {
            repository.getPreferences()
        }
    }

    fun clearLastReport() {
        _lastAddedReport.value = null
    }

    /**
     * Parse natural language text or voice transcript
     */
    fun logFoodByText(text: String, onCompleted: (Boolean) -> Unit = {}) {
        if (text.trim().isEmpty()) return
        viewModelScope.launch {
            _aiLoading.value = true
            try {
                val report = repository.parseAndAddVoiceInput(text)
                _lastAddedReport.value = report
                onCompleted(report != null)
            } catch (e: Exception) {
                onCompleted(false)
            } finally {
                _aiLoading.value = false
            }
        }
    }

    /**
     * Parse food image camera/gallery
     */
    fun logFoodByImage(bitmap: Bitmap, contextText: String, onCompleted: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            _aiLoading.value = true
            try {
                val report = repository.analyzeAndAddImageInput(bitmap, contextText)
                _lastAddedReport.value = report
                onCompleted(report != null)
            } catch (e: Exception) {
                onCompleted(false)
            } finally {
                _aiLoading.value = false
            }
        }
    }

    /**
     * Log water intake
     */
    fun addWater(amountMl: Int) {
        viewModelScope.launch {
            repository.addWater(amountMl)
        }
    }

    fun deleteWaterIntake(id: Int) {
        viewModelScope.launch {
            repository.deleteWaterIntakeById(id)
        }
    }

    /**
     * Delete meal
     */
    fun deleteMeal(mealId: Int) {
        viewModelScope.launch {
            repository.deleteMealById(mealId)
        }
    }

    /**
     * Smart Pot (recipe) calculation & logging
     */
    fun saveSmartPotRecipe(
        title: String,
        rawIngredients: String,
        emptyPotWeightGrams: Double,
        totalWeightWithPotGrams: Double,
        totalCalories: Double,
        totalProtein: Double,
        totalFat: Double,
        totalCarbs: Double
    ) {
        viewModelScope.launch {
            val netCookedWeightGrams = (totalWeightWithPotGrams - emptyPotWeightGrams).coerceAtLeast(1.0)
            
            // Formula: (Total calories in raw ingredients / cooked net weight in grams) * 100
            val caloriesPer100g = (totalCalories / netCookedWeightGrams) * 100.0

            val recipe = Recipe(
                title = title,
                rawIngredients = rawIngredients,
                emptyPotWeightGrams = emptyPotWeightGrams,
                totalWeightWithPotGrams = totalWeightWithPotGrams,
                netCookedWeightGrams = netCookedWeightGrams,
                totalCalories = totalCalories,
                totalProtein = totalProtein,
                totalFat = totalFat,
                totalCarbs = totalCarbs,
                caloriesPer100g = caloriesPer100g,
                timestamp = System.currentTimeMillis()
            )
            repository.addRecipe(recipe)
        }
    }

    fun deleteRecipe(recipeId: Int) {
        viewModelScope.launch {
            repository.deleteRecipeById(recipeId)
        }
    }

    /**
     * Onboarding completed
     */
    fun completeOnboarding(calorieTarget: Int, name: String, healthConnected: Boolean) {
        viewModelScope.launch {
            val currentPrefs = repository.getPreferences()
            val updatedPrefs = currentPrefs.copy(
                dailyCalorieTarget = calorieTarget,
                name = name.ifBlank { "Максим" },
                healthConnected = healthConnected,
                onboardingCompleted = true
            )
            repository.savePreferences(updatedPrefs)
        }
    }

    /**
     * Overwrite dynamic daily target (re-configure in profile)
     */
    fun updateDailyCalorieTarget(newTarget: Int) {
        viewModelScope.launch {
            val currentPrefs = repository.getPreferences()
            val updatedPrefs = currentPrefs.copy(dailyCalorieTarget = newTarget)
            repository.savePreferences(updatedPrefs)
        }
    }

    fun resetPreferencesForTesting() {
        viewModelScope.launch {
            val initial = UserPreferences(id = 1, onboardingCompleted = false)
            repository.savePreferences(initial)
        }
    }

    private fun getStartOfTodayTimestamp(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
}
