package com.example.data

import android.graphics.Bitmap
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AppRepository(
    private val appDao: AppDao,
    private val geminiService: GeminiService
) {
    // Current day meals and all history
    val allMeals: Flow<List<Meal>> = appDao.getAllMeals()
    
    // Today's meals only
    fun getTodayMeals(startOfToday: Long): Flow<List<Meal>> =
        appDao.getAllMeals().map { list ->
            list.filter { it.timestamp >= startOfToday }
        }

    // Water intakes
    val allWaterIntakes: Flow<List<WaterIntake>> = appDao.getAllWaterIntakes()

    fun getTodayWaterIntakes(startOfToday: Long): Flow<List<WaterIntake>> =
        appDao.getAllWaterIntakes().map { list ->
            list.filter { it.timestamp >= startOfToday }
        }

    // Smart Pot Recipes
    val allRecipes: Flow<List<Recipe>> = appDao.getAllRecipes()

    // Preferences reactive flow
    val userPreferences: Flow<UserPreferences?> = appDao.getUserPreferencesFlow()

    suspend fun getPreferences(): UserPreferences {
        return appDao.getUserPreferences() ?: UserPreferences().also {
            appDao.saveUserPreferences(it)
        }
    }

    suspend fun savePreferences(prefs: UserPreferences) {
        appDao.saveUserPreferences(prefs)
    }

    suspend fun addMeal(meal: Meal) {
        appDao.insertMeal(meal)
    }

    suspend fun deleteMeal(meal: Meal) {
        appDao.deleteMeal(meal)
    }

    suspend fun deleteMealById(id: Int) {
        appDao.deleteMealById(id)
    }

    suspend fun addWater(amountMl: Int) {
        appDao.insertWaterIntake(WaterIntake(amountMl = amountMl))
    }

    suspend fun deleteWaterIntakeById(id: Int) {
        appDao.deleteWaterIntakeById(id)
    }

    suspend fun addRecipe(recipe: Recipe) {
        appDao.insertRecipe(recipe)
    }

    suspend fun deleteRecipeById(id: Int) {
        appDao.deleteRecipeById(id)
    }

    /**
     * AI natural voice or text interpreter
     */
    suspend fun parseAndAddVoiceInput(input: String): FoodKcalReport? {
        val report = geminiService.parseFoodInput(input)
        if (report != null) {
            // Save parsed meal into local DB
            val meal = Meal(
                name = report.name,
                calories = report.calories,
                protein = report.protein,
                fat = report.fat,
                carbs = report.carbs,
                weightGrams = report.weightGrams,
                timestamp = System.currentTimeMillis()
            )
            addMeal(meal)
        }
        return report
    }

    /**
     * AI photo scan inspector
     */
    suspend fun analyzeAndAddImageInput(bitmap: Bitmap, textContext: String): FoodKcalReport? {
        val report = geminiService.analyzeFoodImage(bitmap, textContext)
        if (report != null) {
            val meal = Meal(
                name = report.name,
                calories = report.calories,
                protein = report.protein,
                fat = report.fat,
                carbs = report.carbs,
                weightGrams = report.weightGrams,
                timestamp = System.currentTimeMillis()
            )
            addMeal(meal)
        }
        return report
    }
}
