package com.zixion.mangaverse.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import coil3.compose.AsyncImage
import com.zixion.mangaverse.CommonSerializable
import com.zixion.mangaverse.models.Manga
import com.zixion.mangaverse.network.MangaService
import com.zixion.mangaverse.network.UserManager
import kotlinx.coroutines.launch

data class CapitulosScreen(val mangaInicial: Manga) : Screen, CommonSerializable {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val scope = rememberCoroutineScope()
        val listState = rememberLazyListState()

        val navigator = LocalNavigator.current
        val servicio = remember { MangaService() }

        var mangaCompleto by remember { mutableStateOf(mangaInicial) }

        var listaCapitulos by rememberSaveable { mutableStateOf<List<String>>(emptyList()) }
        var cargando by rememberSaveable { mutableStateOf(true) }
        var colorCargado by rememberSaveable { mutableStateOf<Boolean?>(null) }

        var infoRevisada by rememberSaveable { mutableStateOf(false) }

        var textoBusqueda by remember { mutableStateOf("") }
        var existeColor by rememberSaveable { mutableStateOf(false) }
        var modoColorActivo by rememberSaveable { mutableStateOf(false) }

        var mostrarDialogoBorrarProgreso by remember { mutableStateOf(false) }

        var sinopsisExpandida by remember { mutableStateOf(false) }
        var ordenInverso by remember { mutableStateOf(false) }
        var refreshTrigger by remember { mutableStateOf(0) }

        LaunchedEffect(Unit) {
            if (!infoRevisada) {
                UserManager.cargar()
                if (mangaCompleto.sinopsis == "Sin descripción" || mangaCompleto.sinopsis.isEmpty()) {
                    mangaCompleto = servicio.obtenerInfoManga(mangaInicial)
                }
                existeColor = servicio.tieneColor(mangaInicial.titulo)
                infoRevisada = true
            }
        }

        LaunchedEffect(modoColorActivo) {
            if (colorCargado != modoColorActivo || listaCapitulos.isEmpty()) {
                cargando = true
                listaCapitulos = servicio.obtenerCapitulos(mangaInicial.titulo, modoColorActivo)
                colorCargado = modoColorActivo
                cargando = false
            }
        }

        // NUEVO: Aseguramos que la lista suba al principio de forma fiable
        // tanto al cambiar el orden como al escribir en el buscador
        LaunchedEffect(ordenInverso, textoBusqueda) {
            listState.scrollToItem(0)
        }

        val capitulosFiltrados = remember(textoBusqueda, listaCapitulos, ordenInverso) {
            val filtrados = if (textoBusqueda.isBlank()) listaCapitulos
            else listaCapitulos.filter { it.contains(textoBusqueda, ignoreCase = true) }
            if (ordenInverso) filtrados.asReversed() else filtrados
        }

        val colorEstado = remember(mangaCompleto.estado) {
            val estado = mangaCompleto.estado.lowercase()
            if (estado.contains("finalizado") || estado.contains("terminado")) Color(0xFFE50914) else Color(0xFF2ECC71)
        }

