package com.or.daftarcha.data.sync

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.or.daftarcha.data.dao.*
import com.or.daftarcha.data.model.*
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

// Helper extension for Task<T>.await()
private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { continuation ->
    addOnSuccessListener { result -> continuation.resume(result) }
    addOnFailureListener { exception -> continuation.resumeWithException(exception) }
    addOnCanceledListener { continuation.cancel() }
}

sealed class SyncStatus {
    object Idle : SyncStatus()
    object InProgress : SyncStatus()
    data class Success(val message: String, val timestamp: Long) : SyncStatus()
    data class Error(val message: String) : SyncStatus()
}

data class SyncSummary(
    val projectsCount: Int = 0,
    val employeesCount: Int = 0,
    val attendanceCount: Int = 0,
    val paymentsCount: Int = 0
)

@Singleton
class FirestoreSyncManager @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val projectDao: ProjectDao,
    private val employeeDao: EmployeeDao,
    private val projectEmployeeDao: ProjectEmployeeDao,
    private val attendanceDao: AttendanceDao,
    private val bonusDao: BonusDao,
    private val expenseDao: ExpenseDao,
    private val paymentDao: PaymentDao,
    private val appUserDao: AppUserDao,
    @ApplicationContext private val context: Context
) {
    private val TAG = "FirestoreSync"
    private val prefs: SharedPreferences = context.getSharedPreferences("firestore_sync_prefs", Context.MODE_PRIVATE)
    private val authPrefs: SharedPreferences = context.getSharedPreferences("daftarcha_auth_prefs", Context.MODE_PRIVATE)

    private val _syncStatus = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    fun getCurrentBrigadierId(): String {
        val bId = authPrefs.getString("saved_brigadier_id", null)
        if (!bId.isNullOrBlank()) return bId
        val lastBId = authPrefs.getString("last_active_brigadier_id", null)
        if (!lastBId.isNullOrBlank()) return lastBId
        val role = authPrefs.getString("saved_user_role", null)
        val loginId = authPrefs.getString("saved_login_id", null) ?: ""
        return if (role == UserRole.BRIGADIER.name) loginId else ""
    }

    fun getLastSyncTime(): Long {
        return prefs.getLong("last_sync_timestamp", 0L)
    }

    fun getFormattedLastSync(): String {
        val lastTime = getLastSyncTime()
        if (lastTime == 0L) return "Ҳали синхронизация қилинмаган"
        val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
        return sdf.format(Date(lastTime))
    }

    /**
     * Полноценная двухсторонняя синхронизация:
     * Изолирована под текущего бригадира!
     */
    suspend fun syncAll(): Result<SyncSummary> = withContext(Dispatchers.IO) {
        val brigadierId = getCurrentBrigadierId()
        if (brigadierId.isBlank()) {
            val emptySummary = SyncSummary()
            _syncStatus.value = SyncStatus.Idle
            return@withContext Result.success(emptySummary)
        }

        _syncStatus.value = SyncStatus.InProgress
        try {
            Log.d(TAG, "Синхронизация бошланди (Бригадир: $brigadierId)...")

            // Шаг 1: Выгружаем локальные данные бригадира в облако
            uploadToFirestore(brigadierId)

            // Шаг 2: Скачиваем данные бригадира из облака и обновляем локальную базу
            downloadFromFirestore(brigadierId)

            // Шаг 3: Повторно выгружаем объединенное состояние в облако и получаем итог
            val summary = uploadToFirestore(brigadierId)

            val now = System.currentTimeMillis()
            prefs.edit().putLong("last_sync_timestamp", now).apply()

            val successMsg = "Синхронизация муваффақиятли якунланди"
            _syncStatus.value = SyncStatus.Success(successMsg, now)
            Log.d(TAG, successMsg)

            Result.success(summary)
        } catch (e: Exception) {
            val errorMsg = e.localizedMessage ?: "Синхронизацияда хатолик юз берди"
            Log.e(TAG, "Sync error: $errorMsg", e)
            _syncStatus.value = SyncStatus.Error(errorMsg)
            Result.failure(e)
        }
    }

    private fun getBrigadierCollection(brigadierId: String, collectionName: String) =
        firestore.collection("brigadiers").document(brigadierId).collection(collectionName)

    /**
     * Загрузка данных из Firestore в локальный Room для конкретного бригадира
     */
    private suspend fun downloadFromFirestore(brigadierId: String) {
        // 1. Projects
        val downloadedProjectIds = mutableSetOf<Int>()
        try {
            var projectsSnap = getBrigadierCollection(brigadierId, "projects").get().await()
            if (projectsSnap.isEmpty) {
                // Fallback: check legacy root collection if migrating
                projectsSnap = firestore.collection("projects").get().await()
            }
            val projects = projectsSnap.documents.mapNotNull { doc ->
                val id = (doc.getLong("id") ?: doc.id.toLongOrNull())?.toInt() ?: return@mapNotNull null
                val docBId = doc.getString("brigadierId") ?: ""
                if (docBId.isNotBlank() && docBId != brigadierId) return@mapNotNull null

                val name = doc.getString("name") ?: ""
                val cost = doc.getDouble("cost") ?: (doc.getLong("cost")?.toDouble() ?: 0.0)
                val startDate = doc.getString("startDate")
                val endDate = doc.getString("endDate")
                val isArchived = doc.getBoolean("isArchived") ?: false
                Project(id = id, name = name, cost = cost, startDate = startDate, endDate = endDate, isArchived = isArchived, brigadierId = brigadierId)
            }
            if (projects.isNotEmpty()) {
                projectDao.insertAll(projects)
                downloadedProjectIds.addAll(projects.map { it.id })
            }
        } catch (e: Exception) {
            Log.w(TAG, "Projects download warning: ${e.message}")
        }

        // 2. Employees
        // downloadedEmployeeIds used below to drop attendance/project_employees/expenses/payments
        // docs pointing at an employee we don't have: Room's FK on employeeId is CASCADE, and
        // upsertAll/insertAll run as one transaction, so a single stale/orphaned doc would throw
        // and silently roll back the ENTIRE collection for that sync (caught by the try/catch below).
        val downloadedEmployeeIds = mutableSetOf<Int>()
        try {
            var empSnap = getBrigadierCollection(brigadierId, "employees").get().await()
            if (empSnap.isEmpty) {
                empSnap = firestore.collection("employees").get().await()
            }
            val employees = empSnap.documents.mapNotNull { doc ->
                val id = (doc.getLong("id") ?: doc.id.toLongOrNull())?.toInt() ?: return@mapNotNull null
                val docBId = doc.getString("brigadierId") ?: ""
                if (docBId.isNotBlank() && docBId != brigadierId) return@mapNotNull null

                val name = doc.getString("name") ?: ""
                val phone = doc.getString("phone")
                val isFired = doc.getBoolean("isFired") ?: false
                Employee(id = id, name = name, phone = phone, isFired = isFired, brigadierId = brigadierId)
            }
            if (employees.isNotEmpty()) {
                employeeDao.insertAll(employees)
                downloadedEmployeeIds.addAll(employees.map { it.id })
            }
        } catch (e: Exception) {
            Log.w(TAG, "Employees download warning: ${e.message}")
        }

        // 3. Project Employees
        // Эслатма: бу ерда ва пастдаги давомат/пул берди/харажат/тўлов бўлимларида
        // ЭСКИ умумий (brigadier'га боғланмаган) коллекцияга fallback қилинмайди —
        // чунки бу ёзувларда brigadierId йўқ ва projectId локал (қурилмадаги) autoincrement
        // бўлгани учун ҳар хил бригадирларнинг рақамлари тасодифан мос келиб қолиши мумкин,
        // бу эса бошқа бригадирнинг молиявий маълумотлари сизникига "сизиб ўтишига" олиб келади.
        try {
            val peSnap = getBrigadierCollection(brigadierId, "project_employees").get().await()
            val peList = peSnap.documents.mapNotNull { doc ->
                val idParts = doc.id.split("_")
                val projectId = (doc.getLong("projectId") ?: doc.getString("projectId")?.toLongOrNull())?.toInt()
                    ?: idParts.getOrNull(0)?.toIntOrNull()
                val employeeId = (doc.getLong("employeeId") ?: doc.getString("employeeId")?.toLongOrNull())?.toInt()
                    ?: idParts.getOrNull(1)?.toIntOrNull()
                if (projectId != null && employeeId != null) {
                    if (downloadedProjectIds.isNotEmpty() && !downloadedProjectIds.contains(projectId)) return@mapNotNull null
                    if (downloadedEmployeeIds.isNotEmpty() && !downloadedEmployeeIds.contains(employeeId)) return@mapNotNull null
                    ProjectEmployee(projectId = projectId, employeeId = employeeId)
                } else null
            }
            if (peList.isNotEmpty()) {
                projectEmployeeDao.insertAll(peList)
            }
        } catch (e: Exception) {
            Log.w(TAG, "ProjectEmployees download warning: ${e.message}")
        }

        // 4. Attendance
        try {
            val attSnap = getBrigadierCollection(brigadierId, "attendance").get().await()
            val attendanceList = attSnap.documents.mapNotNull { doc ->
                val idParts = doc.id.split("_")
                val projectId = (doc.getLong("projectId") ?: doc.getString("projectId")?.toLongOrNull())?.toInt()
                    ?: idParts.getOrNull(0)?.toIntOrNull()
                val employeeId = (doc.getLong("employeeId") ?: doc.getString("employeeId")?.toLongOrNull())?.toInt()
                    ?: idParts.getOrNull(1)?.toIntOrNull()
                val date = doc.getString("date")
                    ?: idParts.getOrNull(2)
                val present = doc.getBoolean("present")
                    ?: (doc.getLong("present")?.let { it != 0L })
                    ?: (doc.getString("present")?.toBooleanStrictOrNull())
                    ?: false
                if (projectId != null && employeeId != null && !date.isNullOrBlank()) {
                    if (downloadedProjectIds.isNotEmpty() && !downloadedProjectIds.contains(projectId)) return@mapNotNull null
                    if (downloadedEmployeeIds.isNotEmpty() && !downloadedEmployeeIds.contains(employeeId)) return@mapNotNull null
                    Attendance(projectId = projectId, employeeId = employeeId, date = date, present = present)
                } else null
            }
            if (attendanceList.isNotEmpty()) {
                attendanceDao.upsertAll(attendanceList)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Attendance download warning: ${e.message}")
        }

        // 5. Bonuses
        try {
            val bonusSnap = getBrigadierCollection(brigadierId, "bonuses").get().await()
            val bonuses = bonusSnap.documents.mapNotNull { doc ->
                val id = (doc.getLong("id") ?: doc.id.toLongOrNull())?.toInt() ?: return@mapNotNull null
                val projectId = doc.getLong("projectId")?.toInt() ?: return@mapNotNull null
                if (downloadedProjectIds.isNotEmpty() && !downloadedProjectIds.contains(projectId)) return@mapNotNull null
                val amount = doc.getDouble("amount") ?: 0.0
                val description = doc.getString("description")
                val date = doc.getString("date") ?: ""
                val distributionNote = doc.getString("distributionNote")
                val unallocatedAmount = doc.getDouble("unallocatedAmount") ?: 0.0
                Bonus(id = id, projectId = projectId, amount = amount, description = description, date = date, distributionNote = distributionNote, unallocatedAmount = unallocatedAmount)
            }
            if (bonuses.isNotEmpty()) {
                bonusDao.insertAll(bonuses)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Bonuses download warning: ${e.message}")
        }

        // 6. Expenses
        try {
            val expSnap = getBrigadierCollection(brigadierId, "expenses").get().await()
            val expenses = expSnap.documents.mapNotNull { doc ->
                val id = (doc.getLong("id") ?: doc.id.toLongOrNull())?.toInt() ?: return@mapNotNull null
                val projectId = doc.getLong("projectId")?.toInt() ?: return@mapNotNull null
                if (downloadedProjectIds.isNotEmpty() && !downloadedProjectIds.contains(projectId)) return@mapNotNull null
                val amount = doc.getDouble("amount") ?: 0.0
                val description = doc.getString("description")
                val date = doc.getString("date") ?: ""
                val employeeId = doc.getLong("employeeId")?.toInt()
                if (employeeId != null && downloadedEmployeeIds.isNotEmpty() && !downloadedEmployeeIds.contains(employeeId)) return@mapNotNull null
                Expense(id = id, projectId = projectId, amount = amount, description = description, date = date, employeeId = employeeId)
            }
            if (expenses.isNotEmpty()) {
                expenseDao.insertAll(expenses)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Expenses download warning: ${e.message}")
        }

        // 7. Payments
        try {
            val paySnap = getBrigadierCollection(brigadierId, "payments").get().await()
            val payments = paySnap.documents.mapNotNull { doc ->
                val id = (doc.getLong("id") ?: doc.id.toLongOrNull())?.toInt() ?: return@mapNotNull null
                val employeeId = doc.getLong("employeeId")?.toInt() ?: return@mapNotNull null
                if (downloadedEmployeeIds.isNotEmpty() && !downloadedEmployeeIds.contains(employeeId)) return@mapNotNull null
                val projectId = doc.getLong("projectId")?.toInt()
                if (projectId != null && downloadedProjectIds.isNotEmpty() && !downloadedProjectIds.contains(projectId)) return@mapNotNull null
                val amount = doc.getDouble("amount") ?: 0.0
                val date = doc.getString("date") ?: ""
                val description = doc.getString("description")
                Payment(id = id, employeeId = employeeId, projectId = projectId, amount = amount, date = date, description = description)
            }
            if (payments.isNotEmpty()) {
                paymentDao.insertAll(payments)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Payments download warning: ${e.message}")
        }

        // 8. App Users
        try {
            val usersSnap = getBrigadierCollection(brigadierId, "app_users").get().await()
            val users = usersSnap.documents.mapNotNull { doc ->
                val loginId = doc.getString("loginId") ?: doc.id.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                val password = doc.getString("password") ?: return@mapNotNull null
                val name = doc.getString("name") ?: loginId
                val roleStr = doc.getString("role") ?: "WORKER"
                val role = try { UserRole.valueOf(roleStr) } catch (e: Exception) { UserRole.WORKER }
                val employeeId = doc.getLong("employeeId")?.toInt()
                val phone = doc.getString("phone")
                AppUser(loginId = loginId, password = password, name = name, role = role, employeeId = employeeId, phone = phone, brigadierId = brigadierId)
            }
            if (users.isNotEmpty()) {
                appUserDao.insertAll(users)
            }
        } catch (e: Exception) {
            Log.w(TAG, "AppUsers download warning: ${e.message}")
        }

        // 9. Brigadier Document & Settings
        try {
            val bDoc = firestore.collection("brigadiers").document(brigadierId).get().await()
            if (bDoc.exists()) {
                val showEarnings = bDoc.getBoolean("showWorkerEarningsAndDebt") ?: false
                val authPrefs = context.getSharedPreferences("daftarcha_auth_prefs", Context.MODE_PRIVATE)
                authPrefs.edit()
                    .putBoolean("show_worker_earnings_and_debt_$brigadierId", showEarnings)
                    .putBoolean("show_worker_earnings_and_debt_global", showEarnings)
                    .apply()
            }
            val globalDoc = firestore.collection("app_settings").document("worker_visibility").get().await()
            if (globalDoc.exists()) {
                val showEarnings = globalDoc.getBoolean("showWorkerEarningsAndDebt") ?: false
                val authPrefs = context.getSharedPreferences("daftarcha_auth_prefs", Context.MODE_PRIVATE)
                authPrefs.edit().putBoolean("show_worker_earnings_and_debt_global", showEarnings).apply()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Brigadier doc download error: ${e.message}")
        }
    }

    /**
     * Выгрузка локальных данных Room в Firestore для конкретного бригадира
     */
    private suspend fun uploadToFirestore(brigadierId: String): SyncSummary {
        // 0. Brigadier document & settings
        try {
            val authPrefs = context.getSharedPreferences("daftarcha_auth_prefs", Context.MODE_PRIVATE)
            val showEarnings = authPrefs.getBoolean(
                "show_worker_earnings_and_debt_$brigadierId",
                authPrefs.getBoolean("show_worker_earnings_and_debt_global", false)
            )
            val bMap = hashMapOf<String, Any>(
                "id" to brigadierId,
                "showWorkerEarningsAndDebt" to showEarnings,
                "updatedAt" to System.currentTimeMillis()
            )
            firestore.collection("brigadiers").document(brigadierId).set(bMap, SetOptions.merge()).await()
            firestore.collection("app_settings").document("worker_visibility")
                .set(mapOf("showWorkerEarningsAndDebt" to showEarnings, "updatedAt" to System.currentTimeMillis()), SetOptions.merge()).await()
        } catch (e: Exception) {
            Log.w(TAG, "Brigadier document upload warning: ${e.message}")
        }

        // 1. Projects of this brigadier
        val localProjects = projectDao.getProjectsForBrigadierList(brigadierId)
        val projectIds = localProjects.map { it.id }.toSet()

        localProjects.chunked(400).forEach { chunk ->
            val batch = firestore.batch()
            chunk.forEach { p ->
                val ref = getBrigadierCollection(brigadierId, "projects").document(p.id.toString())
                val map = hashMapOf(
                    "id" to p.id,
                    "name" to p.name,
                    "cost" to p.cost,
                    "startDate" to p.startDate,
                    "endDate" to p.endDate,
                    "isArchived" to p.isArchived,
                    "brigadierId" to brigadierId,
                    "updatedAt" to System.currentTimeMillis()
                )
                batch.set(ref, map, SetOptions.merge())
            }
            batch.commit().await()
        }

        // 2. Employees of this brigadier
        val localEmployees = employeeDao.getAllEmployeesForBrigadierSuspend(brigadierId)
        val employeeIds = localEmployees.map { it.id }.toSet()

        localEmployees.chunked(400).forEach { chunk ->
            val batch = firestore.batch()
            chunk.forEach { e ->
                val ref = getBrigadierCollection(brigadierId, "employees").document(e.id.toString())
                val map = hashMapOf(
                    "id" to e.id,
                    "name" to e.name,
                    "phone" to e.phone,
                    "isFired" to e.isFired,
                    "brigadierId" to brigadierId,
                    "updatedAt" to System.currentTimeMillis()
                )
                batch.set(ref, map, SetOptions.merge())
            }
            batch.commit().await()
        }

        // 3. Project Employees
        val localPE = projectEmployeeDao.getAllProjectEmployees()
            .filter { projectIds.contains(it.projectId) }
        localPE.chunked(400).forEach { chunk ->
            val batch = firestore.batch()
            chunk.forEach { pe ->
                val ref = getBrigadierCollection(brigadierId, "project_employees").document("${pe.projectId}_${pe.employeeId}")
                val map = hashMapOf(
                    "projectId" to pe.projectId,
                    "employeeId" to pe.employeeId,
                    "updatedAt" to System.currentTimeMillis()
                )
                batch.set(ref, map, SetOptions.merge())
            }
            batch.commit().await()
        }

        // 4. Attendance
        val localAttendance = attendanceDao.getAllAttendance()
            .filter { projectIds.contains(it.projectId) }
        localAttendance.chunked(400).forEach { chunk ->
            val batch = firestore.batch()
            chunk.forEach { att ->
                val ref = getBrigadierCollection(brigadierId, "attendance").document("${att.projectId}_${att.employeeId}_${att.date}")
                val map = hashMapOf(
                    "projectId" to att.projectId,
                    "employeeId" to att.employeeId,
                    "date" to att.date,
                    "present" to att.present,
                    "brigadierId" to brigadierId,
                    "updatedAt" to System.currentTimeMillis()
                )
                batch.set(ref, map, SetOptions.merge())
            }
            batch.commit().await()
        }

        // 5. Bonuses
        val localBonuses = bonusDao.getAllBonuses()
            .filter { projectIds.contains(it.projectId) }
        localBonuses.chunked(400).forEach { chunk ->
            val batch = firestore.batch()
            chunk.forEach { b ->
                val ref = getBrigadierCollection(brigadierId, "bonuses").document(b.id.toString())
                val map = hashMapOf(
                    "id" to b.id,
                    "projectId" to b.projectId,
                    "amount" to b.amount,
                    "description" to b.description,
                    "date" to b.date,
                    "distributionNote" to b.distributionNote,
                    "unallocatedAmount" to b.unallocatedAmount
                )
                batch.set(ref, map, SetOptions.merge())
            }
            batch.commit().await()
        }

        // 6. Expenses
        val localExpenses = expenseDao.getAllExpenses()
            .filter { projectIds.contains(it.projectId) }
        localExpenses.chunked(400).forEach { chunk ->
            val batch = firestore.batch()
            chunk.forEach { ex ->
                val ref = getBrigadierCollection(brigadierId, "expenses").document(ex.id.toString())
                val map = hashMapOf(
                    "id" to ex.id,
                    "projectId" to ex.projectId,
                    "amount" to ex.amount,
                    "description" to ex.description,
                    "date" to ex.date,
                    "employeeId" to ex.employeeId
                )
                batch.set(ref, map, SetOptions.merge())
            }
            batch.commit().await()
        }

        // 7. Payments
        val localPayments = paymentDao.getAllPayments()
            .filter { employeeIds.contains(it.employeeId) || (it.projectId != null && projectIds.contains(it.projectId)) }
        localPayments.chunked(400).forEach { chunk ->
            val batch = firestore.batch()
            chunk.forEach { pay ->
                val ref = getBrigadierCollection(brigadierId, "payments").document(pay.id.toString())
                val map = hashMapOf(
                    "id" to pay.id,
                    "employeeId" to pay.employeeId,
                    "projectId" to pay.projectId,
                    "amount" to pay.amount,
                    "date" to pay.date,
                    "description" to pay.description
                )
                batch.set(ref, map, SetOptions.merge())
            }
            batch.commit().await()
        }

        // 8. App Users of this brigadier
        val localUsers = appUserDao.getAllUsers()
            .filter { it.brigadierId == brigadierId || it.loginId == brigadierId }
        localUsers.chunked(400).forEach { chunk ->
            val batch = firestore.batch()
            chunk.forEach { u ->
                val ref = getBrigadierCollection(brigadierId, "app_users").document(u.loginId)
                val map = hashMapOf(
                    "loginId" to u.loginId,
                    "password" to u.password,
                    "name" to u.name,
                    "role" to u.role.name,
                    "employeeId" to u.employeeId,
                    "phone" to u.phone,
                    "brigadierId" to brigadierId
                )
                batch.set(ref, map, SetOptions.merge())
            }
            batch.commit().await()
        }

        return SyncSummary(
            projectsCount = localProjects.size,
            employeesCount = localEmployees.size,
            attendanceCount = localAttendance.size,
            paymentsCount = localPayments.size
        )
    }

    /**
     * Быстрое сохранение посещаемости (Room + Firestore в реальном времени)
     */
    suspend fun saveAttendance(attendance: Attendance) = withContext(Dispatchers.IO) {
        attendanceDao.upsert(attendance)
        try {
            val bId = getCurrentBrigadierId()
            val collection = if (bId.isNotBlank()) {
                getBrigadierCollection(bId, "attendance")
            } else {
                firestore.collection("attendance")
            }
            val ref = collection.document("${attendance.projectId}_${attendance.employeeId}_${attendance.date}")
            val map = hashMapOf(
                "projectId" to attendance.projectId,
                "employeeId" to attendance.employeeId,
                "date" to attendance.date,
                "present" to attendance.present,
                "brigadierId" to bId,
                "updatedAt" to System.currentTimeMillis()
            )
            ref.set(map, SetOptions.merge())
        } catch (e: Exception) {
            Log.w(TAG, "Failed to upload attendance: ${e.message}")
        }
    }

    /**
     * Быстрое сохранение списка посещаемости (Room + Firestore в реальном времени)
     */
    suspend fun saveAttendanceList(list: List<Attendance>) = withContext(Dispatchers.IO) {
        attendanceDao.upsertAll(list)
        try {
            val bId = getCurrentBrigadierId()
            val collection = if (bId.isNotBlank()) {
                getBrigadierCollection(bId, "attendance")
            } else {
                firestore.collection("attendance")
            }
            list.chunked(400).forEach { chunk ->
                val batch = firestore.batch()
                chunk.forEach { attendance ->
                    val ref = collection.document("${attendance.projectId}_${attendance.employeeId}_${attendance.date}")
                    val map = hashMapOf(
                        "projectId" to attendance.projectId,
                        "employeeId" to attendance.employeeId,
                        "date" to attendance.date,
                        "present" to attendance.present,
                        "brigadierId" to bId,
                        "updatedAt" to System.currentTimeMillis()
                    )
                    batch.set(ref, map, SetOptions.merge())
                }
                batch.commit().await()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to upload attendance batch: ${e.message}")
        }
    }

    /**
     * Быстрое сохранение связи Проект - Шерик (Room + Firestore в реальном времени)
     */
    suspend fun saveProjectEmployee(pe: ProjectEmployee) = withContext(Dispatchers.IO) {
        projectEmployeeDao.insert(pe)
        try {
            val bId = getCurrentBrigadierId()
            val collection = if (bId.isNotBlank()) {
                getBrigadierCollection(bId, "project_employees")
            } else {
                firestore.collection("project_employees")
            }
            val ref = collection.document("${pe.projectId}_${pe.employeeId}")
            val map = hashMapOf(
                "projectId" to pe.projectId,
                "employeeId" to pe.employeeId,
                "updatedAt" to System.currentTimeMillis()
            )
            ref.set(map, SetOptions.merge())
        } catch (e: Exception) {
            Log.w(TAG, "Failed to upload project employee: ${e.message}")
        }
    }

    /**
     * Быстрое сохранение списка связей Проект - Шерик
     */
    suspend fun saveProjectEmployeesList(list: List<ProjectEmployee>) = withContext(Dispatchers.IO) {
        projectEmployeeDao.insertAll(list)
        try {
            val bId = getCurrentBrigadierId()
            val collection = if (bId.isNotBlank()) {
                getBrigadierCollection(bId, "project_employees")
            } else {
                firestore.collection("project_employees")
            }
            list.chunked(400).forEach { chunk ->
                val batch = firestore.batch()
                chunk.forEach { pe ->
                    val ref = collection.document("${pe.projectId}_${pe.employeeId}")
                    val map = hashMapOf(
                        "projectId" to pe.projectId,
                        "employeeId" to pe.employeeId,
                        "updatedAt" to System.currentTimeMillis()
                    )
                    batch.set(ref, map, SetOptions.merge())
                }
                batch.commit().await()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to upload project employees batch: ${e.message}")
        }
    }

    /**
     * Удаление проекта из облака при его полном удалении
     */
    suspend fun deleteProjectFromCloud(projectId: Int) = withContext(Dispatchers.IO) {
        try {
            val bId = getCurrentBrigadierId()
            if (bId.isNotBlank()) {
                getBrigadierCollection(bId, "projects").document(projectId.toString()).delete().await()
            }
            firestore.collection("projects").document(projectId.toString()).delete().await()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to delete project $projectId from cloud: ${e.message}")
        }
    }

    /**
     * Удаление сотрудника из облака
     */
    suspend fun deleteEmployeeFromCloud(employeeId: Int) = withContext(Dispatchers.IO) {
        try {
            val bId = getCurrentBrigadierId()
            if (bId.isNotBlank()) {
                getBrigadierCollection(bId, "employees").document(employeeId.toString()).delete().await()
            }
            firestore.collection("employees").document(employeeId.toString()).delete().await()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to delete employee $employeeId from cloud: ${e.message}")
        }
    }

    /**
     * Очистка начислений (посещаемости) и выплат для сотрудника
     */
    suspend fun deleteAttendanceAndPaymentsForEmployee(employeeId: Int) = withContext(Dispatchers.IO) {
        try {
            val bId = getCurrentBrigadierId()
            val attCollection = if (bId.isNotBlank()) getBrigadierCollection(bId, "attendance") else firestore.collection("attendance")
            val payCollection = if (bId.isNotBlank()) getBrigadierCollection(bId, "payments") else firestore.collection("payments")

            // 1. Удалить посещаемость сотрудника из Firestore
            val attDocs = attCollection.whereEqualTo("employeeId", employeeId).get().await()
            attDocs.documents.chunked(400).forEach { chunk ->
                val batch = firestore.batch()
                chunk.forEach { doc -> batch.delete(doc.reference) }
                batch.commit().await()
            }

            // 2. Удалить выплаты сотрудника из Firestore
            val payDocs = payCollection.whereEqualTo("employeeId", employeeId).get().await()
            payDocs.documents.chunked(400).forEach { chunk ->
                val batch = firestore.batch()
                chunk.forEach { doc -> batch.delete(doc.reference) }
                batch.commit().await()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to delete attendance and payments from cloud for employee $employeeId: ${e.message}")
        }
    }

    /**
     * Удаление отдельной выплаты из облака
     */
    suspend fun deletePaymentFromCloud(paymentId: Int) = withContext(Dispatchers.IO) {
        try {
            val bId = getCurrentBrigadierId()
            if (bId.isNotBlank()) {
                getBrigadierCollection(bId, "payments").document(paymentId.toString()).delete().await()
            }
            firestore.collection("payments").document(paymentId.toString()).delete().await()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to delete payment $paymentId from cloud: ${e.message}")
        }
    }
}
