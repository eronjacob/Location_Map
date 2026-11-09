package dte.masteriot.location_map

import kotlinx.serialization.Serializable
import okhttp3.MediaType.Companion.toMediaType // ADDED: The modern import for MediaType
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import kotlinx.serialization.json.Json

// --- 1. THE DATA MODELS ---
@Serializable
data class RouteRequest(
    val coordinates: List<List<Double>>
)

@Serializable
data class RouteResponse(
    val features: List<RouteFeature>
)

@Serializable
data class RouteFeature(
    val geometry: RouteGeometry,
    val properties: RouteProperties? = null
)

@Serializable
data class RouteGeometry(
    val coordinates: List<List<Double>>
)

@Serializable
data class RouteProperties(
    val summary: RouteSummary? = null
)

@Serializable
data class RouteSummary(
    val distance: Double,
    val duration: Double
)

// --- 2. THE RETROFIT INTERFACE ---
interface RoutesApiService {
    @POST("v2/directions/driving-car/geojson")
    suspend fun getRoute(
        @Header("Authorization") apiKey: String,
        @Body request: RouteRequest
    ): RouteResponse
}

// --- 3. THE NETWORK DATA SOURCE ---
object NetworkDataSource {
    private val json = Json { ignoreUnknownKeys = true }

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://api.heigit.org/openrouteservice/")
        // FIXED: Using the modern extension function format that OkHttp 4+ requires
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    val service: RoutesApiService = retrofit.create(RoutesApiService::class.java)
}