        Scaffold(
            containerColor = Color(0xFF141414)
        ) { padding ->
            Box(modifier = Modifier.padding(padding).fillMaxSize()) {
                Column(modifier = Modifier.fillMaxSize().background(Color(0xFF141414))) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        AsyncImage(model = mangaCompleto.urlPortada, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.matchParentSize().blur(radius = 15.dp).alpha(0.4f))
                        Box(modifier = Modifier.matchParentSize().background(Brush.verticalGradient(colors = listOf(Color.Transparent, Color(0xFF141414)))))
                        IconButton(onClick = { navigator?.pop() }, modifier = Modifier.align(Alignment.TopStart).padding(start = 4.dp, top = 4.dp)) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White)
                        }
                        Row(modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 50.dp, bottom = 12.dp), verticalAlignment = Alignment.Bottom) {
                            AsyncImage(model = mangaCompleto.urlPortada, contentDescription = null, modifier = Modifier.width(90.dp).height(130.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.height(130.dp)) {
                                Box(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                                    Text(mangaCompleto.titulo, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White, lineHeight = 24.sp)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(color = colorEstado, shape = RoundedCornerShape(4.dp), modifier = Modifier.padding(end = 8.dp)) {
                                        Text(mangaCompleto.estado.uppercase(), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                    }
                                    Text(mangaCompleto.tipo.uppercase(), fontSize = 12.sp, color = Color.Gray)
                                }
                            }
                        }
                    }

                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {

                        if (mangaCompleto.generos.isNotEmpty()) {
                            LazyRow(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(mangaCompleto.generos) { genero ->
                                    Surface(
                                        color = Color.DarkGray.copy(alpha = 0.6f),
                                        shape = RoundedCornerShape(16.dp)
                                    ) {
                                        Text(
                                            text = genero.trim(),
                                            color = Color.LightGray,
                                            fontSize = 12.sp,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Column(modifier = Modifier.animateContentSize()) { Text(text = mangaCompleto.sinopsis, color = Color.LightGray, fontSize = 14.sp, lineHeight = 20.sp, maxLines = if (sinopsisExpandida) Int.MAX_VALUE else 3, overflow = TextOverflow.Ellipsis) }
                        Text(text = if (sinopsisExpandida) "Mostrar menos" else "Mostrar todo...", color = Color(0xFFE50914), fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp, bottom = 12.dp).clickable { sinopsisExpandida = !sinopsisExpandida })

                        if (existeColor) {
                            Button(onClick = { modoColorActivo = !modoColorActivo }, colors = ButtonDefaults.buttonColors(containerColor = if (modoColorActivo) Color(0xFFE50914) else Color.DarkGray), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp)) { Text(if (modoColorActivo) "Ver Original 📄" else "Ver a Color 🎨") }
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        OutlinedTextField(value = textoBusqueda, onValueChange = { textoBusqueda = it }, placeholder = { Text("Buscar capítulo...", color = Color.Gray) }, leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) }, trailingIcon = { if (textoBusqueda.isNotEmpty()) IconButton(onClick = { textoBusqueda = "" }) { Icon(Icons.Default.Close, contentDescription = "Borrar", tint = Color.Gray) } }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, cursorColor = Color.Red, focusedBorderColor = Color.Red, unfocusedBorderColor = Color.DarkGray), singleLine = true, shape = RoundedCornerShape(12.dp))

                        Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Capítulos: ${capitulosFiltrados.size}", color = Color.White, fontWeight = FontWeight.Bold)

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = {
                                    scope.launch {
                                        cargando = true
                                        listaCapitulos = servicio.obtenerCapitulos(mangaInicial.titulo, modoColorActivo, forceRefresh = true)
                                        cargando = false
                                    }
                                }) {
                                    Icon(Icons.Default.Refresh, contentDescription = "Actualizar Capítulos", tint = Color.Gray)
                                }

                                val tieneProgreso = remember(refreshTrigger) { UserManager.tieneCapitulosLeidos(mangaCompleto.titulo) }

                                if (tieneProgreso) {
                                    IconButton(onClick = { mostrarDialogoBorrarProgreso = true }, modifier = Modifier.size(36.dp)) {
                                        Icon(Icons.Default.Delete, contentDescription = "Borrar progreso", tint = Color.Gray)
                                    }
                                }

                                // Hemos quitado de aquí el scrollToItem porque ahora lo maneja el LaunchedEffect de arriba
                                TextButton(onClick = { ordenInverso = !ordenInverso }) {
                                    Text(if (ordenInverso) "Más nuevos" else "Más viejos", color = Color.Gray, fontSize = 12.sp)
                                    Spacer(Modifier.width(4.dp))
                                    Icon(if(ordenInverso) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, "Ordenar", tint = Color.Gray)
                                }
                            }
                        }
                    }

                    LazyColumn(state = listState, modifier = Modifier.fillMaxWidth().weight(1f), contentPadding = PaddingValues(bottom = 80.dp)) {
                        if (cargando) {
                            item { Box(modifier = Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Color.Red) } }
                        } else {
                            if (capitulosFiltrados.isEmpty()) {
                                item { Text("No se encontraron capítulos.", color = Color.Gray, modifier = Modifier.padding(16.dp)) }
                            } else {
                                items(items = capitulosFiltrados, key = { it }, contentType = { "capitulo" }) { capitulo ->
                                    val nombreLimpio = capitulo.replace(".cbz", "").replace(".zip", "")

                                    val leido = remember(refreshTrigger, modoColorActivo) { UserManager.isCapituloLeido(mangaCompleto.titulo, nombreLimpio, modoColorActivo) }

                                    CapituloItem(
                                        nombre = nombreLimpio,
                                        leido = leido,
                                        onClick = {
                                            navigator?.push(LectorCapituloScreen(manga = mangaCompleto, capituloInicial = capitulo, listaTodosCapitulos = listaCapitulos, esColor = modoColorActivo))
                                        },
                                        onMarcarLeido = {
                                            if (leido) UserManager.desmarcarCapitulo(mangaCompleto.titulo, nombreLimpio, modoColorActivo)
                                            else {
                                                UserManager.marcarCapituloComoLeido(mangaCompleto.titulo, nombreLimpio, modoColorActivo)
                                                UserManager.guardarProgreso(mangaCompleto.titulo, nombreLimpio, 0, modoColorActivo)
                                            }
                                            refreshTrigger++
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (mostrarDialogoBorrarProgreso) {
            AlertDialog(
                onDismissRequest = { mostrarDialogoBorrarProgreso = false },
                title = { Text("¿Olvidar progreso?", color = Color.White, fontWeight = FontWeight.Bold) },
                text = { Text("Se marcarán todos los capítulos como no leídos y este manga desaparecerá de tu lista de 'Continuar Leyendo'.", color = Color.LightGray) },
                containerColor = Color(0xFF1E1E1E),
                confirmButton = {
                    Button(
                        onClick = {
                            UserManager.borrarProgresoManga(mangaCompleto.titulo)
                            refreshTrigger++
                            mostrarDialogoBorrarProgreso = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914))
                    ) { Text("Borrar progreso") }
                },
                dismissButton = {
                    TextButton(onClick = { mostrarDialogoBorrarProgreso = false }) { Text("Cancelar", color = Color.White) }
                }
            )
        }
    }
}

@Composable
fun CapituloItem(nombre: String, leido: Boolean, onClick: () -> Unit, onMarcarLeido: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp).clickable(onClick = onClick)) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = if(leido) Color.Gray else Color.Red)
                Spacer(modifier = Modifier.width(12.dp))
                Text(text = nombre, color = if(leido) Color.Gray else Color.White, fontWeight = if(leido) FontWeight.Normal else FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Surface(color = if (leido) Color(0xFF2ECC71).copy(alpha = 0.2f) else Color.Gray.copy(alpha = 0.2f), shape = RoundedCornerShape(6.dp), border = BorderStroke(1.dp, if (leido) Color(0xFF2ECC71) else Color.Gray), modifier = Modifier.clip(RoundedCornerShape(6.dp)).clickable(onClick = onMarcarLeido)) {
                Text(text = if (leido) "LEÍDO" else "LEER", color = if (leido) Color(0xFF2ECC71) else Color.LightGray, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
            }
        }
    }
}