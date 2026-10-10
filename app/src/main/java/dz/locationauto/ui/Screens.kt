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
fun AppRoot(vm: MainViewModel) {
    var tab by remember { mutableIntStateOf(0) }
    val titles = listOf(
        stringResource(R.string.tab_contracts),
        stringResource(R.string.tab_vehicles),
        stringResource(R.string.tab_clients),
        stringResource(R.string.tab_returns)
    )
    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.app_name)) }) },
        bottomBar = {
            NavigationBar {
                titles.forEachIndexed { i, t ->
                    NavigationBarItem(
                        selected = tab == i, onClick = { tab = i },
                        icon = {
                            Icon(
                                when (i) {
                                    0 -> Icons.Default.Description
                                    1 -> Icons.Default.DirectionsCar
                                    2 -> Icons.Default.People
                                    else -> Icons.Default.AssignmentReturn
                                }, null
                            )
                        },
                        label = { Text(t) }
                    )
                }
            }
        }
    ) { pad ->
        Box(Modifier.padding(pad)) {
            when (tab) {
                0 -> ContractsScreen(vm)
                1 -> VehiclesScreen(vm)
                2 -> ClientsScreen(vm)
                3 -> ReturnsScreen(vm)
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
                        Text("#${c.id} ${v?.brand} ${v?.model} — ${cl?.fullName ?: "?"}",
                            style = MaterialTheme.typography.titleSmall)
                        Text("${c.days()} j • ${c.totalDzd()} DZD • ${c.status}")
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
                OutlinedTextField(km, { km = it.filter(Char::isDigit) }, label = { Text("Km départ") })
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
        OutlinedTextField(brand, { brand = it }, label = { Text("Marque") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(model, { model = it }, label = { Text("Modèle") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(plate, { plate = it }, label = { Text("Immatriculation") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(rate, { rate = it.filter(Char::isDigit) }, label = { Text("Tarif DZD/jour") }, modifier = Modifier.fillMaxWidth())
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
                    supportingContent = { Text("${v.dailyRateDzd} DZD/j • " + if (v.available) "OK" else "Loué") }
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
        OutlinedTextField(name, { name = it }, label = { Text("Nom complet / الاسم") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(phone, { phone = it }, label = { Text("Téléphone / هاتف") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(license, { license = it }, label = { Text("Permis N°") }, modifier = Modifier.fillMaxWidth())
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
                    supportingContent = { Text("${c.phone} • ${c.licenseNumber}") }
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
                        Text("#${c.id} ${v?.brand} ${v?.model} — ${cl?.fullName}")
                        Text("Départ: ${c.startKm} km / ${c.startFuelPercent}% • ${c.totalDzd()} DZD")
                    }
                }
            }
        }
        val sel = active.firstOrNull { it.id == selectedId }
        if (sel != null) {
            OutlinedTextField(endKm, { endKm = it.filter(Char::isDigit) }, label = { Text("Km retour") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(fuel, { fuel = it.filter(Char::isDigit) }, label = { Text("Carburant %") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(damages, { damages = it }, label = { Text("Dégâts / أضرار") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(extras, { extras = it.filter(Char::isDigit) }, label = { Text("Frais supp DZD") }, modifier = Modifier.fillMaxWidth())
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
