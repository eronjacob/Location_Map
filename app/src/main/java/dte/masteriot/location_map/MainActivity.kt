package dte.masteriot.location_map

import android.Manifest //New for GPS Permission Milestone 3: Hardware GPS & Edge Location (Exercise 5)
import android.content.Context // New for location Milestone 3: Hardware GPS & Edge Location (Exercise 5)
import android.content.pm.PackageManager // New for Permission checking Milestone 3: Hardware GPS & Edge Location (Exercise 5)
import android.location.LocationManager // New for Hardware GPS Milestone 3: Hardware GPS & Edge Location (Exercise 5)
import android.os.Bundle
import android.widget.Toast // Missing Import Fixed
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult // New for pop-up Milestone 3: Hardware GPS & Edge Location (Exercise 5)
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts // New for pop-up Milestone 3: Hardware GPS & Edge Location (Exercise 5)
import androidx.compose.foundation.layout.Box // Missing Import Fixed
import androidx.compose.foundation.layout.Column // New Layout Milestone 3: Hardware GPS & Edge Location (Exercise 5)
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button // New for UI Button Milestone 3: Hardware GPS & Edge Location (Exercise 5)
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember // New to store the map state Milestone 3: Hardware GPS & Edge Location (Exercise 5)
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp //Added: Fixed the "Unresolved reference 'dp'" error
import androidx.compose.ui.viewinterop.AndroidView //Missing Import Fixed
import androidx.core.content.ContextCompat // New for Permission checking Milestone 3: Hardware GPS & Edge Location (Exercise 5)
import dte.masteriot.location_map.ui.theme.Location_MapTheme

// osmdroid imports
import org.osmdroid.config.Configuration // Import needed for User-Agent
import org.osmdroid.tileprovider.tilesource.TileSourceFactory // Import for MAPNIK
import org.osmdroid.util.GeoPoint // Import for coordinates
import org.osmdroid.views.MapView // Import for the classic MapView-

//Add these new imports at the top of MainActivity.kt:
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.views.overlay.MapEventsOverlay

//Exercise 3: Add a marker and Exercise 4: Draw a Shape
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import android.graphics.Color

