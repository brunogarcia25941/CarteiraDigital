package com.brunogarcia.carteira

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import org.jetbrains.compose.resources.painterResource
import carteiradigital.composeapp.generated.resources.Res
import carteiradigital.composeapp.generated.resources.compose_multiplatform
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import com.preat.peekaboo.image.picker.ResizeOptions
import com.preat.peekaboo.image.picker.SelectionMode
import com.preat.peekaboo.image.picker.rememberImagePickerLauncher
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.IconButton
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.animation.Crossfade
import androidx.compose.animation.togetherWith

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Preview
fun App() {

    // A memória para apanharmos o culpado!
    var erroGaleria by remember { mutableStateOf("") }

    // Instanciamos o Cofre
    val storageManager = remember { StorageManager() }

    // A memória da lista (começa por carregar o que estiver guardado)
    var listaDocumentos by remember { mutableStateOf(storageManager.carregarDocumentos()) }

    // variável que controla se o pop-up está visível ou não
    var mostrarDialogo by remember { mutableStateOf(false) }

    // variaveis para os campos de texto nos documentos
    var nomeDocumento by remember { mutableStateOf("") }
    var notasDocumento by remember { mutableStateOf("") }

    // A memória temporária para a foto
    var fotoBytes by remember { mutableStateOf<ByteArray?>(null) }

    // A memória para saber qual o documento a mostrar em ecrã inteiro (se for null, não mostra nada)
    var documentoEmEcraInteiro by remember { mutableStateOf<Documento?>(null) }

    // A memória para o Pop-up de confirmação de eliminação
    var documentoAApagar by remember { mutableStateOf<Documento?>(null) }

    // Variável de segurança (começa trancada = false)
    var autenticado by remember { mutableStateOf(false) }
    // Leitor para autenticação
    val biometricAuthenticator = rememberBiometricAuthenticator()

    // A "linha de trabalho" em segundo plano
    val scope = rememberCoroutineScope()

    // O Lançador do Peekaboo
    val imagePicker = rememberImagePickerLauncher(
        selectionMode = SelectionMode.Single,
        scope = scope,
        resizeOptions = ResizeOptions(width = 800, height = 800, compressionQuality = 0.5),
        onResult = { bytes ->
            // Quando voltamos da galeria, guardamos a primeira foto na nossa variável
            fotoBytes = bytes.firstOrNull()
        }
    )

    CarteiraTheme {

        // Garante que o fundo por trás da animação é sempre escuro, matando o "flash branco".
        androidx.compose.material3.Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            // Corre assim que o ecrã de bloqueio aparece
            LaunchedEffect(Unit) {
                biometricAuthenticator.authenticate(
                    onSuccess = { autenticado = true },
                    onError = { erro -> println("Erro na biometria: $erro") }
                )
            }

            AnimatedContent(
                targetState = autenticado,
                label = "TransicaoEcra",
                transitionSpec = {
                    // Quando a app é desbloqueada: O ecrã entra a deslizar de baixo e a aparecer, e o cadeado desaparece
                    (androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(500)) +
                            androidx.compose.animation.slideInVertically(animationSpec = androidx.compose.animation.core.tween(500)) { height -> height / 2 }) togetherWith
                            androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(500))
                }
            ) { estaAutenticado ->
                if (!estaAutenticado) {


                    // ECRÃ DE BLOQUEIO
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.Default.Lock,
                                contentDescription = "Cofre Trancado",
                                modifier = Modifier.size(80.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Carteira Trancada",
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.height(32.dp))
                            Button(
                                onClick = {
                                    // Chamar o sensor do telemóvel
                                    biometricAuthenticator.authenticate(
                                        onSuccess = { autenticado = true },
                                        onError = { erro ->
                                            println("Erro na biometria: $erro")
                                            // (Aqui poderíamos mostrar um aviso vermelho, mas para já só imprimimos o erro)
                                        }
                                    )
                                }
                            ) {
                                Text("Desbloquear com Biometria")
                            }
                        }
                    }
                } else {
                    // O Scaffold é o esqueleto do projeto
                    Scaffold(
                        topBar = {
                            TopAppBar(
                                title = {
                                    Text(
                                        text = "Carteira Digital",
                                        fontWeight = FontWeight.Bold
                                    )
                                },
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        },
                        floatingActionButton = {
                            FloatingActionButton(
                                onClick = { mostrarDialogo = true } // Abre o pop-up
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Adicionar Documento"
                                )
                            }
                        }
                    ) { innerPadding ->

                        // Box é o ecrã central
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding),
                            contentAlignment = Alignment.Center
                        ) {
                            // Se a lista estiver vazia, mostramos a mensagem
                            if (listaDocumentos.isEmpty()) {
                                Text(
                                    text = "A tua carteira está vazia.\nClica no '+' para adicionar um documento.",
                                    textAlign = TextAlign.Center,
                                    color = Color.Gray
                                )
                            } else {
                                // Se tiver documentos, mostramos a lista
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(16.dp)
                                ) {
                                    items(listaDocumentos, key = { it.id }) { doc ->
                                        Card(
                                            modifier = Modifier
                                                .animateItem()
                                                .fillMaxWidth()
                                                .padding(bottom = 16.dp)
                                                .clickable {
                                                    // Ao clicar no cartão, abrimos este documento em ecrã inteiro
                                                    documentoEmEcraInteiro = doc
                                                }
                                        ) {
                                            // Usamos uma Row para ter Foto na Esquerda, Texto na Direita
                                            Row(
                                                modifier = Modifier.padding(16.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {

                                                // 1. A NOSSA MÁGICA DA IMAGEM AQUI!
                                                if (doc.caminhoFoto != null) {
                                                    // O 'remember' faz com que a foto só seja convertida 1 vez para poupar bateria
                                                    val bitmap = remember(doc.caminhoFoto) {
                                                        val bytes =
                                                            Base64.Default.decode(doc.caminhoFoto)
                                                        converterBytesParaBitmap(bytes)
                                                    }

                                                    if (bitmap != null) {
                                                        androidx.compose.foundation.Image(
                                                            bitmap = bitmap,
                                                            contentDescription = "Foto de ${doc.nome}",
                                                            modifier = Modifier
                                                                .size(60.dp) // O tamanho da miniatura
                                                                .clip(RoundedCornerShape(8.dp)), // Cantos redondos na foto
                                                            contentScale = ContentScale.Crop // Corta a foto para ficar um quadrado perfeito
                                                        )
                                                        Spacer(modifier = Modifier.width(16.dp)) // Espaço entre a foto e o texto
                                                    }
                                                }

                                                // TEXTOS
                                                Column {
                                                    Text(
                                                        text = doc.nome,
                                                        fontWeight = FontWeight.Bold,
                                                        style = MaterialTheme.typography.titleLarge
                                                    )
                                                    if (doc.notas.isNotBlank()) {
                                                        Spacer(modifier = Modifier.height(4.dp))
                                                        Text(text = doc.notas, color = Color.Gray)
                                                    }
                                                }

                                                // CÓDIGO DO CAIXOTE DO LIXO
                                                // Este Spacer empurra o botão do lixo totalmente para a direita
                                                Spacer(modifier = Modifier.weight(1f))

                                                IconButton(
                                                    onClick = {
                                                        // Em vez de apagar logo, dizemos à app qual queremos apagar
                                                        documentoAApagar = doc
                                                    }
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Delete,
                                                        contentDescription = "Apagar Documento",
                                                        tint = Color.Red.copy(alpha = 0.7f) // Um vermelho suave
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } // fim scaffold
                } // fim autenticado
            } // fim crossfade

            // Se a variável for verdadeira, Pop-up no ecrã
            if (mostrarDialogo) {
                AlertDialog(
                    onDismissRequest = { mostrarDialogo = false },
                    title = {
                        Text(text = "Novo Documento")
                    },
                    text = {
                        Column(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = nomeDocumento,
                                onValueChange = { nomeDocumento = it },
                                label = { Text("Nome do Documento") },
                                placeholder = { Text("Ex: Cartão de Cidadão") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            OutlinedTextField(
                                value = notasDocumento,
                                onValueChange = { notasDocumento = it },
                                label = { Text("Notas Adicionais") },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 3
                            )
                            Spacer(modifier = Modifier.height(16.dp))


                            if (erroGaleria.isNotBlank()) {
                                Text(
                                    text = "Erro: $erroGaleria",
                                    color = Color.Red,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            // botao da camara

                            Button(
                                onClick = {
                                    try {
                                        erroGaleria = "" // Limpa erros antigos
                                        imagePicker.launch() // Tenta abrir a Galeria
                                    } catch (e: Exception) {
                                        // APANHADO! Guardamos o erro para mostrar no ecrã
                                        erroGaleria = e.toString()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                // Se já tiver foto, mostra um Visto verde
                                if (fotoBytes == null) {
                                    Text("📸 Anexar Fotografia")
                                } else {
                                    Text("✅ Fotografia Anexada")
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (nomeDocumento.isNotBlank()) {

                                    // Transformamos a foto num texto gigante (Base64)
                                    val fotoTextoBase64 =
                                        fotoBytes?.let { Base64.Default.encode(it) }

                                    val novoDoc = Documento(
                                        id = kotlin.random.Random.nextInt().toString(),
                                        nome = nomeDocumento,
                                        notas = notasDocumento,
                                        caminhoFoto = fotoTextoBase64 // Passamos o texto gigante
                                    )

                                    storageManager.guardarDocumento(novoDoc)
                                    listaDocumentos = storageManager.carregarDocumentos()

                                    // Limpa tudo
                                    nomeDocumento = ""
                                    notasDocumento = ""
                                    fotoBytes = null
                                    mostrarDialogo = false
                                }
                            }
                        ) {
                            Text("Guardar")
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = {
                                nomeDocumento = ""
                                notasDocumento = ""
                                fotoBytes = null // Limpamos a foto também
                                mostrarDialogo = false
                            }
                        ) {
                            Text("Cancelar", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                )
            }
            documentoEmEcraInteiro?.let { docSelecionado ->
                Dialog(
                    onDismissRequest = { documentoEmEcraInteiro = null },
                    properties = DialogProperties(usePlatformDefaultWidth = false) // Para ocupar o ecrã TODO!
                ) {
                    // Variáveis para controlar o Zoom e o Arrastar (Pan)
                    var scale by remember { mutableStateOf(1f) }
                    var offsetX by remember { mutableStateOf(0f) }
                    var offsetY by remember { mutableStateOf(0f) }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black) // Fundo preto de galeria
                    ) {
                        // Se o documento tiver foto, desenhamos a foto com os gestos
                        if (docSelecionado.caminhoFoto != null) {
                            val bitmap = remember(docSelecionado.caminhoFoto) {
                                val bytes = Base64.Default.decode(docSelecionado.caminhoFoto)
                                converterBytesParaBitmap(bytes)
                            }

                            if (bitmap != null) {
                                Image(
                                    bitmap = bitmap,
                                    contentDescription = "Foto em ecrã inteiro",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        // zoom
                                        .pointerInput(Unit) {
                                            detectTransformGestures { _, pan, zoom, _ ->
                                                // Atualiza o nível de zoom (limitado entre 1x e 5x)
                                                scale = (scale * zoom).coerceIn(1f, 5f)
                                                // Atualiza a posição (para poder arrastar a foto quando tem zoom)
                                                offsetX += pan.x * scale
                                                offsetY += pan.y * scale
                                            }
                                        }
                                        // aplica o zoom e a posicao à imagem visualmente
                                        .graphicsLayer(
                                            scaleX = scale,
                                            scaleY = scale,
                                            translationX = offsetX,
                                            translationY = offsetY
                                        )
                                )
                            }
                        }

                        // Botão de Fechar no canto superior direito
                        IconButton(
                            onClick = { documentoEmEcraInteiro = null },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(16.dp)
                                .background(
                                    Color.Black.copy(alpha = 0.5f),
                                    RoundedCornerShape(50)
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Fechar",
                                tint = Color.White
                            )
                        }
                    }
                }
            }
            // 4. O POP-UP DE CONFIRMAÇÃO PARA APAGAR
            documentoAApagar?.let { doc ->
                AlertDialog(
                    onDismissRequest = { documentoAApagar = null },
                    title = { Text("Apagar Documento") },
                    text = { Text("Tens a certeza que queres apagar '${doc.nome}'? Esta ação não pode ser desfeita.") },
                    confirmButton = {
                        Button(
                            onClick = {
                                // 1. Apaga do cofre
                                storageManager.removerDocumento(doc.id)
                                // 2. Atualiza a lista no ecrã
                                listaDocumentos = storageManager.carregarDocumentos()
                                // 3. Fecha o pop-up
                                documentoAApagar = null
                            },
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = Color.Red
                            )
                        ) {
                            Text("Apagar", color = Color.White)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { documentoAApagar = null }) {
                            Text("Cancelar", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                )
            }
        }
    }// fim carteira theme
}