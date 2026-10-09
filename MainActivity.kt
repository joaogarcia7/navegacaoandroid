package com.seuprojeto.diariodetreinos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import androidx.navigation.toRoute
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable

sealed interface Rota {
    @Serializable data object Splash : Rota
    @Serializable data object Login : Rota
    @Serializable data object ListaTreinos : Rota
    @Serializable data class DetalheTreino(val id: Int) : Rota
    @Serializable data object Estatisticas : Rota
    @Serializable data object Perfil : Rota
}

data class Treino(val id: Int, val titulo: String, val descricao: String)

class TreinoViewModel : ViewModel() {
    var usuarioLogado by mutableStateOf(false)
        private set

    val treinos = mutableStateListOf<Treino>()

    fun fazerLogin() {
        usuarioLogado = true
    }

    fun carregarTreinos() {
        treinos.clear()
        treinos.addAll(
            listOf(
                Treino(1, "Treino A - Peito e Tríceps", "Foco em hipertrofia, 4 séries de 10-12 repetições."),
                Treino(2, "Treino B - Costas e Bíceps", "Remadas e puxadas pesadas para expansão de dorsal."),
                Treino(3, "Treino C - Pernas Completo", "Agachamento livre, leg press e isoladores.")
            )
        )
    }

    fun limparTreinos() {
        treinos.clear()
    }

    fun buscarTreinoPorId(id: Int): Treino? {
        return treinos.find { it.id == id }
    }
}

private val LightColors = lightColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFFD32F2F),
    onPrimary = androidx.compose.ui.graphics.Color.White,
    secondary = androidx.compose.ui.graphics.Color(0xFF1976D2),
    onSecondary = androidx.compose.ui.graphics.Color.White,
    background = androidx.compose.ui.graphics.Color(0xFFF5F5F5),
    surface = androidx.compose.ui.graphics.Color.White,
    onBackground = androidx.compose.ui.graphics.Color(0xFF212121),
    onSurface = androidx.compose.ui.graphics.Color(0xFF212121)
)

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        content = content
    )
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppTheme {
                AppHost()
            }
        }
    }
}

@Composable
fun AppHost() {
    val navController = rememberNavController()
    val viewModel: TreinoViewModel = viewModel()
    
    val currentBackStack by navController.currentBackStackEntryAsState()
    val currentDestination = currentBackStack?.destination

    val showBottomBar = currentDestination?.hasRoute(Rota.ListaTreinos::class) == true ||
                        currentDestination?.hasRoute(Rota.Estatisticas::class) == true ||
                        currentDestination?.hasRoute(Rota.Perfil::class) == true

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                AppBottomNavigation(navController, currentDestination)
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Rota.Splash,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable<Rota.Splash> {
                SplashScreen(
                    onTempoEsgotado = {
                        val proximaRota = if (viewModel.usuarioLogado) Rota.ListaTreinos else Rota.Login
                        navController.navigate(proximaRota) {
                            popUpTo<Rota.Splash> { inclusive = true }
                        }
                    }
                )
            }

            composable<Rota.Login> {
                LoginScreen(
                    viewModel = viewModel,
                    onLoginSucesso = {
                        navController.navigate(Rota.ListaTreinos) {
                            popUpTo<Rota.Login> { inclusive = true }
                        }
                    }
                )
            }

            composable<Rota.ListaTreinos> {
                ListaTreinosScreen(
                    viewModel = viewModel,
                    onTreinoClick = { id -> 
                        navController.navigate(Rota.DetalheTreino(id)) 
                    }
                )
            }

            composable<Rota.DetalheTreino> { backStackEntry ->
                val detalheParams = backStackEntry.toRoute<Rota.DetalheTreino>()
                DetalheTreinoScreen(
                    id = detalheParams.id,
                    viewModel = viewModel,
                    onVoltar = { navController.popBackStack() }
                )
            }

            composable<Rota.Estatisticas> {
                EstatisticasScreen()
            }

            composable<Rota.Perfil> {
                PerfilScreen()
            }
        }
    }
}

@Composable
fun SplashScreen(onTempoEsgotado: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(2000)
        onTempoEsgotado()
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.FitnessCenter,
            contentDescription = "Carregando",
            modifier = Modifier.size(100.dp),
            tint = MaterialTheme.colorScheme.onPrimary
        )
    }
}

@Composable
fun LoginScreen(viewModel: TreinoViewModel, onLoginSucesso: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = "Login",
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = {
                viewModel.fazerLogin()
                onLoginSucesso()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Entrar no Diário", fontSize = 18.sp)
        }
    }
}

@Composable
fun ListaTreinosScreen(viewModel: TreinoViewModel, onTreinoClick: (Int) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(onClick = { viewModel.carregarTreinos() }) {
                Text("Carregar Treinos")
            }
            OutlinedButton(onClick = { viewModel.limparTreinos() }) {
                Text("Limpar")
            }
        }

        if (viewModel.treinos.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Lista vazia",
                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Nenhum treino encontrado.\nClique em 'Carregar Treinos'.",
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(viewModel.treinos) { treino ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onTreinoClick(treino.id) },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.DirectionsRun,
                                contentDescription = "Ícone de treino",
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = treino.titulo,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DetalheTreinoScreen(id: Int, viewModel: TreinoViewModel, onVoltar: () -> Unit) {
    val treino = viewModel.buscarTreinoPorId(id)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
    ) {
        IconButton(onClick = onVoltar) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
        }
        Spacer(modifier = Modifier.height(16.dp))
        
        if (treino != null) {
            Text(
                text = treino.titulo,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = treino.descricao,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
        } else {
            Text("Treino não encontrado.", color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
fun EstatisticasScreen() {
    Box(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Text("Tela de Estatísticas (Aba 2)", fontSize = 24.sp, color = MaterialTheme.colorScheme.onBackground)
    }
}

@Composable
fun PerfilScreen() {
    Box(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Text("Tela de Perfil (Aba 3)", fontSize = 24.sp, color = MaterialTheme.colorScheme.onBackground)
    }
}

@Composable
fun AppBottomNavigation(
    navController: NavHostController,
    currentDestination: androidx.navigation.NavDestination?
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.primary
    ) {
        val tabs = listOf(
            Triple(Rota.ListaTreinos, "Treinos", Icons.Default.List),
            Triple(Rota.Estatisticas, "Status", Icons.Default.ShowChart),
            Triple(Rota.Perfil, "Perfil", Icons.Default.Person)
        )

        tabs.forEach { (rota, titulo, icone) ->
            val selecionado = currentDestination?.hierarchy?.any { it.hasRoute(rota::class) } == true

            NavigationBarItem(
                selected = selecionado,
                onClick = {
                    navController.navigate(rota) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(icone, contentDescription = titulo) },
                label = { Text(titulo) }
            )
        }
    }
}
