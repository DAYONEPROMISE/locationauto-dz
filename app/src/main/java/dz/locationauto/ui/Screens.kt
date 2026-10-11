package dz.locationauto.ui

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import dz.locationauto.R
import dz.locationauto.data.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppRoot(vm: MainViewModel, dark: Boolean = false, onToggleDark: () -> Unit = {}) {
    var tab by remember { mutableIntStateOf(0) }
    val titles = listOf(
        stringResource(R.string.tab_contracts),
        stringResource(R.string.tab_vehicles),
        stringResource(R.string.tab_clients),
        stringResource(R.string.tab_returns)
    )
    val icons = listOf(
        Icons.Default.Description, Icons.Default.DirectionsCar,
        Icons.Default.People, Icons.Default.AssignmentReturn
    )
    BoxWithConstraints {
        val wide = maxWidth >= 600.dp
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.app_name)) },
                    actions = {
                        IconButton(onClick = onToggleDark) {
                            Icon(
                                if (dark) Icons.Default.LightMode else Icons.Default.DarkMode,
                                contentDescription = if (dark) "Mode clair" else "Mode sombre"
                            )
                        }
                    }
                )
            },
            bottomBar = {
                if (!wide) {
                    NavigationBar {
                        titles.forEachIndexed { i, t ->
                            NavigationBarItem(
                                selected = tab == i, onClick = { tab = i },
                                icon = { Icon(icons[i], contentDescription = t) },
                                label = { Text(t) }
                            )
                        }
                    }
                }
            },
            floatingActionButton = { },
            floatingActionButtonPosition = FabPosition.End
        ) { pad ->
            Row(Modifier.padding(pad).fillMaxSize()) {
                if (wide) {
                    NavigationRail {
                        titles.forEachIndexed { i, t ->
                            NavigationRailItem(
                                selected = tab == i, onClick = { tab = i },
                                icon = { Icon(icons[i], contentDescription = t) },
                                label = { Text(t) }
                            )
                        }
                    }
                    VerticalDivider()
                }
                Box(Modifier.weight(1f).fillMaxHeight()) {
                    when (tab) {
                        0 -> ContractsScreen(vm)
                        1 -> VehiclesScreen(vm)
                        2 -> ClientsScreen(vm)
                        else -> ReturnsScreen(vm)
                    }
                }
            }
        }
    }
}

@Composable
fun ContractsScreen(vm: MainViewModel) {
    val contracts by vm.contracts.collectAsState()
    val vehicles by vm.vehicles.collectAsState()
    val clients by vm.clients.collectAsState()
    val context = LocalContext.current
    var showNew by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().padding(12.dp)) {
        Button(onClick = { showNew = true }, enabled = vehicles.isNotEmpty() && clients.isNotEmpty()) {
            Text(stringResource(R.string.new_contract))
        }
        if (vehicles.isEmpty() || clients.isEmpty()) {
            Text(stringResource(R.string.add_vehicle_client_first), style = MaterialTheme.typography.bodySmall)
        }
        LazyColumn {
            items(contracts) { c ->
                val v = vehicles.firstOrNull { it.id == c.vehicleId }
                val cl = clients.firstOrNull { it.id == c.clientId }
                ElevatedCard(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Column(Modifier.padding(12.dp)) {
                        Text("#${c.id} ${v?.brand} ${v?.model} \u2014 ${cl?.fullName ?: "?"}",
                            style = MaterialTheme.typography.titleSmall)
                        Text(stringResource(R.string.days_fmt, c.days(), c.totalDzd(), c.status))
                        Row {
                            TextButton(onClick = {
                                if (v != null && cl != null) {
                                    val f = ContractPdfGenerator.generate(context, c, v, cl)
                                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", f)
                                    context.startActivity(Intent(Intent.ACTION_VIEW).apply {
                                        setDataAndType(uri, "application/pdf")
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    })
                                }
                            }) { Text(stringResource(R.string.pdf)) }
                        }
                    }
                }
            }
        }
    }
    if (showNew) NewContractDialog(vm, vehicles, clients) { showNew = false }
}

