package com.zixion.mangaverse.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.CachePolicy
import coil3.request.crossfade
import coil3.size.Scale
import coil3.size.Size
import com.zixion.mangaverse.models.Manga
import com.zixion.mangaverse.network.MangaService
import com.zixion.mangaverse.network.UserManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import okio.Path.Companion.toPath

data class LectorCapituloScreen(
    val manga: Manga,
    val capituloInicial: String,
    val listaTodosCapitulos: List<String>,
    val esColor: Boolean
) : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.current
        val servicio = remember { MangaService() }
        val context = LocalPlatformContext.current

        var capituloActual by rememberSaveable { mutableStateOf(capituloInicial) }
        var paginas by rememberSaveable { mutableStateOf<List<String>>(emptyList()) }
        var cargando by rememberSaveable { mutableStateOf(true) }
        var capituloCargado by rememberSaveable { mutableStateOf("") }
        var uiVisible by rememberSaveable { mutableStateOf(true) }

        LaunchedEffect(capituloActual) {
            if (capituloCargado != capituloActual || paginas.isEmpty()) {
                cargando = true
                val nombreLimpio = capituloActual.replace(".cbz", "").replace(".zip", "")

                // Marcamos en el historial cuál es el ÚLTIMO capítulo que hemos tocado de esta obra
                UserManager.actualizarHistorial(manga.titulo, nombreLimpio, esColor)

                paginas = servicio.obtenerPaginas(manga.titulo, capituloActual, esColor)
                capituloCargado = capituloActual
                cargando = false
            }
        }

        val indexActual = listaTodosCapitulos.indexOf(capituloActual)
        val hayAnteriorHistoria = indexActual > 0
        val haySiguienteHistoria = indexActual < listaTodosCapitulos.size - 1

        Scaffold(
            containerColor = Color.Black,
            topBar = {
                AnimatedVisibility(
                    visible = uiVisible,
                    enter = slideInVertically(initialOffsetY = { -it }),
                    exit = slideOutVertically(targetOffsetY = { -it })
                ) {
                    Box(modifier = Modifier.fillMaxWidth().background(Color.Black.copy(alpha = 0.8f)).statusBarsPadding()) {
                        TopBarLector(titulo = capituloActual, esColor = esColor, onBack = { navigator?.pop() })
                    }
                }
            },
            bottomBar = {
                AnimatedVisibility(
                    visible = uiVisible,
                    enter = slideInVertically(initialOffsetY = { it }),
                    exit = slideOutVertically(targetOffsetY = { it })
                ) {
                    Box(modifier = Modifier.fillMaxWidth().background(Color.Black.copy(alpha = 0.9f)).navigationBarsPadding()) {
                        BottomBarMejorada(
                            hayAnterior = hayAnteriorHistoria,
                            haySiguiente = haySiguienteHistoria,
                            onAnterior = { if (hayAnteriorHistoria) capituloActual = listaTodosCapitulos[indexActual - 1] },
                            onSiguiente = { if (haySiguienteHistoria) capituloActual = listaTodosCapitulos[indexActual + 1] }
                        )
                    }
                }
            }
        ) { padding ->
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .clipToBounds()
            ) {
                if (cargando) {
                    CircularProgressIndicator(color = Color.Red, modifier = Modifier.align(Alignment.Center))
                } else if (paginas.isEmpty()) {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(50.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No se pudieron cargar las imágenes", color = Color.Gray)
                    }
                } else {
                    val widthPx = constraints.maxWidth.toFloat()
                    val heightPx = constraints.maxHeight.toFloat()

                    // LA CLAVE DE ORO: Evita el State Leak entre mangas distintos
                    key(manga.titulo, capituloActual) {
                        val nombreLimpio = capituloActual.replace(".cbz", "").replace(".zip", "")
                        val paginaGuardada = remember { UserManager.getPaginaGuardada(manga.titulo, nombreLimpio, esColor) }
                        val estaLeido = remember { UserManager.isCapituloLeido(manga.titulo, nombreLimpio, esColor) }

                        val initialIndex = if (!estaLeido && paginaGuardada in paginas.indices) paginaGuardada else 0

                        val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
                        var ultimaPaginaConocida by remember { mutableIntStateOf(initialIndex) }

                        LaunchedEffect(listState) {
                            snapshotFlow { listState.firstVisibleItemIndex }
                                .collectLatest { index ->
                                    val item = listState.layoutInfo.visibleItemsInfo.find { it.index == index }

                                    // IGNORA imágenes fantasma (menores a 50px de alto) mientras cargan
                                    if (item != null && item.size > 50) {
                                        ultimaPaginaConocida = index
                                        delay(500)
                                        UserManager.guardarProgreso(manga.titulo, nombreLimpio, index, esColor)

                                        val ultimaVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                                        if (ultimaVisible >= paginas.size - 1) {
                                            UserManager.marcarCapituloComoLeido(manga.titulo, nombreLimpio, esColor)
                                        }
                                    }
                                }
                        }

                        // SALVAVIDAS: Guarda exactamente por dónde ibas justo al darle al botón atrás
                        DisposableEffect(Unit) {
                            onDispose {
                                UserManager.guardarProgreso(manga.titulo, nombreLimpio, ultimaPaginaConocida, esColor)
                            }
                        }

                        var scale by remember { mutableFloatStateOf(1f) }
                        var offsetX by remember { mutableFloatStateOf(0f) }

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onTap = { uiVisible = !uiVisible },
                                        onDoubleTap = { scale = 1f; offsetX = 0f }
                                    )
                                }
                                .pointerInput(Unit) {
                                    awaitEachGesture {
                                        awaitFirstDown(requireUnconsumed = false)
                                        do {
                                            val event = awaitPointerEvent()
                                            val zoom = event.calculateZoom()
                                            val pan = event.calculatePan()
                                            val centroid = event.calculateCentroid(useCurrent = false)

                                            val isMultiTouch = event.changes.size > 1

                                            if (scale > 1f || isMultiTouch) {
                                                val oldScale = scale
                                                scale = (scale * zoom).coerceIn(1f, 4f)
                                                val scaleFactor = scale / oldScale

                                                if (scaleFactor != 1f) {
                                                    val cx = centroid.x - (widthPx / 2f)
                                                    val cy = centroid.y - (heightPx / 2f)

                                                    val shiftX = cx * scaleFactor - cx
                                                    val shiftY = cy * scaleFactor - cy

                                                    offsetX -= shiftX
                                                    listState.dispatchRawDelta(shiftY / scale)
                                                }

                                                offsetX += pan.x
                                                val maxOffsetX = (widthPx * (scale - 1f)) / 2f
                                                offsetX = offsetX.coerceIn(-maxOffsetX, maxOffsetX)

                                                if (pan.y != 0f) {
                                                    listState.dispatchRawDelta(-pan.y / scale)
                                                }

                                                event.changes.forEach { if (it.positionChanged()) it.consume() }
                                            }
                                        } while (event.changes.any { it.pressed })

                                        if (scale <= 1f) {
                                            scale = 1f; offsetX = 0f
                                        }
                                    }
                                }
                        ) {
                            LazyColumn(
                                state = listState,
                                userScrollEnabled = scale <= 1f,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer(
                                        scaleX = scale,
                                        scaleY = scale,
                                        translationX = offsetX,
                                        transformOrigin = TransformOrigin.Center
                                    ),
                                contentPadding = PaddingValues(top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding() + 40.dp)
                            ) {
                                items(paginas, key = { it }) { rutaPagina ->
                                    val request = remember(rutaPagina, context) {
                                        ImageRequest.Builder(context)
                                            .data(if (rutaPagina.startsWith("http")) rutaPagina else rutaPagina.toPath())
                                            .size(Size.ORIGINAL)
                                            .scale(Scale.FIT)
                                            .memoryCachePolicy(CachePolicy.ENABLED)
                                            .diskCachePolicy(CachePolicy.ENABLED)
                                            .crossfade(true)
                                            .build()
                                    }

                                    AsyncImage(
                                        model = request,
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxWidth(), // <- Sin forzar tamaños, sin huecos
                                        contentScale = ContentScale.FillWidth,
                                        filterQuality = FilterQuality.High
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TopBarLector(titulo: String, esColor: Boolean, onBack: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás", tint = Color.White) }
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(titulo.replace(".zip", "").replace(".cbz", ""), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (esColor) Text("Modo Color 🎨", color = Color(0xFFE50914), fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun BottomBarMejorada(hayAnterior: Boolean, haySiguiente: Boolean, onAnterior: () -> Unit, onSiguiente: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp, horizontal = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Button(onClick = onAnterior, enabled = hayAnterior, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF333333), disabledContainerColor = Color(0xFF222222)), shape = RoundedCornerShape(8.dp), contentPadding = PaddingValues(horizontal = 12.dp)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp)); Spacer(modifier = Modifier.width(8.dp)); Text("Anterior", fontSize = 12.sp)
        }
        Button(onClick = onSiguiente, enabled = haySiguiente, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914), disabledContainerColor = Color(0xFF222222)), shape = RoundedCornerShape(8.dp), contentPadding = PaddingValues(horizontal = 12.dp)) {
            Text("Siguiente", fontSize = 12.sp, fontWeight = FontWeight.Bold); Spacer(modifier = Modifier.width(8.dp)); Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
        }
    }
}