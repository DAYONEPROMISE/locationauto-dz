package dz.locationauto.data

import androidx.room.*

@Entity(tableName = "vehicles")
data class Vehicle(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val brand: String,
    val model: String,
    val plate: String,
    val year: Int = 2020,
    val dailyRateDzd: Long = 0,
    val available: Boolean = true
)

@Entity(tableName = "clients")
data class Client(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fullName: String,
    val phone: String,
    val licenseNumber: String = "",
    val nationalId: String = ""
)

@Entity(tableName = "contracts")
data class Contract(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val vehicleId: Long,
    val clientId: Long,
    val startDateEpoch: Long,
    val endDateEpoch: Long,
    val dailyRateDzd: Long,
    val depositDzd: Long = 0,
    val startKm: Int = 0,
    val startFuelPercent: Int = 100,
    val status: String = "ACTIVE", // ACTIVE | CLOSED
    val endKm: Int? = null,
    val endFuelPercent: Int? = null,
    val damageNotes: String = "",
    val extraChargesDzd: Long = 0
) {
    fun days(): Long = maxOf(1, (endDateEpoch - startDateEpoch) / 86_400_000 + 1)
    fun totalDzd(): Long = days() * dailyRateDzd + extraChargesDzd
}

@Dao
interface VehicleDao {
    @Query("SELECT * FROM vehicles ORDER BY id DESC") fun observeAll(): kotlinx.coroutines.flow.Flow<List<Vehicle>>
    @Query("SELECT * FROM vehicles ORDER BY id DESC") suspend fun getAll(): List<Vehicle>
    @Query("SELECT * FROM vehicles WHERE id = :id") suspend fun byId(id: Long): Vehicle?
    @Insert suspend fun insert(v: Vehicle): Long
    @Update suspend fun update(v: Vehicle)
    @Delete suspend fun delete(v: Vehicle)
}

@Dao
interface ClientDao {
    @Query("SELECT * FROM clients ORDER BY id DESC") fun observeAll(): kotlinx.coroutines.flow.Flow<List<Client>>
    @Query("SELECT * FROM clients ORDER BY id DESC") suspend fun getAll(): List<Client>
    @Query("SELECT * FROM clients WHERE id = :id") suspend fun byId(id: Long): Client?
    @Insert suspend fun insert(c: Client): Long
    @Update suspend fun update(c: Client)
    @Delete suspend fun delete(c: Client)
}

@Dao
interface ContractDao {
    @Query("SELECT * FROM contracts ORDER BY id DESC") fun observeAll(): kotlinx.coroutines.flow.Flow<List<Contract>>
    @Query("SELECT * FROM contracts ORDER BY id DESC") suspend fun getAll(): List<Contract>
    @Query("SELECT * FROM contracts WHERE id = :id") suspend fun byId(id: Long): Contract?
    @Query("SELECT * FROM contracts WHERE status = 'ACTIVE' ORDER BY id DESC") fun observeActive(): kotlinx.coroutines.flow.Flow<List<Contract>>
    @Insert suspend fun insert(c: Contract): Long
    @Update suspend fun update(c: Contract)
    @Delete suspend fun delete(c: Contract)
}

@Database(entities = [Vehicle::class, Client::class, Contract::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun vehicleDao(): VehicleDao
    abstract fun clientDao(): ClientDao
    abstract fun contractDao(): ContractDao
}