// Exercise 6: Cloud Routing
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import org.osmdroid.views.overlay.Polyline

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        //1. Identify your app to the OSM servers BEFORE setting the UI.
        // This prevents the map from loading as a blank, grey grid.
        Configuration.getInstance().userAgentValue = packageName

        setContent {
            Location_MapTheme {
                Scaffold(modifier = Modifier. fillMaxSize()) { innerPadding ->

                    // 2. Call the map composable instead of the Greeting text
                    MyMapScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

//3. The Composable that bridges the classic osmdroid MapView into Jetpack Compose
@Composable
fun MyMapScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // We keep your exact map logic intact, but wrap it in a 'remember' block so the button can use it
    val mapView = remember {
        MapView(context).apply {
            //Set the map style to standard roads
            setTileSource(TileSourceFactory.MAPNIK)

            //Enable pinch-to-zoom gestures
            setMultiTouchControls(true)

            //Set an initial starting point (UPM Campus Sur)
            val upmCampusSur = GeoPoint(40.3896, -3.6274)
            controller.setZoom(15.0)
            controller.setCenter(upmCampusSur)

            // --Exercise 3: Add a Marker
            val startMarker = Marker(this)
            startMarker.position = upmCampusSur
            startMarker.title = "UPM Campus Sur"

            //Add the marker to the map overlays
            overlays.add(startMarker)

            // --Exercise 4: Draw a Shape (Geofence) --
            //We keep a reference to the active circle so we can remove it before drawing a new one
            var activeCircle: Polygon? = null

            val receiver = object : MapEventsReceiver {
                override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                    Toast.makeText(
                        context,
                        "Latitude: ${p.latitude}\nLongitude: ${p.longitude}",
                        Toast.LENGTH_SHORT
                    ).show()
                    return true
                }

                override fun longPressHelper(p: GeoPoint): Boolean {
                    // 1, Remove the previous circle if it exists (prevents memory lag)
                    activeCircle?.let { overlays.remove(it) }

                    // 2. Create a new Polygon circle at the long-press location
                    val newCircle = Polygon().apply {
                        //Draw a circle with a 300 km radius (300,000 meters)
                        points = Polygon.pointsAsCircle(p, 300000.0)

                        // FIXED DEPRECATIONS: Using the modern Paint API to remove warnings
                        fillPaint.color = Color.argb(75, 255, 0, 0)
                        outlinePaint.color = Color.RED
                        outlinePaint.strokeWidth = 2.0f
                    }

                    //3. Save the reference, add it to the map, and force a redraw
                    activeCircle = newCircle
                    overlays.add(newCircle)
                    invalidate() //Crucial: Tells the map to refresh the screen

                    return true // Indicate the event was consumed
                }
            }

            // Add the touch event overlay
            overlays.add(MapEventsOverlay(receiver))
        }
    }
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                fetchAndDisplayLocation(context, mapView)
            } else {
                Toast.makeText(context, "Location permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    )

    Column(modifier = modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.weight(1f).fillMaxSize(),
            factory = { mapView }
        )

        // We replaced the single Button with a Row containing TWO buttons
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // BUTTON 1: Your exact existing Hardware GPS Button
            Button(
                onClick = {
                    val hasPermission = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED

                    if (hasPermission) {
                        fetchAndDisplayLocation(context, mapView)
                    } else {
                        locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    }
                }
            ) {
                Text("My Location")
            }

            // BUTTON 2: The NEW Cloud Routing API Button
            Button(
                onClick = {
                    coroutineScope.launch {
                        try {
                            // 1. Your API Key
                            val apiKey =
                                "eyJvcmciOiI1YjNjZTM1OTc4NTExMTAwMDFjZjYyNDgiLCJpZCI6ImFiODFhYjA0YmM2NTQ1OTdhOGFiOTBlOTdiOTQ3ZTdhIiwiaCI6Im11cm11cjY0In0="

                            // 2. Format request (openrouteservice requires [Longitude, Latitude])
                            val upmStart = listOf(-3.6274, 40.3896)
                            val solEnd = listOf(-3.7038, 40.4168)
                            val request = RouteRequest(listOf(upmStart, solEnd))

                            // 3. Make the API call over the network
                            val response = NetworkDataSource.service.getRoute(apiKey, request)

                            // 4. Extract the coordinates and draw the blue line
                            val coords = response.features.first().geometry.coordinates
                            val geoPoints = coords.map { GeoPoint(it[1], it[0]) }

                            val routeLine = Polyline().apply {
                                setPoints(geoPoints)
                                // FIXED DEPRECATIONS: Using the modern Paint API
                                outlinePaint.color = Color.BLUE
                                outlinePaint.strokeWidth = 8f
                            }

                            mapView.overlays.add(routeLine)
                            mapView.invalidate() // Refresh map

                        } catch (e: Exception) {
                            Toast.makeText(context, "Routing failed", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            ) {
                Text("Draw Route")
            }
        }
    }
}

// RESTORED: The helper function you accidentally deleted!
private fun fetchAndDisplayLocation(context: Context, mapView: MapView) {
    try {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        var location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)

        if(location == null) {
            location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        }

        if (location != null) {
            val userGeoPoint = GeoPoint(location.latitude, location.longitude)

            mapView.controller.animateTo(userGeoPoint)
            mapView.controller.setZoom(17.0)

            val userMarker = Marker(mapView)
            userMarker.position = userGeoPoint
            userMarker.title = "My Location"
            mapView.overlays.add(userMarker)
            mapView.invalidate()

        } else {
            Toast.makeText(context, "Location not available.", Toast.LENGTH_SHORT).show()
        }
    } catch (e: SecurityException) {
        Toast.makeText(context, "Location permission missing", Toast.LENGTH_SHORT).show()
    }
}