package dz.locationauto.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dz.locationauto.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(private val db: AppDatabase) : ViewModel() {
    val vehicles: StateFlow<List<Vehicle>> = db.vehicleDao().observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val clients: StateFlow<List<Client>> = db.clientDao().observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val contracts: StateFlow<List<Contract>> = db.contractDao().observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val activeContracts: StateFlow<List<Contract>> = db.contractDao().observeActive()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addVehicle(v: Vehicle) = viewModelScope.launch { db.vehicleDao().insert(v) }
    fun addClient(c: Client) = viewModelScope.launch { db.clientDao().insert(c) }

    fun createContract(c: Contract, onDone: (Long) -> Unit = {}) = viewModelScope.launch {
        val id = db.contractDao().insert(c)
        db.vehicleDao().byId(c.vehicleId)?.let { db.vehicleDao().update(it.copy(available = false)) }
        onDone(id)
    }

    fun closeContract(
        contract: Contract, endKm: Int, endFuel: Int,
        damages: String, extras: Long, onDone: () -> Unit = {}
    ) = viewModelScope.launch {
        db.contractDao().update(
            contract.copy(
                status = "CLOSED", endKm = endKm, endFuelPercent = endFuel,
                damageNotes = damages, extraChargesDzd = extras
            )
        )
        db.vehicleDao().byId(contract.vehicleId)?.let { db.vehicleDao().update(it.copy(available = true)) }
        onDone()
    }
}
