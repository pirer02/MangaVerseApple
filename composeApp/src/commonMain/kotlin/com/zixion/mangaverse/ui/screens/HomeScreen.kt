package com.zixion.mangaverse.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.zixion.mangaverse.models.Manga
import com.zixion.mangaverse.network.MangaService
import com.zixion.mangaverse.network.UserManager
import com.zixion.mangaverse.ui.components.MangaCard
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.saveable.rememberSaveable
import com.zixion.mangaverse.network.AuthManager
import mangaverse.composeapp.generated.resources.Res
import mangaverse.composeapp.generated.resources.google
import org.jetbrains.compose.resources.painterResource
import mangaverse.composeapp.generated.resources.mangaverse
import org.jetbrains.compose.resources.painterResource

enum class Seccion { INICIO, BIBLIOTECA, EXPLORAR, PERFIL }
enum class ModoLectura { NORMAL, COLOR, PREGUNTAR }

data class CategoriaManga(val titulo: String, val mangas: List<Manga>)
data class ContinuarData(val manga: Manga, val capitulo: String, val modo: ModoLectura)

class HomeScreen : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val servicio = remember { MangaService() }
        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
        val scope = rememberCoroutineScope()
        val snackbarHostState = remember { SnackbarHostState() }

        val authManager = remember { AuthManager() }
        var usuarioActual by remember { mutableStateOf(authManager.obtenerUsuarioActual()) }
        var mostrarDialogoCerrarSesion by remember { mutableStateOf(false) }
        var mostrarDialogoBorrarCuenta by remember { mutableStateOf(false) }

        var seccionActual by rememberSaveable { mutableStateOf(Seccion.INICIO) }
        var listaCompleta by remember { mutableStateOf<List<Manga>>(emptyList()) }
        var mangasContinuar by remember { mutableStateOf<List<ContinuarData>>(emptyList()) }
        var categoriasDinamicas by remember { mutableStateOf<List<CategoriaManga>>(emptyList()) }

        var cargando by remember { mutableStateOf(true) }
        var mostrarDialogoBorrar by remember { mutableStateOf(false) }
        var mostrarDialogoActualizar by remember { mutableStateOf(false) }
        var mangaABorrarProgreso by remember { mutableStateOf<Manga?>(null) }
        var dialogContinuarColor by remember { mutableStateOf<ContinuarData?>(null) }

        val estadoGlobal = UserManager.estadoReactivo

        val inicioScrollState = rememberLazyListState()
        val biblioScrollState = rememberLazyListState()
        val explorarGridState = rememberLazyGridState()

        var explorarQuery by rememberSaveable { mutableStateOf("") }
        var explorarFiltroGenero by rememberSaveable { mutableStateOf("Todos") }
        var explorarFiltroEstado by rememberSaveable { mutableStateOf("Todos") }
        var explorarFiltroColor by rememberSaveable { mutableStateOf(false) } // NUEVO


        LaunchedEffect(Unit) {
            cargarDatos(servicio) { lista, continuar, categorias ->
                listaCompleta = lista
                mangasContinuar = continuar
                categoriasDinamicas = categorias
                cargando = false
            }
        }

        fun toggleBiblio(manga: Manga) {
            val agregado = UserManager.toggleBiblioteca(manga.titulo)
            scope.launch { snackbarHostState.showSnackbar(if (agregado) "Añadido a biblioteca" else "Eliminado de biblioteca") }
        }

        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet(
                    modifier = Modifier.width(280.dp),
                    drawerContainerColor = Color.Transparent,
                    drawerContentColor = Color.White
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize().background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF800000),
                                    Color(0xFF400000),
                                    Color.Black
                                )
                            )
                        )
                    ) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            Spacer(Modifier.height(40.dp))

                            Image(
                                painter = painterResource(Res.drawable.mangaverse),
                                contentDescription = "Logo MangaVerse",
                                modifier = Modifier
                                    .size(100.dp)
                                    .align(Alignment.CenterHorizontally)
                            )
                            Spacer(Modifier.height(16.dp))


                            Text(
                                "MangaVerse",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 20.dp)
                                    .align(Alignment.CenterHorizontally), // Lo centramos para que quede bien con el logo
                                color = Color.White
                            )

                            // RESTAURADO: Mostrar correo en el menú lateral si hay usuario
                            if (usuarioActual != null) {
                                Column(
                                    modifier = Modifier.padding(
                                        horizontal = 20.dp,
                                        vertical = 8.dp
                                    )
                                ) {
                                    Text(
                                        "Sesión iniciada como:",
                                        fontSize = 11.sp,
                                        color = Color.White.copy(alpha = 0.6f)
                                    )
                                    Text(
                                        usuarioActual!!.email,
                                        fontSize = 14.sp,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                }
                                Spacer(Modifier.height(12.dp))
                            } else {
                                Spacer(Modifier.height(24.dp))
                            }

                            HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
                            Spacer(Modifier.height(16.dp))

                            // Navegación Simplificada
                            NavigationDrawerItem(
                                label = { Text("Inicio") },
                                selected = seccionActual == Seccion.INICIO,
                                onClick = {
                                    seccionActual =
                                        Seccion.INICIO; scope.launch { drawerState.close() }
                                },
                                icon = { Icon(Icons.Default.Home, null) },
                                colors = NavigationDrawerItemDefaults.colors(
                                    selectedContainerColor = Color.White.copy(alpha = 0.2f),
                                    selectedTextColor = Color.White,
                                    unselectedTextColor = Color.LightGray,
                                    unselectedIconColor = Color.LightGray,
                                    selectedIconColor = Color.White
                                ),
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                            NavigationDrawerItem(
                                label = { Text("Biblioteca") },
                                selected = seccionActual == Seccion.BIBLIOTECA,
                                onClick = {
                                    seccionActual =
                                        Seccion.BIBLIOTECA; scope.launch { drawerState.close() }
                                },
                                icon = { Icon(Icons.Default.List, null) },
                                colors = NavigationDrawerItemDefaults.colors(
                                    selectedContainerColor = Color.White.copy(alpha = 0.2f),
                                    selectedTextColor = Color.White,
                                    unselectedTextColor = Color.LightGray,
                                    unselectedIconColor = Color.LightGray,
                                    selectedIconColor = Color.White
                                ),
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                            NavigationDrawerItem(
                                label = { Text("Explorar") },
                                selected = seccionActual == Seccion.EXPLORAR,
                                onClick = {
                                    seccionActual =
                                        Seccion.EXPLORAR; scope.launch { drawerState.close() }
                                },
                                icon = { Icon(Icons.Default.Search, null) },
                                colors = NavigationDrawerItemDefaults.colors(
                                    selectedContainerColor = Color.White.copy(alpha = 0.2f),
                                    selectedTextColor = Color.White,
                                    unselectedTextColor = Color.LightGray,
                                    unselectedIconColor = Color.LightGray,
                                    selectedIconColor = Color.White
                                ),
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )

                            Spacer(Modifier.height(8.dp))
                            HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
                            Spacer(Modifier.height(8.dp))

                            // Botón de Perfil en el panel
                            NavigationDrawerItem(
                                label = { Text("Mi Perfil") },
                                selected = seccionActual == Seccion.PERFIL,
                                onClick = {
                                    seccionActual =
                                        Seccion.PERFIL; scope.launch { drawerState.close() }
                                },
                                icon = { Icon(Icons.Default.Person, null) },
                                colors = NavigationDrawerItemDefaults.colors(
                                    selectedContainerColor = Color.White.copy(alpha = 0.2f),
                                    selectedTextColor = Color.White,
                                    unselectedTextColor = Color.LightGray,
                                    unselectedIconColor = Color.LightGray,
                                    selectedIconColor = Color.White
                                ),
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                        }
                    }
                }
            }
        ) {
            Scaffold(
                containerColor = Color(0xFF141414),
                topBar = {
                    Box(
                        modifier = Modifier.fillMaxWidth().background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF800000),
                                    Color.Black
                                )
                            )
                        )
                    ) {
                        TopAppBar(
                            title = {
                                Text(
                                    text = when (seccionActual) {
                                        Seccion.INICIO -> "Inicio"
                                        Seccion.BIBLIOTECA -> "Mi Biblioteca"
                                        Seccion.EXPLORAR -> "Explorar"
                                        Seccion.PERFIL -> "Mi Perfil"
                                    }, color = Color.White, fontWeight = FontWeight.Bold
                                )
                            },
                            navigationIcon = {
                                IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                    Icon(
                                        Icons.Default.Menu,
                                        "Menú",
                                        tint = Color.White
                                    )
                                }
                            },
                            actions = {
                                if (usuarioActual == null && seccionActual != Seccion.PERFIL) {
                                    TextButton(
                                        onClick = {
                                            scope.launch {
                                                cargando = true
                                                authManager.cerrarSesion() // Cierre preventivo
                                                UserManager.borrarTodoDeFabrica() // Limpieza segura

                                                val usuario = authManager.iniciarSesionGoogle()
                                                if (usuario != null) {
                                                    usuarioActual = usuario
                                                    UserManager.sincronizarDesdeNube()
                                                    cargarDatos(servicio) { lista, continuar, categorias ->
                                                        listaCompleta = lista; mangasContinuar = continuar; categoriasDinamicas = categorias
                                                    }
                                                }
                                                cargando = false
                                            }
                                        },
                                        contentPadding = PaddingValues(
                                            horizontal = 8.dp,
                                            vertical = 4.dp
                                        )
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                "Vincular con",
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Surface(
                                                shape = CircleShape,
                                                color = Color.White,
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    painter = painterResource(Res.drawable.google),
                                                    contentDescription = "Google",
                                                    tint = Color.Unspecified,
                                                    modifier = Modifier.padding(5.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                        )
                    }
                },
                snackbarHost = { SnackbarHost(snackbarHostState) }
            ) { padding ->
                Box(modifier = Modifier.padding(padding).fillMaxSize()) {
                    if (cargando) {
                        CircularProgressIndicator(
                            color = Color(0xFFE50914),
                            modifier = Modifier.align(Alignment.Center)
                        )
                    } else {
                        key(estadoGlobal) {
                            when (seccionActual) {
                                Seccion.INICIO -> VistaInicio(
                                    state = inicioScrollState,
                                    categorias = categoriasDinamicas,
                                    continuar = mangasContinuar,
                                    onToggle = { m -> toggleBiblio(m) },
                                    onClick = { m -> navigator.push(CapitulosScreen(m)) },
                                    onContinueClick = { data ->
                                        scope.launch {
                                            cargando = true
                                            when (data.modo) {
                                                ModoLectura.NORMAL -> {
                                                    val caps = servicio.obtenerCapitulos(
                                                        data.manga.titulo,
                                                        false
                                                    )
                                                    cargando = false; navigator.push(
                                                        LectorCapituloScreen(
                                                            data.manga,
                                                            data.capitulo,
                                                            caps,
                                                            false
                                                        )
                                                    )
                                                }

                                                ModoLectura.COLOR -> {
                                                    val caps = servicio.obtenerCapitulos(
                                                        data.manga.titulo,
                                                        true
                                                    )
                                                    cargando = false; navigator.push(
                                                        LectorCapituloScreen(
                                                            data.manga,
                                                            data.capitulo,
                                                            caps,
                                                            true
                                                        )
                                                    )
                                                }

                                                ModoLectura.PREGUNTAR -> {
                                                    cargando = false; dialogContinuarColor = data
                                                }
                                            }
                                        }
                                    },
                                    onDeleteProgress = { mangaABorrarProgreso = it }
                                )

                                Seccion.BIBLIOTECA -> VistaBiblioteca(
                                    state = biblioScrollState,
                                    lista = listaCompleta,
                                    onClick = { m -> navigator.push(CapitulosScreen(m)) },
                                    onToggle = { m -> toggleBiblio(m) }
                                )

                                Seccion.EXPLORAR -> VistaExplorar(
                                    state = explorarGridState,
                                    lista = listaCompleta,
                                    query = explorarQuery,
                                    onQueryChange = {
                                        explorarQuery = it
                                        scope.launch { explorarGridState.scrollToItem(0) }
                                    },
                                    filtroGenero = explorarFiltroGenero,
                                    onFiltroGeneroChange = {
                                        explorarFiltroGenero = it
                                        scope.launch { explorarGridState.scrollToItem(0) }
                                    },
                                    filtroEstado = explorarFiltroEstado,
                                    onFiltroEstadoChange = {
                                        explorarFiltroEstado = it
                                        scope.launch { explorarGridState.scrollToItem(0) }
                                    },
                                    filtroColor = explorarFiltroColor,
                                    onFiltroColorChange = {
                                        explorarFiltroColor = it
                                        scope.launch { explorarGridState.scrollToItem(0) }
                                    },
                                    servicio = servicio,
                                    onToggle = { m -> toggleBiblio(m) },
                                    onClick = { m -> navigator.push(CapitulosScreen(m)) }
                                )

                                Seccion.PERFIL -> VistaPerfil(
                                    emailUsuario = usuarioActual?.email,
                                    onLogin = {
                                        scope.launch {
                                            cargando = true

                                            // 1. Limpiamos datos locales previos
                                            UserManager.borrarTodoDeFabrica()

                                            val usuario = authManager.iniciarSesionGoogle()
                                            if (usuario != null) {
                                                usuarioActual = usuario
                                                // 2. Forzamos la descarga desde la nube
                                                UserManager.sincronizarDesdeNube()
                                                cargarDatos(servicio) { lista, continuar, categorias ->
                                                    listaCompleta = lista
                                                    mangasContinuar = continuar
                                                    categoriasDinamicas = categorias
                                                }
                                            }
                                            cargando = false
                                        }
                                    },
                                    onChangeAccount = {
                                        scope.launch {
                                            cargando = true
                                            authManager.cerrarSesion()
                                            UserManager.borrarTodoDeFabrica()

                                            val usuario = authManager.iniciarSesionGoogle()
                                            if (usuario != null) {
                                                usuarioActual = usuario
                                                UserManager.sincronizarDesdeNube()
                                                cargarDatos(servicio) { lista, continuar, categorias ->
                                                    listaCompleta = lista; mangasContinuar = continuar; categoriasDinamicas = categorias
                                                }

                                                seccionActual = Seccion.INICIO
                                            } else {
                                                usuarioActual = null // Si cancela el login, se queda deslogueado correctamente
                                            }
                                            cargando = false
                                        }
                                    },
                                    onLogout = { mostrarDialogoCerrarSesion = true },
                                    onDeleteAccount = { mostrarDialogoBorrarCuenta = true },
                                    onManualUpdate = { mostrarDialogoActualizar = true },
                                    onClearLocalData = { mostrarDialogoBorrar = true }
                                )
                            }
                        }
                    }
                }
            }
        }

        if (dialogContinuarColor != null) {
            val data = dialogContinuarColor!!
            AlertDialog(
                onDismissRequest = { dialogContinuarColor = null },
                title = {
                    Text(
                        "¿Cómo deseas leerlo?",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        "Estás al mismo nivel en ambas versiones. Elige una:",
                        color = Color.LightGray
                    )
                },
                containerColor = Color(0xFF1E1E1E),
                confirmButton = {
                    Button(
                        onClick = {
                            dialogContinuarColor = null
                            scope.launch {
                                cargando = true
                                val capsColor = servicio.obtenerCapitulos(data.manga.titulo, true)
                                val nombreBuscado =
                                    data.capitulo.replace(".cbz", "").replace(".zip", "")
                                val capElegido =
                                    capsColor.find { it.contains(nombreBuscado) } ?: data.capitulo
                                cargando = false
                                navigator.push(
                                    LectorCapituloScreen(
                                        data.manga,
                                        capElegido,
                                        capsColor,
                                        true
                                    )
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914))
                    ) { Text("A Color \uD83C\uDFA8") }
                },
                dismissButton = {
                    Button(
                        onClick = {
                            dialogContinuarColor = null
                            scope.launch {
                                cargando = true
                                val capsOriginal =
                                    servicio.obtenerCapitulos(data.manga.titulo, false)
                                cargando = false
                                navigator.push(
                                    LectorCapituloScreen(
                                        data.manga,
                                        data.capitulo,
                                        capsOriginal,
                                        false
                                    )
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
                    ) { Text("Original \uD83D\uDCC4") }
                }
            )
        }

        if (mangaABorrarProgreso != null) {
            AlertDialog(
                onDismissRequest = { mangaABorrarProgreso = null },
                title = {
                    Text(
                        "¿Olvidar manga?",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        "Se eliminará de tu lista de 'Continuar Leyendo' y se marcarán todos sus capítulos como no leídos.",
                        color = Color.LightGray
                    )
                },
                containerColor = Color(0xFF1E1E1E),
                confirmButton = {
                    Button(
                        onClick = {
                            val manga = mangaABorrarProgreso!!
                            mangaABorrarProgreso = null
                            scope.launch {
                                cargando = true
                                UserManager.borrarProgresoManga(manga.titulo)
                                cargarDatos(servicio) { lista, continuar, categorias ->
                                    listaCompleta = lista; mangasContinuar =
                                    continuar; categoriasDinamicas = categorias; cargando = false
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914))
                    ) { Text("Borrar progreso") }
                },
                dismissButton = {
                    TextButton(onClick = {
                        mangaABorrarProgreso = null
                    }) { Text("Cancelar", color = Color.White) }
                }
            )
        }

        if (mostrarDialogoActualizar) {
            AlertDialog(
                onDismissRequest = { mostrarDialogoActualizar = false },
                title = {
                    Text(
                        "¿Forzar actualización?",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        "Se buscará nuevo contenido en el servidor inmediatamente sin esperar el tiempo de caché. Esto puede tardar unos segundos.",
                        color = Color.LightGray
                    )
                },
                containerColor = Color(0xFF1E1E1E),
                confirmButton = {
                    Button(
                        onClick = {
                            mostrarDialogoActualizar = false
                            cargando = true
                            scope.launch {
                                UserManager.forzarExpiracionCache()
                                cargarDatos(servicio) { lista, continuar, categorias ->
                                    listaCompleta = lista
                                    mangasContinuar = continuar
                                    categoriasDinamicas = categorias
                                    cargando = false
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4DA8DA))
                    ) { Text("Actualizar", color = Color.Black, fontWeight = FontWeight.Bold) }
                },
                dismissButton = {
                    TextButton(onClick = { mostrarDialogoActualizar = false }) {
                        Text(
                            "Cancelar",
                            color = Color.White
                        )
                    }
                }
            )
        }

        if (mostrarDialogoBorrar) {
            AlertDialog(
                onDismissRequest = { mostrarDialogoBorrar = false },
                title = { Text("¿Restablecer Aplicación?", color = Color.White) },
                text = {
                    Text(
                        "Se borrarán todos los mangas guardados y tu historial de lectura. Esta acción no se deshace.",
                        color = Color.LightGray
                    )
                },
                containerColor = Color(0xFF1E1E1E),
                confirmButton = {
                    Button(
                        onClick = {
                            UserManager.borrarTodoDeFabrica()
                            mostrarDialogoBorrar = false
                            cargando = true
                            scope.launch {
                                cargarDatos(servicio) { lista, continuar, categorias ->
                                    listaCompleta = lista; mangasContinuar =
                                    continuar; categoriasDinamicas = categorias; cargando = false
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914))
                    ) { Text("Borrar Todo") }
                },
                dismissButton = {
                    TextButton(onClick = {
                        mostrarDialogoBorrar = false
                    }) { Text("Cancelar", color = Color.White) }
                }
            )
        }

        if (mostrarDialogoCerrarSesion) {
            AlertDialog(
                onDismissRequest = { mostrarDialogoCerrarSesion = false },
                title = { Text("¿Cerrar Sesión?", color = Color.White) },
                text = {
                    Text(
                        "Se cerrará tu sesión y se borrarán los datos locales por seguridad, pero tu biblioteca seguirá guardada en la nube para cuando vuelvas.",
                        color = Color.LightGray
                    )
                },
                containerColor = Color(0xFF1E1E1E),
                confirmButton = {
                    Button(
                        onClick = {
                            mostrarDialogoCerrarSesion = false
                            authManager.cerrarSesion()
                            usuarioActual = null

                            UserManager.borrarTodoDeFabrica()
                            seccionActual = Seccion.INICIO
                            cargando = true
                            scope.launch {
                                cargarDatos(servicio) { lista, continuar, categorias ->
                                    listaCompleta = lista; mangasContinuar =
                                    continuar; categoriasDinamicas = categorias; cargando = false
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914))
                    ) { Text("Cerrar Sesión") }
                },
                dismissButton = {
                    TextButton(onClick = {
                        mostrarDialogoCerrarSesion = false
                    }) { Text("Cancelar", color = Color.White) }
                }
            )
        }

        if (mostrarDialogoBorrarCuenta) {
            AlertDialog(
                onDismissRequest = { mostrarDialogoBorrarCuenta = false },
                title = {
                    Text(
                        "¿Eliminar cuenta permanentemente?",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        "Esta acción no se puede deshacer. Se borrarán todos tus datos guardados en la nube, incluyendo tu biblioteca y progreso de lectura de forma definitiva.",
                        color = Color.LightGray
                    )
                },
                containerColor = Color(0xFF1E1E1E),
                confirmButton = {
                    Button(
                        onClick = {
                            mostrarDialogoBorrarCuenta = false
                            cargando = true
                            scope.launch {
                                val exito = authManager.eliminarCuenta()

                                if (exito) {
                                    UserManager.borrarTodoDeFabrica()
                                    usuarioActual = null
                                    seccionActual = Seccion.INICIO

                                    cargarDatos(servicio) { lista, continuar, categorias ->
                                        listaCompleta = lista; mangasContinuar =
                                        continuar; categoriasDinamicas = categorias; cargando =
                                        false
                                    }
                                } else {
                                    cargando = false
                                    snackbarHostState.showSnackbar("Error al eliminar la cuenta. Es posible que necesites re-autenticarte.")
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914))
                    ) {
                        Text("Confirmar Eliminación", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { mostrarDialogoBorrarCuenta = false }) {
                        Text("Cancelar", color = Color.White)
                    }
                }
            )
        }
    }

    private suspend fun cargarDatos(
        servicio: MangaService,
        onResult: (List<Manga>, List<ContinuarData>, List<CategoriaManga>) -> Unit
    ) = coroutineScope {
        UserManager.cargar()

        // 2. NUEVO: Si hay un usuario, intentamos traer los datos más frescos de la nube
        val authManager = AuthManager()
        if (authManager.obtenerUsuarioActual() != null) {
            try {
                // Esto sobrescribirá los datos locales si hay algo más nuevo en la nube
                UserManager.sincronizarDesdeNube()
            } catch (e: Exception) {
                e.printStackTrace() // Si falla (ej. sin internet), seguimos con lo local
            }
        }

        val cacheEstabaExpirada = UserManager.isCacheExpired()

        val todosBasicos = servicio.obtenerMangas()
        val todosCompletos = todosBasicos.map { async { servicio.obtenerInfoManga(it) } }.awaitAll()

        val listaContinuarTemp = mutableListOf<ContinuarData>()
        todosCompletos.forEach { manga ->
            val ultimoNormal = UserManager.getUltimoCapitulo(manga.titulo, false)
            val ultimoColor = UserManager.getUltimoCapitulo(manga.titulo, true)

            if (ultimoNormal != null || ultimoColor != null) {
                try {
                    val capsNormal = servicio.obtenerCapitulos(manga.titulo, false)
                    val capsColor = try {
                        servicio.obtenerCapitulos(manga.titulo, true)
                    } catch (e: Exception) {
                        emptyList()
                    }

                    var scoreNormal = -1
                    var nextCapNormal = ""

                    if (ultimoNormal != null) {
                        val idx = capsNormal.indexOfFirst {
                            it.replace(".cbz", "").replace(".zip", "") == ultimoNormal
                        }
                        if (idx != -1) {
                            val leido =
                                UserManager.isCapituloLeido(manga.titulo, ultimoNormal, false)

                            if (leido && idx >= capsNormal.size - 1) {
                                // No hacemos nada
                            } else {
                                scoreNormal = (idx * 2) + if (leido) 1 else 0
                                val nextNormalIdx =
                                    if (leido && idx < capsNormal.size - 1) idx + 1 else idx
                                nextCapNormal = capsNormal[nextNormalIdx]
                            }
                        }
                    }

                    var scoreColor = -1
                    var nextCapColor = ""
                    var colorTieneSiguiente = false
                    var colorSePasaANormal = false

                    if (ultimoColor != null) {
                        val idxMasterDeColor = capsNormal.indexOfFirst {
                            it.replace(".cbz", "").replace(".zip", "") == ultimoColor
                        }

                        if (idxMasterDeColor != -1) {
                            val leido = UserManager.isCapituloLeido(manga.titulo, ultimoColor, true)
                            val idxInColorList = capsColor.indexOfFirst {
                                it.replace(".cbz", "").replace(".zip", "") == ultimoColor
                            }

                            val noHayMasColor =
                                idxInColorList == -1 || idxInColorList >= capsColor.size - 1
                            val noHayMasNormal = idxMasterDeColor >= capsNormal.size - 1

                            if (leido && noHayMasColor && noHayMasNormal) {
                                // Fin del manga
                            } else {
                                scoreColor = (idxMasterDeColor * 2) + if (leido) 1 else 0

                                if (idxInColorList != -1) {
                                    if (leido) {
                                        if (idxInColorList < capsColor.size - 1) {
                                            nextCapColor = capsColor[idxInColorList + 1]
                                            colorTieneSiguiente = true
                                        } else {
                                            if (idxMasterDeColor < capsNormal.size - 1) {
                                                colorSePasaANormal = true
                                                nextCapColor = capsNormal[idxMasterDeColor + 1]
                                            } else {
                                                nextCapColor = capsNormal[idxMasterDeColor]
                                            }
                                        }
                                    } else {
                                        nextCapColor = capsColor[idxInColorList]
                                        colorTieneSiguiente = true
                                    }
                                }
                            }
                        }
                    }

                    if (scoreNormal != -1 || scoreColor != -1) {
                        if (scoreNormal > scoreColor) {
                            listaContinuarTemp.add(
                                ContinuarData(
                                    manga,
                                    nextCapNormal,
                                    ModoLectura.NORMAL
                                )
                            )
                        } else if (scoreColor > scoreNormal) {
                            if (colorSePasaANormal) {
                                listaContinuarTemp.add(
                                    ContinuarData(
                                        manga,
                                        nextCapColor,
                                        ModoLectura.NORMAL
                                    )
                                )
                            } else {
                                listaContinuarTemp.add(
                                    ContinuarData(
                                        manga,
                                        nextCapColor,
                                        ModoLectura.COLOR
                                    )
                                )
                            }
                        } else {
                            if (colorSePasaANormal) {
                                listaContinuarTemp.add(
                                    ContinuarData(
                                        manga,
                                        nextCapColor,
                                        ModoLectura.NORMAL
                                    )
                                )
                            } else if (colorTieneSiguiente) {
                                listaContinuarTemp.add(
                                    ContinuarData(
                                        manga,
                                        nextCapNormal,
                                        ModoLectura.PREGUNTAR
                                    )
                                )
                            } else {
                                listaContinuarTemp.add(
                                    ContinuarData(
                                        manga,
                                        nextCapNormal,
                                        ModoLectura.NORMAL
                                    )
                                )
                            }
                        }
                    }


                } catch (e: Exception) {
                }
            }
        }

        val generosPermitidos = listOf(
            "Shonen",
            "Accion",
            "Aventura",
            "Comedia",
            "Drama",
            "Seinen",
            "Romance",
            "Isekai",
            "Deporte",
            "Chanbara"
        )

        val generosValidos = generosPermitidos.filter { permitido ->
            todosCompletos.any { manga ->
                manga.generos.any { g -> g.equals(permitido, ignoreCase = true) }
            }
        }

        val categoriasGeneradas = mutableListOf<CategoriaManga>()

        if (generosValidos.isNotEmpty()) {
            generosValidos.shuffled().take(minOf(10, generosValidos.size)).forEach { genero ->

                val mangasDelGenero = todosCompletos
                    .filter { it.generos.any { g -> g.equals(genero, ignoreCase = true) } }
                    .shuffled()
                    .take(24)

                if (mangasDelGenero.isNotEmpty()) {
                    categoriasGeneradas.add(CategoriaManga("Lo mejor en $genero", mangasDelGenero))
                }
            }
        } else {
            categoriasGeneradas.add(
                CategoriaManga(
                    "Descubrimientos Aleatorios",
                    todosCompletos.shuffled().take(15)
                )
            )
        }

        if (cacheEstabaExpirada) {
            UserManager.actualizarTimestampCache()
        }

        onResult(todosCompletos, listaContinuarTemp, categoriasGeneradas)
    }
}

@Composable
fun VistaPerfil(
    emailUsuario: String?,
    onLogin: () -> Unit,
    onChangeAccount: () -> Unit,
    onLogout: () -> Unit,
    onDeleteAccount: () -> Unit,
    onManualUpdate: () -> Unit,
    onClearLocalData: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 24.dp, bottom = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            // --- CABECERA DE PERFIL ---
            Icon(
                imageVector = Icons.Default.AccountCircle,
                contentDescription = "Avatar de Perfil",
                tint = Color.LightGray,
                modifier = Modifier.size(100.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (emailUsuario != null) {
                Text(
                    text = emailUsuario,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Cuenta vinculada",
                    color = Color(0xFF4DA8DA),
                    fontSize = 14.sp
                )
            } else {
                Text(
                    text = "No has iniciado sesión",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onLogin,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.google),
                            contentDescription = "Google",
                            tint = Color.Unspecified,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            "Vincular con Google",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            HorizontalDivider(color = Color.DarkGray)
            Spacer(modifier = Modifier.height(16.dp))
        }

        // --- NUEVA SECCIÓN: PREFERENCIAS ---
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "PREFERENCIAS",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp, start = 8.dp)
                )

                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = null,
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                "Notificaciones de capítulos",
                                color = Color.White,
                                fontSize = 16.sp
                            )
                        }

                        // El Switch que controla UserManager
                        Switch(
                            checked = UserManager.areNotificacionesActivas(),
                            onCheckedChange = { UserManager.setNotificaciones(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFFE50914), // Rojo MangaVerse
                                checkedTrackColor = Color(0xFFE50914).copy(alpha = 0.5f),
                                uncheckedThumbColor = Color.Gray,
                                uncheckedTrackColor = Color.DarkGray
                            )
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // --- SECCIÓN: GESTIÓN DE CUENTA (Solo si hay sesión) ---
        if (emailUsuario != null) {
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "GESTIÓN DE CUENTA",
                        color = Color.Gray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp, start = 8.dp)
                    )

                    PerfilItemCard(
                        texto = "Cambiar de cuenta",
                        icono = Icons.Default.Refresh,
                        onClick = onChangeAccount
                    )
                    PerfilItemCard(
                        texto = "Cerrar Sesión",
                        icono = Icons.Default.ExitToApp,
                        onClick = onLogout
                    )
                    PerfilItemCard(
                        texto = "Eliminar Cuenta Permanentemente",
                        icono = Icons.Default.DeleteForever,
                        colorTexto = Color(0xFFFF6B6B),
                        onClick = onDeleteAccount
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // --- SECCIÓN: SISTEMA Y DATOS ---
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "SISTEMA Y DATOS",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp, start = 8.dp)
                )

                PerfilItemCard(
                    texto = "Forzar Actualización Manual",
                    icono = Icons.Default.CloudSync,
                    colorTexto = Color(0xFF4DA8DA),
                    onClick = onManualUpdate
                )
                PerfilItemCard(
                    texto = "Borrar Todos los Datos Locales",
                    icono = Icons.Default.Delete,
                    colorTexto = Color(0xFFFF6B6B),
                    onClick = onClearLocalData
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerfilItemCard(
    texto: String,
    icono: androidx.compose.ui.graphics.vector.ImageVector,
    colorTexto: Color = Color.White,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icono,
                contentDescription = null,
                tint = colorTexto,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(text = texto, color = colorTexto, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun VistaInicio(
    state: LazyListState,
    categorias: List<CategoriaManga>,
    continuar: List<ContinuarData>,
    onToggle: (Manga) -> Unit,
    onClick: (Manga) -> Unit,
    onContinueClick: (ContinuarData) -> Unit,
    onDeleteProgress: (Manga) -> Unit
) {
    LazyColumn(
        state = state,
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        contentPadding = PaddingValues(bottom = 30.dp)
    ) {
        if (continuar.isNotEmpty()) {
            item {
                Text(
                    "Continuar Leyendo",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(16.dp)
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(continuar) { data ->
                        val nombreVisible = data.capitulo.replace(".cbz", "").replace(".zip", "")
                        val suffix = if (data.modo == ModoLectura.COLOR) " (Color)" else ""
                        MangaCard(
                            manga = data.manga,
                            esGrande = true,
                            enBiblioteca = UserManager.enBiblioteca(data.manga.titulo),
                            subtitulo = "Retomar: $nombreVisible$suffix",
                            onToggleLibrary = { onToggle(data.manga) },
                            onClick = { onContinueClick(data) },
                            onDeleteProgress = { onDeleteProgress(data.manga) },
                            onInfoClick = { onClick(data.manga) }
                        )
                    }
                }
            }
        }
        items(categorias) { categoria ->
            FilaGenero(
                categoria.titulo,
                categoria.mangas,
                onToggle,
                onClick
            )
        }
    }
}

@Composable
fun FilaGenero(
    titulo: String,
    lista: List<Manga>,
    onToggle: (Manga) -> Unit,
    onClick: (Manga) -> Unit
) {
    Column {
        Text(
            titulo,
            color = Color(0xFFE5E5E5),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(lista) { manga ->
                MangaCard(
                    manga = manga,
                    enBiblioteca = UserManager.enBiblioteca(manga.titulo),
                    onToggleLibrary = { onToggle(manga) },
                    onClick = { onClick(manga) })
            }
        }
    }
}

@Composable
fun VistaBiblioteca(
    state: LazyListState,
    lista: List<Manga>,
    onClick: (Manga) -> Unit,
    onToggle: (Manga) -> Unit
) {
    val enBiblio = lista.filter { UserManager.enBiblioteca(it.titulo) }

    LazyColumn(
        state = state,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 30.dp)
    ) {
        item {
            Text(
                "Guardados",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(16.dp)
            )
        }
        if (enBiblio.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    contentAlignment = Alignment.Center
                ) { Text("Tu biblioteca está vacía.", color = Color.Gray) }
            }
        } else {
            items(enBiblio.chunked(3)) { fila ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    fila.forEach { m ->
                        MangaCard(
                            manga = m,
                            enBiblioteca = true,
                            onToggleLibrary = { onToggle(m) },
                            onClick = { onClick(m) })
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VistaExplorar(
    state: LazyGridState,
    lista: List<Manga>,
    query: String,
    onQueryChange: (String) -> Unit,
    filtroGenero: String,
    onFiltroGeneroChange: (String) -> Unit,
    filtroEstado: String,
    onFiltroEstadoChange: (String) -> Unit,
    filtroColor: Boolean,
    onFiltroColorChange: (Boolean) -> Unit,
    servicio: MangaService,
    onToggle: (Manga) -> Unit,
    onClick: (Manga) -> Unit
) {
    val opcionesGeneros = listOf(
        "Todos", "Shonen", "Accion", "Aventura", "Comedia", "Drama", "Seinen", "Romance", "Isekai", "Deporte", "Chanbara"
    )
    val opcionesEstados = listOf("Todos", "En Emisión", "Terminado")

    var mangasFiltrados by remember { mutableStateOf(emptyList<Manga>()) }
    var cargandoFiltros by remember { mutableStateOf(false) }

    LaunchedEffect(query, filtroGenero, filtroEstado, filtroColor, lista) {
        cargandoFiltros = true

        // 1. Filtro local (instantáneo)
        val preFiltrados = lista.filter { manga ->
            val coincideTexto = manga.titulo.contains(query, ignoreCase = true)
            val coincideGenero = if (filtroGenero == "Todos") true else {
                manga.generos.any { it.trim().equals(filtroGenero, ignoreCase = true) }
            }
            val coincideEstado = when (filtroEstado) {
                "Terminado" -> manga.estado.contains("finalizado", true) || manga.estado.contains("terminado", true)
                "En Emisión" -> !manga.estado.contains("finalizado", true) && !manga.estado.contains("terminado", true)
                else -> true
            }
            coincideTexto && coincideGenero && coincideEstado
        }

        // 2. Filtro de color (Consulta al servidor)
        if (filtroColor) {
            val finales = preFiltrados.map { m ->
                async {
                    if (servicio.tieneColor(m.titulo)) m else null
                }
            }.awaitAll().filterNotNull()

            mangasFiltrados = finales
        } else {
            mangasFiltrados = preFiltrados
        }

        // Apagamos el icono de carga
        cargandoFiltros = false

        // ¡El scroll automático hacia arriba se ha eliminado de aquí!
    }

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text("Buscar por título...", color = Color.Gray) },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = Color.Gray) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFFE50914),
                unfocusedBorderColor = Color.DarkGray
            ),
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
            singleLine = true
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            var expandidoGenero by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = expandidoGenero,
                onExpandedChange = { expandidoGenero = it },
                modifier = Modifier.weight(1f)
            ) {
                OutlinedTextField(
                    value = filtroGenero,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Género", color = Color.Gray, fontSize = 12.sp) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandidoGenero) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFE50914),
                        unfocusedBorderColor = Color.DarkGray
                    ),
                    modifier = Modifier.menuAnchor(),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp)
                )
                ExposedDropdownMenu(
                    expanded = expandidoGenero,
                    onDismissRequest = { expandidoGenero = false },
                    modifier = Modifier.background(Color(0xFF1E1E1E))
                ) {
                    opcionesGeneros.forEach { seleccion ->
                        DropdownMenuItem(
                            text = { Text(seleccion, color = Color.White) },
                            onClick = { onFiltroGeneroChange(seleccion); expandidoGenero = false }
                        )
                    }
                }
            }

            var expandidoEstado by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = expandidoEstado,
                onExpandedChange = { expandidoEstado = it },
                modifier = Modifier.weight(1f)
            ) {
                OutlinedTextField(
                    value = filtroEstado,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Estado", color = Color.Gray, fontSize = 12.sp) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandidoEstado) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFE50914),
                        unfocusedBorderColor = Color.DarkGray
                    ),
                    modifier = Modifier.menuAnchor(),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp)
                )
                ExposedDropdownMenu(
                    expanded = expandidoEstado,
                    onDismissRequest = { expandidoEstado = false },
                    modifier = Modifier.background(Color(0xFF1E1E1E))
                ) {
                    opcionesEstados.forEach { seleccion ->
                        DropdownMenuItem(
                            text = { Text(seleccion, color = Color.White) },
                            onClick = { onFiltroEstadoChange(seleccion); expandidoEstado = false }
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = filtroColor,
                onCheckedChange = onFiltroColorChange,
                colors = CheckboxDefaults.colors(
                    checkedColor = Color(0xFFE50914),
                    uncheckedColor = Color.Gray
                )
            )
            Text("Solo mangas a color", color = Color.White, fontSize = 14.sp)
        }

        if (cargandoFiltros) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFFE50914))
            }
        } else if (mangasFiltrados.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No se encontraron resultados.", color = Color.Gray)
            }
        } else {
            LazyVerticalGrid(
                state = state,
                columns = GridCells.Adaptive(minSize = 110.dp),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(mangasFiltrados) { manga ->
                    MangaCard(
                        manga = manga,
                        enBiblioteca = UserManager.enBiblioteca(manga.titulo),
                        onToggleLibrary = { onToggle(manga) },
                        onClick = { onClick(manga) }
                    )
                }
            }
        }
    }
}