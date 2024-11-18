package com.mano_solidaria.app.solicitantes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.mano_solidaria.app.solicitantes.ui.theme.ManosolidariaTheme

class SolicitantesPropuestasActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ViewContainer()
            Content()
//            ManosolidariaTheme {
//                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
//                    Greeting(
//                        name = "Android",
//                        modifier = Modifier.padding(innerPadding)
//                    )
//                }
//            }
        }
    }
}

@Preview
@Composable
fun ViewContainer() {

}



@Preview
@Composable
fun Content() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp) // Espaciado entre ítems
    ){
        item {
            MyGoogleMaps()
            Spacer(modifier = Modifier.height(16.dp)) // Espacio entre ítems
        }
        item {
            MyReserva()
            Spacer(modifier = Modifier.height(16.dp)) // Espacio entre ítems
        }

        item {
            MyReserva()
            Spacer(modifier = Modifier.height(16.dp)) // Espacio entre ítems
        }

        item {
            MyReserva()
            Spacer(modifier = Modifier.height(16.dp)) // Espacio entre ítems
        }
        item {
            MyReserva()
            Spacer(modifier = Modifier.height(16.dp)) // Espacio entre ítems
        }
        item {
            MyReserva()
            Spacer(modifier = Modifier.height(16.dp)) // Espacio entre ítems
        }
        item {
            MyReserva()
            Spacer(modifier = Modifier.height(16.dp)) // Espacio entre ítems
        }
        item {
            MyReserva()
            Spacer(modifier = Modifier.height(16.dp)) // Espacio entre ítems
        }
        item {
            MyReserva()
            Spacer(modifier = Modifier.height(16.dp)) // Espacio entre ítems
        }
        item {
            MyReserva()
            Spacer(modifier = Modifier.height(16.dp)) // Espacio entre ítems
        }
        item {
            MyReserva()
            Spacer(modifier = Modifier.height(16.dp)) // Espacio entre ítems
        }
    }
}


@Composable
fun MyGoogleMaps() {
    val singapore = LatLng(1.35, 103.87)
    val uiSettings by remember { mutableStateOf(MapUiSettings(zoomControlsEnabled = true)) }

    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val mapHeight = screenHeight / 3

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(singapore, 15f)
    }
    GoogleMap(
        modifier = Modifier.fillMaxWidth().height(mapHeight),
        cameraPositionState = cameraPositionState,
        uiSettings = uiSettings,
    ) {
        Marker(
            state = MarkerState(position = singapore),
            title = "Singapore",
            snippet = "Marker in Singapore"
        )
    }
}


@Composable
fun MyReserva(){
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, color = Color.Red),
        verticalAlignment = Alignment.CenterVertically
    ){
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ){
            AsyncImage(
                model = "https://peruretail.sfo3.cdn.digitaloceanspaces.com/wp-content/uploads/Pollo-a-al-abrasa.jpg",
                contentDescription = "Imagen de reserva",
                modifier = Modifier.size(80.dp).padding(end = 8.dp),
                contentScale = ContentScale.Crop
            )
        }
        Column(
            modifier = Modifier
                .weight(2f)
                .fillMaxHeight()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "50 kg Pollo",
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Caduca en: 2 días",
                )
            }
        }
        Column(
            modifier = Modifier
                .weight(2f)
                .fillMaxHeight()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Coto",
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "7 km",
                )
            }
        }
    }
}



@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    ManosolidariaTheme {
        Greeting("Android")
    }
}