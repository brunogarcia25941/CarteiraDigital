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
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.graphics.ImageBitmap


// Função auxiliar Composable que carrega o bitmap da imagem.
// Caso a imagem ainda esteja no formato Base64 antigo, faz a migração automática para ficheiro físico local.
@Composable
fun rememberDocumentBitmap(
    doc: Documento,
    imageStorage: ImageStorage,
    storageManager: StorageManager
): ImageBitmap? {
    return remember(doc.caminhoFoto) {
        val caminho = doc.caminhoFoto ?: return@remember null
        
        // 1. Tentar ler do ficheiro físico local
        var bytes = imageStorage.loadImage(caminho)
        if (bytes == null) {
            // 2. Se falhar, tenta ler como Base64 (migração do formato antigo)
            try {
                bytes = Base64.Default.decode(caminho)
                // Se descodificar com sucesso, migramos imediatamente para ficheiro local
                val novoCaminho = imageStorage.saveImage(bytes, "foto_${doc.id}.jpg")
                if (novoCaminho != null) {
                    val listaAtual = storageManager.carregarDocumentos().toMutableList()
                    val index = listaAtual.indexOfFirst { it.id == doc.id }
                    if (index != -1) {
                        listaAtual[index] = doc.copy(caminhoFoto = novoCaminho)
                        storageManager.guardarListaDocumentos(listaAtual)
                        println("Documento ${doc.nome} migrado com sucesso para armazenamento local.")
                    }
                }
            } catch (e: Exception) {
                // Ficheiro corrompido ou formato não-Base64
                println("Erro ao carregar ou migrar imagem: ${e.message}")
            }
        }
        bytes?.let { converterBytesParaBitmap(it) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Preview
fun App() {

    // A memória para apanharmos o culpado!
    var erroGaleria by remember { mutableStateOf("") }

    // Instanciamos o Cofre e o Gestor de Ficheiros Físicos
    val storageManager = remember { StorageManager() }
    val imageStorage = rememberImageStorage()

    // A memória da lista (começa por carregar o que estiver guardado)
    var listaDocumentos by remember { mutableStateOf(storageManager.carregarDocumentos()) }

    // variável que controla se o pop-up está visível ou não
    var mostrarDialogo by remember { mutableStateOf(false) }

    // variaveis para os campos de texto nos documentos
    var nomeDocumento by remember { mutableStateOf("") }
    var notasDocumento by remember { mutableStateOf("") }

    // Gestão de Categorias e Filtros
    var categoriaSelecionada by remember { mutableStateOf("Todos") }
    var categoriaNovoDocumento by remember { mutableStateOf("Outros") }

    // Instanciamos o Reconhecedor de Texto (OCR)
    val textRecognizer = rememberTextRecognizer()
    var ocrProcessando by remember { mutableStateOf(false) }
    var ocrErro by remember { mutableStateOf("") }

    // Filtra a lista de documentos com base na categoria selecionada
    val documentosFiltrados = remember(listaDocumentos, categoriaSelecionada) {
        if (categoriaSelecionada == "Todos") {
            listaDocumentos
        } else {
            listaDocumentos.filter { it.categoria == categoriaSelecionada }
        }
    }

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

    // Variáveis de estado para a autenticação alternativa por PIN
    var pinIntroduzido by remember { mutableStateOf("") }
    var pinErro by remember { mutableStateOf("") }
    var definindoNovoPin by remember { mutableStateOf(false) }
    var novoPin by remember { mutableStateOf("") }
    var confirmarNovoPin by remember { mutableStateOf("") }
    var erroMensagem by remember { mutableStateOf("") }
    var mostrarPromptPinAposBiometria by remember { mutableStateOf(false) }

    // A "linha de trabalho" em segundo plano
    val scope = rememberCoroutineScope()

    // O Lançador do Peekaboo com melhor resolução (1600x1600) e qualidade superior (90%)
    val imagePicker = rememberImagePickerLauncher(
        selectionMode = SelectionMode.Single,
        scope = scope,
        resizeOptions = ResizeOptions(width = 1600, height = 1600, compressionQuality = 0.90),
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
                    onSuccess = { 
                        autenticado = true 
                        erroMensagem = ""
                        // Se a biometria deu certo, mas não temos um PIN de fallback, vamos sugerir criar um
                        if (storageManager.obterPin() == null) {
                            mostrarPromptPinAposBiometria = true
                        }
                    },
                    onError = { erro -> 
                        println("Erro na biometria: $erro")
                        erroMensagem = erro
                    }
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


                    // ECRÃ DE BLOQUEIO / DEFINIÇÃO DE PIN
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.Default.Lock,
                                contentDescription = "Cofre Trancado",
                                modifier = Modifier.size(80.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            val pinRegistado = storageManager.obterPin()

                            if (definindoNovoPin || (pinRegistado == null && erroMensagem.isNotBlank())) {
                                // Modo de configuração do primeiro PIN (Fallback ou dispositivo sem biometria)
                                Text(
                                    text = "Configurar PIN de Acesso",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Define um código PIN para acederes aos teus documentos.",
                                    textAlign = TextAlign.Center,
                                    color = Color.Gray,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Spacer(modifier = Modifier.height(24.dp))

                                OutlinedTextField(
                                    value = novoPin,
                                    onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) novoPin = it },
                                    label = { Text("Novo PIN (4-6 dígitos)") },
                                    visualTransformation = PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(0.8f)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                OutlinedTextField(
                                    value = confirmarNovoPin,
                                    onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) confirmarNovoPin = it },
                                    label = { Text("Confirmar PIN") },
                                    visualTransformation = PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(0.8f)
                                )

                                if (pinErro.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(text = pinErro, color = Color.Red, style = MaterialTheme.typography.bodySmall)
                                }

                                Spacer(modifier = Modifier.height(24.dp))
                                Button(
                                    onClick = {
                                        if (novoPin.length < 4) {
                                            pinErro = "O PIN deve ter entre 4 a 6 dígitos."
                                        } else if (novoPin != confirmarNovoPin) {
                                            pinErro = "Os PINs introduzidos não coincidem."
                                        } else {
                                            storageManager.guardarPin(novoPin)
                                            autenticado = true
                                            definindoNovoPin = false
                                            novoPin = ""
                                            confirmarNovoPin = ""
                                            pinErro = ""
                                            erroMensagem = ""
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(0.8f)
                                ) {
                                    Text("Guardar PIN e Entrar")
                                }
                            } else {
                                // Modo de Desbloqueio Normal (Biometria ou PIN existente)
                                Text(
                                    text = "Carteira Trancada",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = MaterialTheme.colorScheme.onBackground
                                )

                                if (erroMensagem.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = erroMensagem,
                                        color = Color.Red,
                                        style = MaterialTheme.typography.bodySmall,
                                        textAlign = TextAlign.Center
                                    )
                                }

                                Spacer(modifier = Modifier.height(32.dp))

                                if (pinRegistado != null) {
                                    // Campo para introduzir o PIN existente
                                    OutlinedTextField(
                                        value = pinIntroduzido,
                                        onValueChange = {
                                            if (it.length <= 6 && it.all { c -> c.isDigit() }) {
                                                pinIntroduzido = it
                                                pinErro = ""
                                                if (it == pinRegistado) {
                                                    autenticado = true
                                                    pinIntroduzido = ""
                                                    erroMensagem = ""
                                                }
                                            }
                                        },
                                        label = { Text("Introduz o PIN de Acesso") },
                                        visualTransformation = PasswordVisualTransformation(),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(0.8f)
                                    )

                                    if (pinErro.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(text = pinErro, color = Color.Red, style = MaterialTheme.typography.bodySmall)
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(0.8f),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    Button(
                                        onClick = {
                                            biometricAuthenticator.authenticate(
                                                onSuccess = {
                                                    autenticado = true
                                                    erroMensagem = ""
                                                },
                                                onError = { erro ->
                                                    println("Erro na biometria: $erro")
                                                    erroMensagem = erro
                                                }
                                            )
                                        }
                                    ) {
                                        Text("Usar Biometria")
                                    }

                                    if (pinRegistado == null) {
                                        // Se não há PIN e a biometria falhou/indisponível, permite criar um
                                        Button(
                                            onClick = { definindoNovoPin = true }
                                        ) {
                                            Text("Configurar PIN")
                                        }
                                    }
                                }
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

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            // 1. Barra de Filtros de Categorias (só visível se tiver algum documento)
                            if (listaDocumentos.isNotEmpty()) {
                                val filterScrollState = rememberScrollState()
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(filterScrollState)
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val CATEGORIAS = listOf("Todos", "Identidade", "Finanças", "Saúde", "Viagens", "Outros")
                                    CATEGORIAS.forEach { cat ->
                                        val selecionada = categoriaSelecionada == cat
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(50))
                                                .background(
                                                    if (selecionada) MaterialTheme.colorScheme.primary 
                                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                                )
                                                .clickable { categoriaSelecionada = cat }
                                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                        ) {
                                            Text(
                                                text = cat,
                                                color = if (selecionada) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = if (selecionada) FontWeight.Bold else FontWeight.Normal
                                            )
                                        }
                                    }
                                }
                            }

                            // 2. Área principal com a Lista Filtrada ou Mensagem Vazia
                            if (documentosFiltrados.isEmpty()) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (listaDocumentos.isEmpty()) {
                                            "A tua carteira está vazia.\nClica no '+' para adicionar um documento."
                                        } else {
                                            "Nenhum documento encontrado em '$categoriaSelecionada'."
                                        },
                                        textAlign = TextAlign.Center,
                                        color = Color.Gray
                                    )
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 16.dp)
                                ) {
                                    items(documentosFiltrados, key = { it.id }) { doc ->
                                        Card(
                                            modifier = Modifier
                                                .animateItem()
                                                .fillMaxWidth()
                                                .padding(bottom = 16.dp)
                                                .clickable {
                                                    documentoEmEcraInteiro = doc
                                                }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(16.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                if (doc.caminhoFoto != null) {
                                                    val bitmap = rememberDocumentBitmap(doc, imageStorage, storageManager)

                                                    if (bitmap != null) {
                                                        androidx.compose.foundation.Image(
                                                            bitmap = bitmap,
                                                            contentDescription = "Foto de ${doc.nome}",
                                                            modifier = Modifier
                                                                .size(60.dp)
                                                                .clip(RoundedCornerShape(8.dp)),
                                                            contentScale = ContentScale.Crop
                                                        )
                                                        Spacer(modifier = Modifier.width(16.dp))
                                                    }
                                                }

                                                // TEXTOS (com badge de categoria)
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(4.dp))
                                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                                    ) {
                                                        Text(
                                                            text = doc.categoria.uppercase(),
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = MaterialTheme.colorScheme.primary,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.height(4.dp))
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

            // Pop-up sugerindo a criação de um PIN caso o utilizador tenha entrado por biometria mas não tenha PIN de fallback
            if (mostrarPromptPinAposBiometria) {
                var pinTemp by remember { mutableStateOf("") }
                var pinTempConfirmar by remember { mutableStateOf("") }
                var pinTempErro by remember { mutableStateOf("") }

                AlertDialog(
                    onDismissRequest = { mostrarPromptPinAposBiometria = false },
                    title = { Text("Recomendado: Definir PIN") },
                    text = {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Para garantir que não ficas trancado caso a biometria falhe, cria um PIN de acesso de segurança.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            OutlinedTextField(
                                value = pinTemp,
                                onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) pinTemp = it },
                                label = { Text("PIN (4-6 dígitos)") },
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = pinTempConfirmar,
                                onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) pinTempConfirmar = it },
                                label = { Text("Confirmar PIN") },
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            if (pinTempErro.isNotBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(text = pinTempErro, color = Color.Red, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (pinTemp.length < 4) {
                                    pinTempErro = "O PIN deve ter entre 4 a 6 dígitos."
                                } else if (pinTemp != pinTempConfirmar) {
                                    pinTempErro = "Os PINs não coincidem."
                                } else {
                                    storageManager.guardarPin(pinTemp)
                                    mostrarPromptPinAposBiometria = false
                                }
                            }
                        ) {
                            Text("Guardar PIN")
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { mostrarPromptPinAposBiometria = false }
                        ) {
                            Text("Mais Tarde", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                )
            }

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

                            // SELETOR DE CATEGORIA (Premium Custom Chips)
                            Text("Categoria", style = MaterialTheme.typography.titleSmall)
                            Spacer(modifier = Modifier.height(8.dp))
                            val scrollState = rememberScrollState()
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(scrollState),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val CATEGORIAS_CADASTRO = listOf("Identidade", "Finanças", "Saúde", "Viagens", "Outros")
                                CATEGORIAS_CADASTRO.forEach { cat ->
                                    val selecionada = categoriaNovoDocumento == cat
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(50))
                                            .background(if (selecionada) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                            .clickable { categoriaNovoDocumento = cat }
                                            .padding(horizontal = 16.dp, vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = cat,
                                            color = if (selecionada) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }
                            }

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

                            // Botão para anexar fotografia
                            Button(
                                onClick = {
                                    try {
                                        erroGaleria = "" // Limpa erros antigos
                                        imagePicker.launch() // Tenta abrir a Galeria
                                    } catch (e: Exception) {
                                        erroGaleria = e.toString()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (fotoBytes == null) {
                                    Text("📸 Anexar Fotografia")
                                } else {
                                    Text("✅ Fotografia Anexada")
                                }
                            }

                            // EXTRAÇÃO DE TEXTO POR OCR (Só visível se houver foto)
                            if (fotoBytes != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = {
                                        val bytes = fotoBytes ?: return@Button
                                        ocrProcessando = true
                                        ocrErro = ""
                                        textRecognizer.recognizeText(
                                            imageBytes = bytes,
                                            onSuccess = { textoExtraido ->
                                                ocrProcessando = false
                                                if (textoExtraido.isNotBlank()) {
                                                    // Junta o texto do OCR às notas
                                                    notasDocumento = if (notasDocumento.isBlank()) {
                                                        textoExtraido
                                                    } else {
                                                        "$notasDocumento\n\n[OCR - Texto Detetado]:\n$textoExtraido"
                                                    }
                                                } else {
                                                    ocrErro = "Nenhum texto legível encontrado."
                                                }
                                            },
                                            onError = { erro ->
                                                ocrProcessando = false
                                                ocrErro = erro
                                            }
                                        )
                                    },
                                    enabled = !ocrProcessando,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                ) {
                                    Text(if (ocrProcessando) "🔍 A processar OCR..." else "🔍 Extrair Texto da Foto (OCR)")
                                }

                                if (ocrErro.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = ocrErro,
                                        color = Color.Red,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (nomeDocumento.isNotBlank()) {
                                    val docId = kotlin.random.Random.nextInt().toString()
                                    // Grava os bytes da imagem no disco e obtém o caminho absoluto
                                    val fotoCaminho = fotoBytes?.let { bytes ->
                                        imageStorage.saveImage(bytes, "foto_$docId.jpg")
                                    }

                                    val novoDoc = Documento(
                                        id = docId,
                                        nome = nomeDocumento,
                                        notas = notasDocumento,
                                        caminhoFoto = fotoCaminho,
                                        categoria = categoriaNovoDocumento // Guarda a categoria selecionada
                                    )

                                    storageManager.guardarDocumento(novoDoc)
                                    listaDocumentos = storageManager.carregarDocumentos()

                                    // Limpa tudo
                                    nomeDocumento = ""
                                    notasDocumento = ""
                                    fotoBytes = null
                                    categoriaNovoDocumento = "Outros"
                                    ocrErro = ""
                                    ocrProcessando = false
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
                                fotoBytes = null
                                categoriaNovoDocumento = "Outros"
                                ocrErro = ""
                                ocrProcessando = false
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
                            val bitmap = rememberDocumentBitmap(docSelecionado, imageStorage, storageManager)


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
                                // 1. Apaga o ficheiro físico no disco, se existir
                                doc.caminhoFoto?.let { caminho ->
                                    imageStorage.deleteImage(caminho)
                                }
                                // 2. Apaga do cofre
                                storageManager.removerDocumento(doc.id)
                                // 3. Atualiza a lista no ecrã
                                listaDocumentos = storageManager.carregarDocumentos()
                                // 4. Fecha o pop-up
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