@Composable
fun NewContractDialog(vm: MainViewModel, vehicles: List<Vehicle>, clients: List<Client>, onClose: () -> Unit) {
    var vIdx by remember { mutableIntStateOf(0) }
    var cIdx by remember { mutableIntStateOf(0) }
    var days by remember { mutableStateOf("3") }
    var deposit by remember { mutableStateOf("20000") }
    var km by remember { mutableStateOf("0") }
    val avail = vehicles.filter { it.available }
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(stringResource(R.string.new_contract)) },
        text = {
            Column {
                if (avail.isEmpty()) { Text(stringResource(R.string.no_vehicle_available)); return@Column }
                Text("${stringResource(R.string.tab_vehicles)}: ${avail.getOrNull(vIdx)?.let { "${it.brand} ${it.model} (${it.plate})" } ?: ""}")
                Row {
                    Button(onClick = { vIdx = (vIdx + 1) % avail.size }) { Text(">>") }
                }
                Text("${stringResource(R.string.tab_clients)}: ${clients.getOrNull(cIdx)?.fullName ?: ""}")
                Row {
                    Button(onClick = { if (clients.isNotEmpty()) cIdx = (cIdx + 1) % clients.size }) { Text(">>") }
                }
                OutlinedTextField(days, { days = it.filter(Char::isDigit) }, label = { Text(stringResource(R.string.days)) })
                OutlinedTextField(deposit, { deposit = it.filter(Char::isDigit) }, label = { Text(stringResource(R.string.deposit)) })
                OutlinedTextField(km, { km = it.filter(Char::isDigit) }, label = { Text(stringResource(R.string.km_start)) })
            }
        },
        confirmButton = {
            Button(onClick = {
                val v = avail.getOrNull(vIdx) ?: return@Button
                val cl = clients.getOrNull(cIdx) ?: return@Button
                val now = System.currentTimeMillis()
                val d = days.toLongOrNull()?.coerceAtLeast(1) ?: 1
                vm.createContract(
                    Contract(
                        vehicleId = v.id, clientId = cl.id,
                        startDateEpoch = now, endDateEpoch = now + (d - 1) * 86_400_000,
                        dailyRateDzd = v.dailyRateDzd,
                        depositDzd = deposit.toLongOrNull() ?: 0,
                        startKm = km.toIntOrNull() ?: 0
                    )
                )
                onClose()
            }, enabled = avail.isNotEmpty() && clients.isNotEmpty()) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onClose) { Text(stringResource(R.string.cancel)) } }
    )
}

