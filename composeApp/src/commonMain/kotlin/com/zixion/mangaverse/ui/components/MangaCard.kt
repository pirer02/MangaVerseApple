package com.zixion.mangaverse.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.zixion.mangaverse.models.Manga

@Composable
fun MangaCard(
    manga: Manga,
    esGrande: Boolean = false,
    enBiblioteca: Boolean = false,
    subtitulo: String? = null,
    onToggleLibrary: () -> Unit,
    onClick: () -> Unit,
    onDeleteProgress: (() -> Unit)? = null,
    onInfoClick: (() -> Unit)? = null // NUEVO: Para ir a la ficha del manga
) {
    val ancho = if (esGrande) 140.dp else 110.dp
    val alto = if (esGrande) 200.dp else 160.dp

    var menuExpandido by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .width(ancho)
            .height(alto)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
    ) {
        AsyncImage(
            model = manga.urlPortada,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f)),
                        startY = 50f
                    )
                )
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(8.dp)
                .fillMaxWidth()
        ) {
            Text(
                text = manga.titulo,
                color = Color.White,
                fontSize = if (esGrande) 12.sp else 10.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 14.sp
            )
            if (subtitulo != null) {
                Text(
                    text = subtitulo,
                    color = Color(0xFFE50914),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (onDeleteProgress != null) {
            Box(modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)) {
                IconButton(
                    onClick = { menuExpandido = true },
                    modifier = Modifier
                        .size(28.dp)
                        .background(Color.Black.copy(alpha = 0.7f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Opciones",
                        tint = Color.White,
                        modifier = Modifier.padding(2.dp)
                    )
                }

                DropdownMenu(
                    expanded = menuExpandido,
                    onDismissRequest = { menuExpandido = false },
                    modifier = Modifier.background(Color(0xFF1E1E1E))
                ) {
                    // NUEVA OPCIÓN: Información
                    if (onInfoClick != null) {
                        DropdownMenuItem(
                            text = { Text("Información", color = Color.White) },
                            leadingIcon = { Icon(Icons.Default.Info, contentDescription = null, tint = Color.White) },
                            onClick = {
                                onInfoClick()
                                menuExpandido = false
                            }
                        )
                    }

                    DropdownMenuItem(
                        text = { Text(if (enBiblioteca) "Quitar de biblioteca" else "Añadir a biblioteca", color = Color.White) },
                        leadingIcon = { Icon(if (enBiblioteca) Icons.Default.Check else Icons.Default.Add, contentDescription = null, tint = Color.White) },
                        onClick = {
                            onToggleLibrary()
                            menuExpandido = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Olvidar progreso", color = Color(0xFFE50914)) },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFE50914)) },
                        onClick = {
                            onDeleteProgress()
                            menuExpandido = false
                        }
                    )
                }
            }
        } else {
            IconButton(
                onClick = onToggleLibrary,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(24.dp)
                    .padding(2.dp)
                    .background(
                        if (enBiblioteca) Color(0xFF2ECC71) else Color.Black.copy(alpha = 0.5f),
                        CircleShape
                    )
            ) {
                Icon(
                    imageVector = if (enBiblioteca) Icons.Default.Check else Icons.Default.Add,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.padding(2.dp)
                )
            }
        }
    }
}