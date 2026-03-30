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

        // Variables añadidas para conectar el Slider con la lista de imágenes de forma segura
        var paginaActualUI by rememberSaveable { mutableIntStateOf(0) }
        var accionScroll by remember { mutableStateOf<Int?>(null) }

        LaunchedEffect(capituloActual) {
            if (capituloCargado != capituloActual || paginas.isEmpty()) {
                cargando = true
                val nombreLimpio = capituloActual.replace(".cbz", "").replace(".zip", "")

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
                    Box(
                        modifier = Modifier.fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.8f)).statusBarsPadding()
                    ) {
                        // Actualizado para incluir la navegación de capítulos
                        TopBarLector(
                            titulo = capituloActual,
                            esColor = esColor,
                            hayAnterior = hayAnteriorHistoria,
                            haySiguiente = haySiguienteHistoria,
                            onAnterior = {
                                if (hayAnteriorHistoria) capituloActual = listaTodosCapitulos[indexActual - 1]
                            },
                            onSiguiente = {
                                if (haySiguienteHistoria) capituloActual = listaTodosCapitulos[indexActual + 1]
                            },
                            onBack = { navigator?.pop() }
                        )
                    }
                }
            },
            bottomBar = {
                AnimatedVisibility(
                    visible = uiVisible,
                    enter = slideInVertically(initialOffsetY = { it }),
                    exit = slideOutVertically(targetOffsetY = { it })
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.9f)).navigationBarsPadding()
                    ) {
                        // Cambiado a la nueva barra de progreso con el cursor
                        BarraProgresoInferior(
                            paginaActual = paginaActualUI,
                            totalPaginas = paginas.size,
                            onPageChange = { nuevaPagina ->
                                accionScroll = nuevaPagina
                            }
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
                    CircularProgressIndicator(
                        color = Color.Red,
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else if (paginas.isEmpty()) {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(50.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No se pudieron cargar las imágenes", color = Color.Gray)
                    }
                } else {
                    val widthPx = constraints.maxWidth.toFloat()
                    val heightPx = constraints.maxHeight.toFloat()

                    key(manga.titulo, capituloActual) {
                        val nombreLimpio = capituloActual.replace(".cbz", "").replace(".zip", "")
                        val paginaGuardada = remember {
                            UserManager.getPaginaGuardada(
                                manga.titulo,
                                nombreLimpio,
                                esColor
                            )
                        }
                        val estaLeido = remember {
                            UserManager.isCapituloLeido(
                                manga.titulo,
                                nombreLimpio,
                                esColor
                            )
                        }

                        val initialIndex =
                            if (!estaLeido && paginaGuardada in paginas.indices) paginaGuardada else 0

                        val listState =
                            rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
                        var ultimaPaginaConocida by remember { mutableIntStateOf(initialIndex) }

                        // Escucha si el usuario ha tocado el slider para mover la lista a esa página
                        LaunchedEffect(accionScroll) {
                            accionScroll?.let { page ->
                                listState.scrollToItem(page)
                                accionScroll = null
                            }
                        }

                        // MAGIA: Esto obliga al botón atrás a leer siempre la página REAL, pase lo que pase
                        val paginaParaGuardar by rememberUpdatedState(ultimaPaginaConocida)

                        LaunchedEffect(listState) {
                            snapshotFlow { listState.firstVisibleItemIndex }
                                .collectLatest { index ->
                                    paginaActualUI = index // Actualiza la posición del Slider visualmente
                                    ultimaPaginaConocida = index
                                    delay(500)
                                    UserManager.guardarProgreso(
                                        manga.titulo,
                                        nombreLimpio,
                                        index,
                                        esColor
                                    )

                                    val ultimaVisible =
                                        listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index
                                            ?: 0
                                    if (ultimaVisible >= paginas.size - 1) {
                                        UserManager.marcarCapituloComoLeido(
                                            manga.titulo,
                                            nombreLimpio,
                                            esColor
                                        )
                                    }
                                }
                        }

                        // SALVAVIDAS: Usa la página actualizada al salir de la pantalla
                        DisposableEffect(Unit) {
                            onDispose {
                                UserManager.guardarProgreso(
                                    manga.titulo,
                                    nombreLimpio,
                                    paginaParaGuardar,
                                    esColor
                                )
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
                                            val centroid =
                                                event.calculateCentroid(useCurrent = false)

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
                                contentPadding = PaddingValues(top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding())

                            ) {
                                items(paginas, key = { it }) { rutaPagina ->
                                    // ... dentro de tu items(paginas) { rutaPagina -> ...
                                    val request = remember(rutaPagina, context) {
                                        ImageRequest.Builder(context)
                                            // 🔥 EL CAMBIO: Forzamos a que Coil lo entienda como un archivo local válido
                                            .data(if (rutaPagina.startsWith("http")) rutaPagina else "file://$rutaPagina")
                                            .size(Size.ORIGINAL)
                                            .scale(Scale.FIT)
                                            .memoryCachePolicy(CachePolicy.ENABLED)
                                            .diskCachePolicy(CachePolicy.ENABLED)
                                            .crossfade(true)
                                            .build()
                                    }

                                    // MAGIA 2: El Maniquí de Carga. Evita el colapso a 0 sin crear separaciones feas
                                    var isLoaded by remember(rutaPagina) { mutableStateOf(false) }

                                    AsyncImage(
                                        model = request,
                                        contentDescription = null,
                                        onSuccess = { isLoaded = true },
                                        onError = { isLoaded = true },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .then(
                                                if (isLoaded) Modifier else Modifier.aspectRatio(
                                                    0.71f
                                                )
                                            ),
                                        contentScale = ContentScale.FillWidth,
                                        filterQuality = FilterQuality.High
                                    )
                                }
                                item {
                                    val extraHeight = if (scale > 1f) (heightPx * (scale - 1f) / (2f * scale)) else 0f
                                    Spacer(modifier = Modifier.fillMaxWidth().height(with(androidx.compose.ui.platform.LocalDensity.current) { extraHeight.toDp() }))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun TopBarLector(
        titulo: String,
        esColor: Boolean,
        hayAnterior: Boolean,
        haySiguiente: Boolean,
        onAnterior: () -> Unit,
        onSiguiente: () -> Unit,
        onBack: () -> Unit
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Atrás",
                        tint = Color.White
                    )
                }
                Column {
                    Text(
                        titulo.replace(".zip", "").replace(".cbz", ""),
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (esColor) Text(
                        "Modo Color 🎨",
                        color = Color(0xFFE50914),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            // Botones de navegación movidos aquí arriba
            Row {
                Button(
                    onClick = onAnterior,
                    enabled = hayAnterior,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF333333), disabledContainerColor = Color(0xFF222222)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Anterior", fontSize = 11.sp)
                }
                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = onSiguiente,
                    enabled = haySiguiente,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914), disabledContainerColor = Color(0xFF222222)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text("Siguiente", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, null, modifier = Modifier.size(14.dp))
                }
            }
        }
    }

    // Nueva función para el Slider de navegación de páginas
    @Composable
    fun BarraProgresoInferior(paginaActual: Int, totalPaginas: Int, onPageChange: (Int) -> Unit) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Slider(
                value = paginaActual.toFloat(),
                onValueChange = { onPageChange(it.toInt()) },
                valueRange = 0f..(if (totalPaginas > 1) (totalPaginas - 1).toFloat() else 0f),
                colors = SliderDefaults.colors(
                    thumbColor = Color.Red,
                    activeTrackColor = Color.Red,
                    inactiveTrackColor = Color.Gray
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "${if (totalPaginas > 0) paginaActual + 1 else 0} / $totalPaginas",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}