@Composable
fun VehiclesScreen(vm: MainViewModel) {
    val vehicles by vm.vehicles.collectAsState()
    var brand by remember { mutableStateOf("") }
    var model by remember { mutableStateOf("") }
    var plate by remember { mutableStateOf("") }
    var rate by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(12.dp)) {
        OutlinedTextField(brand, { brand = it }, label = { Text(stringResource(R.string.brand)) }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(model, { model = it }, label = { Text(stringResource(R.string.model)) }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(plate, { plate = it }, label = { Text(stringResource(R.string.plate)) }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(rate, { rate = it.filter(Char::isDigit) }, label = { Text(stringResource(R.string.rate_day)) }, modifier = Modifier.fillMaxWidth())
        Button(
            onClick = {
                vm.addVehicle(Vehicle(brand = brand.trim(), model = model.trim(), plate = plate.trim(), dailyRateDzd = rate.toLongOrNull() ?: 0))
                brand = ""; model = ""; plate = ""; rate = ""
            },
            enabled = brand.isNotBlank() && plate.isNotBlank(),
            modifier = Modifier.padding(vertical = 8.dp)
        ) { Text(stringResource(R.string.add)) }
        LazyColumn {
            items(vehicles) { v ->
                ListItem(
                    headlineContent = { Text("${v.brand} ${v.model} (${v.plate})") },
                    supportingContent = { Text(stringResource(R.string.rate_line, v.dailyRateDzd, if (v.available) "OK" else stringResource(R.string.rented))) }
                )
                HorizontalDivider()
            }
        }
    }
}

@Composable
fun ClientsScreen(vm: MainViewModel) {
    val clients by vm.clients.collectAsState()
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var license by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(12.dp)) {
        OutlinedTextField(name, { name = it }, label = { Text("Nom complet / \u0627\u0644\u0627\u0633\u0645") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(phone, { phone = it }, label = { Text("T\u00e9l\u00e9phone / \u0647\u0627\u062a\u0641") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(license, { license = it }, label = { Text(stringResource(R.string.license)) }, modifier = Modifier.fillMaxWidth())
        Button(
            onClick = {
                vm.addClient(Client(fullName = name.trim(), phone = phone.trim(), licenseNumber = license.trim()))
                name = ""; phone = ""; license = ""
            },
            enabled = name.isNotBlank() && phone.isNotBlank(),
            modifier = Modifier.padding(vertical = 8.dp)
        ) { Text(stringResource(R.string.add)) }
        LazyColumn {
            items(clients) { c ->
                ListItem(
                    headlineContent = { Text(c.fullName) },
                    supportingContent = { Text("${c.phone} \u2022 ${c.licenseNumber}") }
                )
                HorizontalDivider()
            }
        }
    }
}

@Composable
fun ReturnsScreen(vm: MainViewModel) {
    val active by vm.activeContracts.collectAsState()
    val vehicles by vm.vehicles.collectAsState()
    val clients by vm.clients.collectAsState()
    var selectedId by remember { mutableStateOf<Long?>(null) }
    var endKm by remember { mutableStateOf("") }
    var fuel by remember { mutableStateOf("100") }
    var damages by remember { mutableStateOf("") }
    var extras by remember { mutableStateOf("0") }
    Column(Modifier.fillMaxSize().padding(12.dp)) {
        Text(stringResource(R.string.tab_returns), style = MaterialTheme.typography.titleMedium)
        LazyColumn(Modifier.weight(1f)) {
            items(active) { c ->
                val v = vehicles.firstOrNull { it.id == c.vehicleId }
                val cl = clients.firstOrNull { it.id == c.clientId }
                ElevatedCard(
                    onClick = { selectedId = c.id },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = if (selectedId == c.id) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text("#${c.id} ${v?.brand} ${v?.model} \u2014 ${cl?.fullName}")
                        Text(stringResource(R.string.depart_fmt, c.startKm, c.startFuelPercent, c.totalDzd()))
                    }
                }
            }
        }
        val sel = active.firstOrNull { it.id == selectedId }
        if (sel != null) {
            OutlinedTextField(endKm, { endKm = it.filter(Char::isDigit) }, label = { Text(stringResource(R.string.km_return)) }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(fuel, { fuel = it.filter(Char::isDigit) }, label = { Text(stringResource(R.string.fuel_pct)) }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(damages, { damages = it }, label = { Text("D\u00e9g\u00e2ts / \u0623\u0636\u0631\u0627\u0631") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(extras, { extras = it.filter(Char::isDigit) }, label = { Text(stringResource(R.string.extra_fees)) }, modifier = Modifier.fillMaxWidth())
            Button(
                onClick = {
                    vm.closeContract(sel, endKm.toIntOrNull() ?: sel.startKm,
                        fuel.toIntOrNull()?.coerceIn(0, 100) ?: 0, damages, extras.toLongOrNull() ?: 0)
                    selectedId = null; endKm = ""; damages = ""; extras = "0"
                },
                modifier = Modifier.padding(vertical = 8.dp)
            ) { Text(stringResource(R.string.close_return)) }
        }
    }
}
