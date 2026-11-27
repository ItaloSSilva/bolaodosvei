package com.bolaodosvei.app.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.bolaodosvei.app.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs

class AppRepository(context: Context) {

    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences("bolao_dos_vei_prefs_v2", Context.MODE_PRIVATE)

    // --- Authentication & Local Persistence ---

    fun getCurrentUser(): UserAccount? {
        val currentId = prefs.getString("current_user_id", null) ?: return null
        return getAllUsers().firstOrNull { it.id == currentId }
    }

    fun getAllUsers(): List<UserAccount> {
        val raw = prefs.getString("users_json", null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                UserAccount(
                    id = obj.getString("id"),
                    name = obj.getString("name"),
                    email = obj.getString("email"),
                    password = obj.getString("password"),
                    avatarColorHex = obj.optLong("avatarColorHex", 0xFF00C853L),
                    favoriteLeague = League.entries.firstOrNull {
                        it.id == obj.optString("favoriteLeague")
                    } ?: League.BRASILEIRAO,
                    avatarImagePath = obj.optString("avatarImagePath", "").takeIf { it.isNotBlank() }
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun saveUsers(users: List<UserAccount>) {
        val arr = JSONArray()
        users.forEach { u ->
            arr.put(JSONObject().apply {
                put("id", u.id)
                put("name", u.name)
                put("email", u.email)
                put("password", u.password)
                put("avatarColorHex", u.avatarColorHex)
                put("favoriteLeague", u.favoriteLeague.id)
                if (!u.avatarImagePath.isNullOrBlank()) {
                    put("avatarImagePath", u.avatarImagePath)
                }
            })
        }
        prefs.edit().putString("users_json", arr.toString()).apply()
    }

    fun registerUser(
        name: String,
        email: String,
        password: String,
        avatarColorHex: Long,
        favoriteLeague: League
    ): Result<UserAccount> {
        val cleanEmail = email.trim().lowercase()
        val cleanName = name.trim()
        if (cleanName.length < 2) return Result.failure(IllegalArgumentException("Digite seu nome ou apelido."))
        if (!cleanEmail.contains("@")) return Result.failure(IllegalArgumentException("Digite um e-mail válido."))
        if (password.length < 4) return Result.failure(IllegalArgumentException("A senha deve ter pelo menos 4 caracteres."))

        val existing = getAllUsers()
        if (existing.any { it.email.equals(cleanEmail, ignoreCase = true) }) {
            return Result.failure(IllegalArgumentException("Já existe uma conta com este e-mail."))
        }

        val stableId = cleanEmail.replace(Regex("[^a-z0-9]"), "_")
        val user = UserAccount(
            id = "u_$stableId",
            name = cleanName,
            email = cleanEmail,
            password = hashPassword(password),
            avatarColorHex = avatarColorHex,
            favoriteLeague = favoriteLeague
        )
        saveUsers(existing + user)
        prefs.edit().putString("current_user_id", user.id).apply()
        return Result.success(user)
    }

    private fun hashPassword(raw: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray(Charsets.UTF_8))
        val hex = digest.joinToString("") { "%02x".format(it) }
        return "sha256:$hex"
    }

    fun loginUser(email: String, password: String): Result<UserAccount> {
        val cleanEmail = email.trim().lowercase()
        val allUsers = getAllUsers()
        val user = allUsers.firstOrNull { u ->
            u.email.equals(cleanEmail, ignoreCase = true) &&
                if (u.password.startsWith("sha256:")) u.password == hashPassword(password)
                else u.password == password
        }
        return if (user != null) {
            val migrated = if (!user.password.startsWith("sha256:")) {
                val updated = user.copy(password = hashPassword(password))
                saveUsers(allUsers.map { if (it.id == user.id) updated else it })
                updated
            } else {
                user
            }
            prefs.edit().putString("current_user_id", migrated.id).apply()
            Result.success(migrated)
        } else {
            Result.failure(IllegalArgumentException("E-mail ou senha incorretos."))
        }
    }

    fun logout() {
        prefs.edit().remove("current_user_id").apply()
    }

    fun updateUserProfile(
        userId: String,
        newName: String,
        newColorHex: Long,
        newFavoriteLeague: League
    ): Result<UserAccount> {
        val cleanName = newName.trim()
        if (cleanName.length < 2) {
            return Result.failure(IllegalArgumentException("Digite um nome com pelo menos 2 letras."))
        }
        val all = getAllUsers()
        val target = all.firstOrNull { it.id == userId }
            ?: return Result.failure(IllegalStateException("Conta não encontrada."))
        val updated = target.copy(
            name = cleanName,
            avatarColorHex = newColorHex,
            favoriteLeague = newFavoriteLeague
        )
        saveUsers(all.map { if (it.id == userId) updated else it })
        return Result.success(updated)
    }

    fun saveProfilePhotoFromUri(userId: String, uri: Uri): String? {
        return try {
            val bytes = appContext.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
            val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
            val maxDim = 512
            val scale = minOf(1f, maxDim.toFloat() / maxOf(decoded.width, decoded.height).toFloat())
            val finalBitmap = if (scale < 1f) {
                Bitmap.createScaledBitmap(
                    decoded,
                    (decoded.width * scale).toInt().coerceAtLeast(1),
                    (decoded.height * scale).toInt().coerceAtLeast(1),
                    true
                )
            } else {
                decoded
            }
            val dir = File(appContext.filesDir, "avatars").apply { mkdirs() }
            val outFile = File(dir, "avatar_${userId}_${System.currentTimeMillis()}.jpg")
            FileOutputStream(outFile).use { fos ->
                finalBitmap.compress(Bitmap.CompressFormat.JPEG, 88, fos)
            }
            if (finalBitmap !== decoded) decoded.recycle()
            finalBitmap.recycle()
            outFile.absolutePath
        } catch (_: Exception) {
            null
        }
    }

    fun updateUserAvatarPhoto(userId: String, photoPath: String?): UserAccount? {
        val all = getAllUsers()
        val target = all.firstOrNull { it.id == userId } ?: return null
        val updated = target.copy(avatarImagePath = photoPath)
        saveUsers(all.map { if (it.id == userId) updated else it })
        return updated
    }

    // --- App Theme Persistence ---

    fun loadSelectedThemeId(): String {
        return prefs.getString("selected_theme_id", "default_black") ?: "default_black"
    }

    fun saveSelectedThemeId(themeId: String) {
        prefs.edit().putString("selected_theme_id", themeId).apply()
    }

    // --- Predictions Persistence ---

    fun loadPredictions(userId: String): Map<String, Prediction> {
        val raw = prefs.getString("predictions_$userId", null) ?: return emptyMap()
        return try {
            val obj = JSONObject(raw)
            val result = mutableMapOf<String, Prediction>()
            val keys = obj.keys()
            while (keys.hasNext()) {
                val matchId = keys.next()
                try {
                    val p = obj.optJSONObject(matchId) ?: continue
                    if (!p.has("home") || !p.has("away")) continue
                    result[matchId] = Prediction(
                        matchId = matchId,
                        homeScore = p.getInt("home"),
                        awayScore = p.getInt("away"),
                        isJoker = p.optBoolean("joker", false)
                    )
                } catch (_: Exception) {
                    // Skip malformed entry without discarding valid predictions
                }
            }
            result
        } catch (_: Exception) {
            emptyMap()
        }
    }

    fun savePredictions(userId: String, predictions: Map<String, Prediction>) {
        val obj = JSONObject()
        predictions.forEach { (matchId, pred) ->
            obj.put(matchId, JSONObject().apply {
                put("home", pred.homeScore)
                put("away", pred.awayScore)
                put("joker", pred.isJoker)
            })
        }
        prefs.edit().putString("predictions_$userId", obj.toString()).apply()
    }

    // --- Room Sync (Only Room Code Needed by Friends) ---

    fun loadRoomCode(): String {
        return prefs.getString("active_room_code", "") ?: ""
    }

    fun saveRoomCode(roomCode: String): String {
        val clean = roomCode.trim().uppercase().replace(Regex("[^A-Z0-9_-]"), "")
        prefs.edit().putString("active_room_code", clean).apply()
        return clean
    }

    fun generateRoomCode(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        val suffix = (1..4).map { chars[(Math.random() * chars.length).toInt()] }.joinToString("")
        return "VEI-$suffix"
    }

    fun hasSyncServer(): Boolean = AppConfig.SYNC_BASE_URL.trim().startsWith("http")

    private fun syncSuffix(baseUrl: String): String =
        if (baseUrl.contains("firebaseio.com", ignoreCase = true)) ".json" else ""

    suspend fun pushUserBolaoToRoom(
        roomCode: String,
        user: UserAccount,
        predictions: Map<String, Prediction>,
        equippedFrameId: String? = null,
        equippedTitle: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        val baseUrl = AppConfig.SYNC_BASE_URL.trim().trimEnd('/')
        val room = roomCode.trim().uppercase()
        if (!baseUrl.startsWith("http") || room.isEmpty()) return@withContext false

        val endpoint = "$baseUrl/salas/$room/participantes/${user.id}${syncSuffix(baseUrl)}"

        val predsObj = JSONObject()
        predictions.forEach { (matchId, p) ->
            predsObj.put(matchId, JSONObject().apply {
                put("home", p.homeScore)
                put("away", p.awayScore)
                put("joker", p.isJoker)
            })
        }

        val payload = JSONObject().apply {
            put("id", user.id)
            put("name", user.name)
            put("avatarColorHex", user.avatarColorHex)
            put("favoriteLeague", user.favoriteLeague.shortName)
            if (!equippedFrameId.isNullOrBlank()) put("equippedFrameId", equippedFrameId)
            if (!equippedTitle.isNullOrBlank()) put("equippedTitle", equippedTitle)
            put("updatedAt", System.currentTimeMillis())
            put("predictions", predsObj)
        }

        return@withContext try {
            val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                requestMethod = "PUT"
                connectTimeout = 5000
                readTimeout = 5000
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            }
            OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { writer ->
                writer.write(payload.toString())
            }
            conn.responseCode in 200..299
        } catch (_: Exception) {
            false
        }
    }

    suspend fun fetchRoomParticipants(
        roomCode: String,
        currentUserId: String?
    ): List<FriendBolao>? = withContext(Dispatchers.IO) {
        val baseUrl = AppConfig.SYNC_BASE_URL.trim().trimEnd('/')
        val room = roomCode.trim().uppercase()
        if (!baseUrl.startsWith("http") || room.isEmpty()) return@withContext null

        val endpoint = "$baseUrl/salas/$room/participantes${syncSuffix(baseUrl)}"

        val body = httpGet(endpoint) ?: return@withContext null
        if (body.trim() == "null" || body.isBlank()) return@withContext emptyList()

        return@withContext try {
            val root = JSONObject(body)
            val list = mutableListOf<FriendBolao>()
            val keys = root.keys()
            while (keys.hasNext()) {
                val uid = keys.next()
                val obj = root.optJSONObject(uid) ?: continue
                val predsMap = mutableMapOf<String, Prediction>()
                val predsObj = obj.optJSONObject("predictions")
                if (predsObj != null) {
                    val pKeys = predsObj.keys()
                    while (pKeys.hasNext()) {
                        val mId = pKeys.next()
                        val p = predsObj.optJSONObject(mId) ?: continue
                        predsMap[mId] = Prediction(
                            matchId = mId,
                            homeScore = p.optInt("home", 0),
                            awayScore = p.optInt("away", 0),
                            isJoker = p.optBoolean("joker", false)
                        )
                    }
                }
                val isMe = uid == currentUserId
                val rawName = obj.optString("name", "Participante")
                list.add(
                    FriendBolao(
                        id = uid,
                        name = if (isMe) "$rawName (Você)" else rawName,
                        subtitle = "Sala $room • ${obj.optString("favoriteLeague", "Bolão")}",
                        avatarColorHex = obj.optLong("avatarColorHex", 0xFF00C853L),
                        predictions = predsMap,
                        isCurrentUser = isMe,
                        equippedFrameId = obj.optString("equippedFrameId", "").takeIf { it.isNotBlank() },
                        equippedTitle = obj.optString("equippedTitle", "").takeIf { it.isNotBlank() }
                    )
                )
            }
            list
        } catch (_: Exception) {
            null
        }
    }

    // --- Real-Users-Only Local List (Zero Fake Profiles) ---

    fun buildLocalRealParticipants(
        currentUser: UserAccount?,
        currentPredictions: Map<String, Prediction>
    ): List<FriendBolao> {
        val list = mutableListOf<FriendBolao>()
        getAllUsers().forEach { user ->
            val isMe = user.id == currentUser?.id
            val preds = if (isMe) currentPredictions else loadPredictions(user.id)
            list.add(
                FriendBolao(
                    id = user.id,
                    name = if (isMe) "${user.name} (Você)" else user.name,
                    subtitle = if (isMe) "Sua conta • ${user.favoriteLeague.shortName}" else "Conta neste aparelho",
                    avatarColorHex = user.avatarColorHex,
                    predictions = preds,
                    isCurrentUser = isMe,
                    avatarImagePath = user.avatarImagePath
                )
            )
        }
        return list
    }

    // --- Notification Settings ---

    fun loadNotificationSettings(): NotificationSettings {
        val map = League.entries.associateWith { league ->
            prefs.getBoolean("notif_${league.id}", true)
        }
        return NotificationSettings(map)
    }

    fun saveNotificationToggle(league: League, enabled: Boolean) {
        prefs.edit().putBoolean("notif_${league.id}", enabled).apply()
    }

    // --- Points Shop Persistence ---

    private data class StoredShopData(
        val ownedItemIds: Set<String>,
        val equippedFrameId: String?,
        val equippedTitleId: String?,
        val equippedFlairId: String?,
        val spentPoints: Int,
        val creditedMatchPoints: Map<String, Int>
    )

    private fun readStoredShopData(userId: String): StoredShopData {
        val raw = prefs.getString("shop_state_$userId", null)
            ?: return StoredShopData(emptySet(), null, null, null, 0, emptyMap())
        return try {
            val obj = JSONObject(raw)
            val ownedArr = obj.optJSONArray("ownedItemIds")
            val owned = mutableSetOf<String>()
            if (ownedArr != null) {
                for (i in 0 until ownedArr.length()) {
                    val id = ownedArr.optString(i, "")
                    if (id.isNotBlank()) owned.add(id)
                }
            }
            val creditedObj = obj.optJSONObject("creditedMatchPoints")
            val credited = mutableMapOf<String, Int>()
            if (creditedObj != null) {
                val keys = creditedObj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    credited[k] = creditedObj.optInt(k, 0)
                }
            }
            StoredShopData(
                ownedItemIds = owned,
                equippedFrameId = obj.optString("equippedFrameId", "").takeIf { it.isNotBlank() },
                equippedTitleId = obj.optString("equippedTitleId", "").takeIf { it.isNotBlank() },
                equippedFlairId = obj.optString("equippedFlairId", "").takeIf { it.isNotBlank() },
                spentPoints = obj.optInt("spentPoints", 0).coerceAtLeast(0),
                creditedMatchPoints = credited
            )
        } catch (_: Exception) {
            StoredShopData(emptySet(), null, null, null, 0, emptyMap())
        }
    }

    private fun writeStoredShopData(userId: String, data: StoredShopData): UserShopState {
        val obj = JSONObject().apply {
            put("ownedItemIds", JSONArray(data.ownedItemIds.toList()))
            put("equippedFrameId", data.equippedFrameId ?: "")
            put("equippedTitleId", data.equippedTitleId ?: "")
            put("equippedFlairId", data.equippedFlairId ?: "")
            put("spentPoints", data.spentPoints)
            put("creditedMatchPoints", JSONObject().apply {
                data.creditedMatchPoints.forEach { (k, v) -> put(k, v) }
            })
        }
        prefs.edit().putString("shop_state_$userId", obj.toString()).apply()
        val earned = 30 + data.creditedMatchPoints.values.sum()
        return UserShopState(
            availablePoints = (earned - data.spentPoints).coerceAtLeast(0),
            ownedItemIds = data.ownedItemIds,
            equippedFrameId = data.equippedFrameId,
            equippedTitleId = data.equippedTitleId,
            equippedFlairId = data.equippedFlairId
        )
    }

    fun loadShopState(userId: String): UserShopState {
        val data = readStoredShopData(userId)
        val earned = 30 + data.creditedMatchPoints.values.sum()
        return UserShopState(
            availablePoints = (earned - data.spentPoints).coerceAtLeast(0),
            ownedItemIds = data.ownedItemIds,
            equippedFrameId = data.equippedFrameId,
            equippedTitleId = data.equippedTitleId,
            equippedFlairId = data.equippedFlairId
        )
    }

    fun creditMatchPoints(
        userId: String,
        matches: List<Match>,
        predictions: Map<String, Prediction>
    ): UserShopState {
        val current = readStoredShopData(userId)
        val updatedCredited = current.creditedMatchPoints.toMutableMap()
        var changed = false
        matches.forEach { match ->
            val pts = calculatePredictionPoints(match, predictions[match.id])
            val prev = updatedCredited[match.id] ?: 0
            if (pts > prev) {
                updatedCredited[match.id] = pts
                changed = true
            }
        }
        return if (changed) {
            writeStoredShopData(userId, current.copy(creditedMatchPoints = updatedCredited))
        } else {
            loadShopState(userId)
        }
    }

    fun purchaseShopItem(userId: String, item: ShopItem): Result<UserShopState> {
        val current = readStoredShopData(userId)
        if (current.ownedItemIds.contains(item.id)) {
            return Result.success(loadShopState(userId))
        }
        val available = (30 + current.creditedMatchPoints.values.sum() - current.spentPoints).coerceAtLeast(0)
        if (available < item.price) {
            return Result.failure(IllegalStateException("Pontos insuficientes (precisa de ${item.price} pts)."))
        }
        val updated = current.copy(
            ownedItemIds = current.ownedItemIds + item.id,
            spentPoints = current.spentPoints + item.price,
            equippedFrameId = if (item.category == ShopCategory.AVATAR_FRAME && current.equippedFrameId == null) item.id else current.equippedFrameId,
            equippedTitleId = if (item.category == ShopCategory.TITLE_BADGE && current.equippedTitleId == null) item.id else current.equippedTitleId,
            equippedFlairId = if (item.category == ShopCategory.CHAT_FLAIR && current.equippedFlairId == null) item.id else current.equippedFlairId
        )
        return Result.success(writeStoredShopData(userId, updated))
    }

    fun equipShopItem(userId: String, item: ShopItem): UserShopState {
        val current = readStoredShopData(userId)
        if (!current.ownedItemIds.contains(item.id)) {
            return loadShopState(userId)
        }
        val updated = when (item.category) {
            ShopCategory.AVATAR_FRAME -> current.copy(equippedFrameId = item.id)
            ShopCategory.TITLE_BADGE -> current.copy(equippedTitleId = item.id)
            ShopCategory.CHAT_FLAIR -> current.copy(equippedFlairId = item.id)
        }
        return writeStoredShopData(userId, updated)
    }

    fun creditBonusPoints(userId: String, bonusKey: String, points: Int): UserShopState {
        if (points <= 0) return loadShopState(userId)
        val current = readStoredShopData(userId)
        val key = "bonus_${todayDateKey()}_$bonusKey"
        val prev = current.creditedMatchPoints[key] ?: 0
        if (prev >= points) return loadShopState(userId)
        val updatedCredited = current.creditedMatchPoints.toMutableMap().apply {
            put(key, points)
        }
        return writeStoredShopData(userId, current.copy(creditedMatchPoints = updatedCredited))
    }

    // --- Daily Challenges & Chat Activity Persistence ---

    private fun todayDateKey(): String =
        SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())

    fun loadClaimedChallengesToday(userId: String): Set<String> {
        val raw = prefs.getString("challenges_${userId}_${todayDateKey()}", null) ?: return emptySet()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { arr.optString(it, "").takeIf { s -> s.isNotBlank() } }.toSet()
        } catch (_: Exception) {
            emptySet()
        }
    }

    fun markChallengeClaimedToday(userId: String, challengeId: String): Set<String> {
        val updated = loadClaimedChallengesToday(userId) + challengeId
        prefs.edit().putString("challenges_${userId}_${todayDateKey()}", JSONArray(updated.toList()).toString()).apply()
        return updated
    }

    fun hasSentChatToday(userId: String): Boolean =
        prefs.getBoolean("chat_sent_${userId}_${todayDateKey()}", false)

    fun markChatSentToday(userId: String) {
        prefs.edit().putBoolean("chat_sent_${userId}_${todayDateKey()}", true).apply()
    }

    // --- Chat / Bate-Papo Persistence & Room Sync ---

    private fun chatRoomKey(roomCode: String): String =
        roomCode.trim().uppercase().ifBlank { "global" }

    fun loadLocalChat(roomCode: String, currentUserId: String?): List<ChatMessage> {
        val raw = prefs.getString("chat_messages_${chatRoomKey(roomCode)}", null) ?: return emptyList()
        return try {
            parseChatArray(JSONArray(raw), currentUserId)
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveLocalChat(roomCode: String, messages: List<ChatMessage>) {
        val arr = JSONArray()
        messages.takeLast(100).forEach { msg ->
            arr.put(JSONObject().apply {
                put("id", msg.id)
                put("senderId", msg.senderId)
                put("senderName", msg.senderName)
                put("senderAvatarColorHex", msg.senderAvatarColorHex)
                if (!msg.senderTitle.isNullOrBlank()) put("senderTitle", msg.senderTitle)
                if (!msg.senderFlair.isNullOrBlank()) put("senderFlair", msg.senderFlair)
                put("text", msg.text)
                put("timestampMillis", msg.timestampMillis)
            })
        }
        prefs.edit().putString("chat_messages_${chatRoomKey(roomCode)}", arr.toString()).apply()
    }

    private fun parseChatMessageObj(obj: JSONObject, currentUserId: String?): ChatMessage? {
        val text = obj.optString("text", "").trim()
        if (text.isEmpty()) return null
        val senderId = obj.optString("senderId", "")
        val ts = obj.optLong("timestampMillis", System.currentTimeMillis())
        return ChatMessage(
            id = obj.optString("id", "msg_${senderId}_$ts"),
            senderId = senderId,
            senderName = obj.optString("senderName", "Participante"),
            senderAvatarColorHex = obj.optLong("senderAvatarColorHex", 0xFF00C853L),
            senderTitle = obj.optString("senderTitle", "").takeIf { it.isNotBlank() },
            senderFlair = obj.optString("senderFlair", "").takeIf { it.isNotBlank() },
            text = text,
            timestampMillis = ts,
            isCurrentUser = currentUserId != null && senderId == currentUserId
        )
    }

    private fun parseChatArray(arr: JSONArray, currentUserId: String?): List<ChatMessage> {
        val list = mutableListOf<ChatMessage>()
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            parseChatMessageObj(obj, currentUserId)?.let { list.add(it) }
        }
        return list.sortedBy { it.timestampMillis }.takeLast(100)
    }

    suspend fun fetchRoomChat(roomCode: String, currentUserId: String?): List<ChatMessage>? = withContext(Dispatchers.IO) {
        val baseUrl = AppConfig.SYNC_BASE_URL.trim().trimEnd('/')
        val room = roomCode.trim().uppercase()
        if (!baseUrl.startsWith("http") || room.isEmpty()) return@withContext null

        val endpoint = "$baseUrl/salas/$room/chat${syncSuffix(baseUrl)}"
        val body = httpGet(endpoint) ?: return@withContext null
        val trimmed = body.trim()
        if (trimmed == "null" || trimmed.isBlank()) return@withContext emptyList()

        return@withContext try {
            if (trimmed.startsWith("[")) {
                parseChatArray(JSONArray(trimmed), currentUserId)
            } else {
                val root = JSONObject(trimmed)
                val list = mutableListOf<ChatMessage>()
                val keys = root.keys()
                while (keys.hasNext()) {
                    val obj = root.optJSONObject(keys.next()) ?: continue
                    parseChatMessageObj(obj, currentUserId)?.let { list.add(it) }
                }
                list.sortedBy { it.timestampMillis }.takeLast(100)
            }
        } catch (_: Exception) {
            null
        }
    }

    suspend fun pushRoomChatMessage(roomCode: String, message: ChatMessage): Boolean = withContext(Dispatchers.IO) {
        val baseUrl = AppConfig.SYNC_BASE_URL.trim().trimEnd('/')
        val room = roomCode.trim().uppercase()
        if (!baseUrl.startsWith("http") || room.isEmpty()) return@withContext false

        val endpoint = "$baseUrl/salas/$room/chat${syncSuffix(baseUrl)}"
        val payload = JSONObject().apply {
            put("id", message.id)
            put("senderId", message.senderId)
            put("senderName", message.senderName)
            put("senderAvatarColorHex", message.senderAvatarColorHex)
            if (!message.senderTitle.isNullOrBlank()) put("senderTitle", message.senderTitle)
            if (!message.senderFlair.isNullOrBlank()) put("senderFlair", message.senderFlair)
            put("text", message.text)
            put("timestampMillis", message.timestampMillis)
        }

        return@withContext try {
            val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 5000
                readTimeout = 5000
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            }
            OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { writer ->
                writer.write(payload.toString())
            }
            conn.responseCode in 200..299
        } catch (_: Exception) {
            false
        }
    }

    // --- Live Data Fetcher (ESPN Public JSON API with Real Club Crests & Local Timezone) ---

    suspend fun fetchLiveMatches(): List<Match> =
        fetchScheduleMatches().filter { it.dayOffset == 0 || it.status == MatchStatus.LIVE || it.status == MatchStatus.HALFTIME }

    suspend fun fetchScheduleMatches(): List<Match> = withContext(Dispatchers.IO) {
        val localTz = TimeZone.getDefault()
        val todayCal = Calendar.getInstance(localTz)
        val espnDateFmt = SimpleDateFormat("yyyyMMdd", Locale.US).apply { timeZone = localTz }

        val dateParams = (0..4).map { offset ->
            val cal = (todayCal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, offset) }
            espnDateFmt.format(cal.time)
        }

        val allRaw = coroutineScope {
            League.entries.flatMap { league ->
                dateParams.map { dateParam ->
                    async { fetchEspnScoreboardForDate(league, dateParam, todayCal, localTz) }
                }
            }.awaitAll().flatten()
        }

        val seenIds = HashSet<String>()
        val deduped = mutableListOf<Match>()
        allRaw.forEach { match ->
            if (seenIds.add(match.id)) {
                deduped.add(match)
            }
        }

        deduped.sortedWith(
            compareBy<Match> { it.dayOffset }
                .thenBy {
                    when (it.status) {
                        MatchStatus.LIVE, MatchStatus.HALFTIME -> 0
                        MatchStatus.SCHEDULED -> 1
                        MatchStatus.FINISHED -> 2
                    }
                }
                .thenBy { it.kickoffTime }
        )
    }

    suspend fun fetchStandings(): Map<League, List<StandingRow>> = withContext(Dispatchers.IO) {
        coroutineScope {
            League.entries.map { league ->
                async {
                    val fetched = fetchEspnStandings(league)
                    league to fetched.ifEmpty { getDefaultStandings(league) }
                }
            }.awaitAll().toMap()
        }
    }

    private fun httpGet(urlString: String): String? {
        return try {
            val conn = (URL(urlString).openConnection() as HttpURLConnection).apply {
                connectTimeout = 5000
                readTimeout = 5000
                requestMethod = "GET"
                setRequestProperty("User-Agent", "BolaoDosVei/2.0")
            }
            if (conn.responseCode in 200..299) {
                conn.inputStream.bufferedReader().use { it.readText() }
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun parseEspnUtcDate(dateStr: String): Date? {
        if (dateStr.isBlank()) return null
        val patterns = listOf(
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm'Z'",
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
        )
        for (pattern in patterns) {
            try {
                val fmt = SimpleDateFormat(pattern, Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
                val parsed = fmt.parse(dateStr)
                if (parsed != null) return parsed
            } catch (_: Exception) {
                // try next pattern
            }
        }
        return null
    }

    private fun computeDayOffset(eventDate: Date, todayCal: Calendar, localTz: TimeZone): Int {
        val eventCal = Calendar.getInstance(localTz).apply { time = eventDate }
        val todayMidnight = (todayCal.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val eventMidnight = (eventCal.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        return kotlin.math.round((eventMidnight - todayMidnight) / 86_400_000.0).toInt()
    }

    private fun fetchEspnScoreboardForDate(
        league: League,
        dateParam: String,
        todayCal: Calendar,
        localTz: TimeZone
    ): List<Match> {
        val url = "https://site.api.espn.com/apis/site/v2/sports/soccer/${league.espnSlug}/scoreboard?dates=$dateParam"
        val body = httpGet(url) ?: return emptyList()
        val localTimeFmt = SimpleDateFormat("HH:mm", Locale.getDefault()).apply { timeZone = localTz }
        val localDateFmt = SimpleDateFormat("dd/MM", Locale.getDefault()).apply { timeZone = localTz }

        return try {
            val root = JSONObject(body)
            val events = root.optJSONArray("events") ?: return emptyList()
            val list = mutableListOf<Match>()
            for (i in 0 until events.length()) {
                val event = events.getJSONObject(i)
                val id = "${league.id}_${event.optString("id", i.toString())}"
                val competitions = event.optJSONArray("competitions") ?: continue
                if (competitions.length() == 0) continue
                val comp = competitions.getJSONObject(0)
                val competitors = comp.optJSONArray("competitors") ?: continue
                if (competitors.length() < 2) continue

                val statusObj = comp.optJSONObject("status")
                val typeObj = statusObj?.optJSONObject("type")
                val state = typeObj?.optString("state", "pre") ?: "pre"
                val detail = typeObj?.optString("shortDetail", "") ?: ""
                val clockVal = statusObj?.optDouble("clock", 0.0)?.toInt()?.div(60) ?: 0

                val matchStatus = when {
                    state == "in" && detail.contains("HT", true) -> MatchStatus.HALFTIME
                    state == "in" -> MatchStatus.LIVE
                    state == "post" -> MatchStatus.FINISHED
                    else -> MatchStatus.SCHEDULED
                }

                // Convert ESPN UTC timestamp strictly to device's local timezone
                val dateStr = event.optString("date", "")
                val parsedDate = parseEspnUtcDate(dateStr)

                // Keep matches happening today..+3 days in local time (or currently LIVE/HALFTIME)
                val isLiveNow = matchStatus == MatchStatus.LIVE || matchStatus == MatchStatus.HALFTIME
                val rawOffset = parsedDate?.let { computeDayOffset(it, todayCal, localTz) }
                if (!isLiveNow && (rawOffset == null || rawOffset !in 0..3)) {
                    continue
                }
                val dayOffset = if (isLiveNow && (rawOffset == null || rawOffset < 0)) 0 else (rawOffset ?: 0)
                val dateLabel = when (dayOffset) {
                    0 -> "Hoje"
                    1 -> "Amanhã"
                    else -> if (parsedDate != null) localDateFmt.format(parsedDate) else "Em breve"
                }

                val kickoff = if (parsedDate != null) localTimeFmt.format(parsedDate) else "Hoje"

                var homeObj: JSONObject? = null
                var awayObj: JSONObject? = null
                for (c in 0 until competitors.length()) {
                    val item = competitors.getJSONObject(c)
                    if (item.optString("homeAway") == "home") homeObj = item else awayObj = item
                }
                if (homeObj == null || awayObj == null) continue

                val homeTeamObj = homeObj.getJSONObject("team")
                val awayTeamObj = awayObj.getJSONObject("team")
                val venue = comp.optJSONObject("venue")?.optString("fullName", "Estádio") ?: "Estádio"

                list.add(
                    Match(
                        id = id,
                        league = league,
                        homeTeam = TeamInfo(
                            name = homeTeamObj.optString("shortDisplayName", homeTeamObj.optString("displayName", "Mandante")),
                            shortName = homeTeamObj.optString("abbreviation", "MAN").take(3),
                            colorHex = parseColorHex(homeTeamObj.optString("color", ""), 0xFF00C853),
                            logoUrl = homeTeamObj.optString("logo", "")
                        ),
                        awayTeam = TeamInfo(
                            name = awayTeamObj.optString("shortDisplayName", awayTeamObj.optString("displayName", "Visitante")),
                            shortName = awayTeamObj.optString("abbreviation", "VIS").take(3),
                            colorHex = parseColorHex(awayTeamObj.optString("color", ""), 0xFF3B82F6),
                            logoUrl = awayTeamObj.optString("logo", "")
                        ),
                        homeScore = homeObj.optString("score", "0").toIntOrNull() ?: 0,
                        awayScore = awayObj.optString("score", "0").toIntOrNull() ?: 0,
                        status = matchStatus,
                        minute = clockVal.coerceIn(0, 95),
                        kickoffTime = kickoff,
                        stadium = venue,
                        isLiveApi = true,
                        dayOffset = dayOffset,
                        dateLabel = dateLabel
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun fetchEspnStandings(league: League): List<StandingRow> {
        val url = "https://site.api.espn.com/apis/v2/sports/soccer/${league.espnSlug}/standings"
        val body = httpGet(url) ?: return emptyList()
        return try {
            val root = JSONObject(body)
            val children = root.optJSONArray("children") ?: return emptyList()
            if (children.length() == 0) return emptyList()
            val standingsObj = children.getJSONObject(0).optJSONObject("standings") ?: return emptyList()
            val entries = standingsObj.optJSONArray("entries") ?: return emptyList()
            val rows = mutableListOf<StandingRow>()
            for (i in 0 until entries.length()) {
                val entry = entries.getJSONObject(i)
                val team = entry.optJSONObject("team") ?: continue
                val stats = entry.optJSONArray("stats") ?: continue
                val statMap = mutableMapOf<String, Int>()
                for (s in 0 until stats.length()) {
                    val st = stats.getJSONObject(s)
                    statMap[st.optString("name")] = st.optDouble("value", 0.0).toInt()
                }
                val name = team.optString("shortDisplayName", team.optString("displayName", "Time"))
                val logoUrl = team.optJSONArray("logos")?.optJSONObject(0)?.optString("href", "") ?: ""
                rows.add(
                    StandingRow(
                        position = statMap["rank"] ?: (i + 1),
                        teamName = name,
                        shortName = team.optString("abbreviation", name.take(3).uppercase()).take(3),
                        colorHex = colorForTeamName(name),
                        logoUrl = logoUrl,
                        played = statMap["gamesPlayed"] ?: 0,
                        won = statMap["wins"] ?: 0,
                        drawn = statMap["ties"] ?: 0,
                        lost = statMap["losses"] ?: 0,
                        goalsFor = statMap["pointsFor"] ?: 0,
                        goalsAgainst = statMap["pointsAgainst"] ?: 0,
                        points = statMap["points"] ?: 0
                    )
                )
            }
            rows.sortedBy { it.position }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun parseColorHex(raw: String, fallback: Long): Long {
        val clean = raw.removePrefix("#").trim()
        if (clean.length != 6) return fallback
        return try {
            0xFF000000L or clean.toLong(16)
        } catch (_: Exception) {
            fallback
        }
    }

    private fun colorForTeamName(name: String): Long {
        val palette = listOf(
            0xFFE53935L, 0xFF1E88E5L, 0xFF43A047L, 0xFFFB8C00L,
            0xFF8E24AAL, 0xFF00ACC1L, 0xFFFDD835L, 0xFF3949ABL
        )
        return palette[abs(name.hashCode()) % palette.size]
    }

    private fun espnLogo(id: Int): String = "https://a.espncdn.com/i/teamlogos/soccer/500/$id.png"

    // --- Fallback Matches & Standings with Official Club Crest URLs ---

    fun getDefaultMatches(): List<Match> = listOf(
        Match(
            id = "bra_1",
            league = League.BRASILEIRAO,
            homeTeam = TeamInfo("Flamengo", "FLA", 0xFFE53935, espnLogo(819)),
            awayTeam = TeamInfo("Palmeiras", "PAL", 0xFF2E7D32, espnLogo(2029)),
            homeScore = 2,
            awayScore = 1,
            status = MatchStatus.LIVE,
            minute = 68,
            kickoffTime = "16:00",
            stadium = "Maracanã",
            lastScorer = "Pedro"
        ),
        Match(
            id = "bra_2",
            league = League.BRASILEIRAO,
            homeTeam = TeamInfo("Botafogo", "BOT", 0xFF37474F, espnLogo(6086)),
            awayTeam = TeamInfo("São Paulo", "SAO", 0xFFD32F2F, espnLogo(2026)),
            homeScore = 1,
            awayScore = 1,
            status = MatchStatus.LIVE,
            minute = 41,
            kickoffTime = "16:30",
            stadium = "Nilton Santos",
            lastScorer = "Luiz Henrique"
        ),
        Match(
            id = "bra_3",
            league = League.BRASILEIRAO,
            homeTeam = TeamInfo("Corinthians", "COR", 0xFF455A64, espnLogo(874)),
            awayTeam = TeamInfo("Atlético-MG", "CAM", 0xFF263238, espnLogo(7632)),
            homeScore = 0,
            awayScore = 0,
            status = MatchStatus.SCHEDULED,
            minute = 0,
            kickoffTime = "19:30",
            stadium = "Neo Química Arena"
        ),
        Match(
            id = "eng_1",
            league = League.PREMIER_LEAGUE,
            homeTeam = TeamInfo("Arsenal", "ARS", 0xFFEF4444, espnLogo(359)),
            awayTeam = TeamInfo("Liverpool", "LIV", 0xFFB91C1C, espnLogo(364)),
            homeScore = 2,
            awayScore = 2,
            status = MatchStatus.LIVE,
            minute = 74,
            kickoffTime = "13:30",
            stadium = "Emirates Stadium",
            lastScorer = "Salah"
        ),
        Match(
            id = "eng_2",
            league = League.PREMIER_LEAGUE,
            homeTeam = TeamInfo("Man City", "MCI", 0xFF38BDF8, espnLogo(382)),
            awayTeam = TeamInfo("Chelsea", "CHE", 0xFF1D4ED8, espnLogo(363)),
            homeScore = 0,
            awayScore = 0,
            status = MatchStatus.SCHEDULED,
            minute = 0,
            kickoffTime = "17:00",
            stadium = "Etihad Stadium"
        ),
        Match(
            id = "esp_1",
            league = League.LA_LIGA,
            homeTeam = TeamInfo("Real Madrid", "RMA", 0xFFF59E0B, espnLogo(86)),
            awayTeam = TeamInfo("Barcelona", "BAR", 0xFF2563EB, espnLogo(83)),
            homeScore = 3,
            awayScore = 2,
            status = MatchStatus.LIVE,
            minute = 82,
            kickoffTime = "16:00",
            stadium = "Santiago Bernabéu",
            lastScorer = "Vini Jr"
        ),
        Match(
            id = "esp_2",
            league = League.LA_LIGA,
            homeTeam = TeamInfo("Atlético Madrid", "ATM", 0xFFDC2626, espnLogo(1068)),
            awayTeam = TeamInfo("Sevilla", "SEV", 0xFF991B1B, espnLogo(243)),
            homeScore = 0,
            awayScore = 0,
            status = MatchStatus.SCHEDULED,
            minute = 0,
            kickoffTime = "18:30",
            stadium = "Metropolitano"
        )
    )

    fun getDefaultStandings(league: League): List<StandingRow> = when (league) {
        League.BRASILEIRAO -> listOf(
            StandingRow(1, "Botafogo", "BOT", 0xFF37474F, espnLogo(6086), 34, 21, 7, 6, 56, 28, 70),
            StandingRow(2, "Palmeiras", "PAL", 0xFF2E7D32, espnLogo(2029), 34, 20, 8, 6, 58, 29, 68),
            StandingRow(3, "Flamengo", "FLA", 0xFFE53935, espnLogo(819), 34, 19, 8, 7, 55, 34, 65),
            StandingRow(4, "Internacional", "INT", 0xFFDC2626, espnLogo(1936), 34, 17, 11, 6, 50, 31, 62),
            StandingRow(5, "São Paulo", "SAO", 0xFFD32F2F, espnLogo(2026), 34, 17, 7, 10, 47, 34, 58),
            StandingRow(6, "Cruzeiro", "CRU", 0xFF2563EB, espnLogo(2022), 34, 14, 9, 11, 41, 36, 51),
            StandingRow(7, "Bahia", "BAH", 0xFF0284C7, espnLogo(9967), 34, 14, 7, 13, 44, 42, 49),
            StandingRow(8, "Corinthians", "COR", 0xFF455A64, espnLogo(874), 34, 12, 11, 11, 45, 42, 47),
            StandingRow(9, "Atlético-MG", "CAM", 0xFF263238, espnLogo(7632), 34, 11, 12, 11, 44, 46, 45),
            StandingRow(10, "Vasco", "VAS", 0xFF334155, espnLogo(3454), 34, 12, 7, 15, 39, 49, 43),
            StandingRow(11, "Grêmio", "GRE", 0xFF0EA5E9, espnLogo(6273), 34, 11, 8, 15, 40, 45, 41),
            StandingRow(12, "Mirassol", "MIR", 0xFFFACC15, espnLogo(9169), 34, 11, 7, 16, 38, 46, 40),
            StandingRow(13, "Coritiba", "CFC", 0xFF047857, espnLogo(3456), 34, 10, 9, 15, 36, 44, 39),
            StandingRow(14, "Chapecoense", "CHA", 0xFF22C55E, espnLogo(9318), 34, 10, 8, 16, 34, 47, 38),
            StandingRow(15, "Remo", "REM", 0xFF1D4ED8, espnLogo(4936), 34, 9, 9, 16, 32, 46, 36)
        )
        League.PREMIER_LEAGUE -> listOf(
            StandingRow(1, "Liverpool", "LIV", 0xFFB91C1C, espnLogo(364), 28, 21, 5, 2, 64, 24, 68),
            StandingRow(2, "Arsenal", "ARS", 0xFFEF4444, espnLogo(359), 28, 18, 7, 3, 56, 25, 61),
            StandingRow(3, "Man City", "MCI", 0xFF38BDF8, espnLogo(382), 28, 17, 6, 5, 59, 31, 57),
            StandingRow(4, "Chelsea", "CHE", 0xFF1D4ED8, espnLogo(363), 28, 15, 7, 6, 53, 34, 52),
            StandingRow(5, "Newcastle", "NEW", 0xFF334155, espnLogo(361), 28, 14, 7, 7, 48, 35, 49),
            StandingRow(6, "Aston Villa", "AVL", 0xFF7C3AED, espnLogo(362), 28, 13, 8, 7, 45, 38, 47),
            StandingRow(7, "Tottenham", "TOT", 0xFF475569, espnLogo(367), 28, 13, 5, 10, 52, 40, 44),
            StandingRow(8, "Brighton", "BHA", 0xFF0284C7, espnLogo(331), 28, 11, 10, 7, 44, 39, 43),
            StandingRow(9, "Man United", "MUN", 0xFFDC2626, espnLogo(360), 28, 11, 7, 10, 38, 37, 40),
            StandingRow(10, "Fulham", "FUL", 0xFF64748B, espnLogo(370), 28, 10, 9, 9, 40, 38, 39)
        )
        League.LA_LIGA -> listOf(
            StandingRow(1, "Real Madrid", "RMA", 0xFFF59E0B, espnLogo(86), 28, 20, 5, 3, 62, 25, 65),
            StandingRow(2, "Barcelona", "BAR", 0xFF2563EB, espnLogo(83), 28, 20, 3, 5, 71, 29, 63),
            StandingRow(3, "Atlético Madrid", "ATM", 0xFFDC2626, espnLogo(1068), 28, 17, 8, 3, 49, 22, 59),
            StandingRow(4, "Athletic Club", "ATH", 0xFFB91C1C, espnLogo(93), 28, 14, 9, 5, 46, 26, 51),
            StandingRow(5, "Villarreal", "VIL", 0xFFEAB308, espnLogo(102), 28, 13, 8, 7, 50, 39, 47),
            StandingRow(6, "Real Betis", "BET", 0xFF16A34A, espnLogo(244), 28, 12, 8, 8, 39, 34, 44),
            StandingRow(7, "Real Sociedad", "RSO", 0xFF0284C7, espnLogo(89), 28, 11, 7, 10, 34, 31, 40),
            StandingRow(8, "Girona", "GIR", 0xFFEF4444, espnLogo(9812), 28, 10, 8, 10, 38, 39, 38),
            StandingRow(9, "Sevilla", "SEV", 0xFF991B1B, espnLogo(243), 28, 9, 9, 10, 33, 37, 36),
            StandingRow(10, "Valencia", "VAL", 0xFFF97316, espnLogo(94), 28, 8, 9, 11, 31, 40, 33)
        )
        League.CHAMPIONS_LEAGUE -> listOf(
            StandingRow(1, "Liverpool", "LIV", 0xFFB91C1C, espnLogo(364), 8, 7, 0, 1, 17, 5, 21),
            StandingRow(2, "Barcelona", "BAR", 0xFF2563EB, espnLogo(83), 8, 6, 1, 1, 28, 13, 19),
            StandingRow(3, "Arsenal", "ARS", 0xFFEF4444, espnLogo(359), 8, 6, 1, 1, 16, 3, 19),
            StandingRow(4, "Inter", "INT", 0xFF1D4ED8, espnLogo(110), 8, 6, 1, 1, 11, 1, 19),
            StandingRow(5, "Atlético Madrid", "ATM", 0xFFDC2626, espnLogo(1068), 8, 6, 0, 2, 20, 12, 18),
            StandingRow(6, "Real Madrid", "RMA", 0xFFF59E0B, espnLogo(86), 8, 5, 0, 3, 20, 12, 15),
            StandingRow(7, "Bayern", "BAY", 0xFFDC2626, espnLogo(132), 8, 5, 0, 3, 20, 12, 15),
            StandingRow(8, "Dortmund", "BVB", 0xFFEAB308, espnLogo(124), 8, 5, 0, 3, 22, 12, 15),
            StandingRow(9, "PSG", "PSG", 0xFF1E3A8A, espnLogo(160), 8, 4, 1, 3, 14, 9, 13),
            StandingRow(10, "Man City", "MCI", 0xFF38BDF8, espnLogo(382), 8, 3, 2, 3, 18, 14, 11)
        )
        League.LIBERTADORES -> listOf(
            StandingRow(1, "Botafogo", "BOT", 0xFF37474F, espnLogo(6086), 6, 4, 1, 1, 11, 5, 13),
            StandingRow(2, "Flamengo", "FLA", 0xFFE53935, espnLogo(819), 6, 4, 1, 1, 10, 4, 13),
            StandingRow(3, "Palmeiras", "PAL", 0xFF2E7D32, espnLogo(2029), 6, 4, 0, 2, 12, 6, 12),
            StandingRow(4, "River Plate", "RIV", 0xFFDC2626, espnLogo(16), 6, 3, 2, 1, 9, 5, 11),
            StandingRow(5, "Atlético-MG", "CAM", 0xFF263238, espnLogo(7632), 6, 3, 1, 2, 10, 7, 10),
            StandingRow(6, "São Paulo", "SAO", 0xFFD32F2F, espnLogo(2026), 6, 3, 1, 2, 8, 6, 10),
            StandingRow(7, "Peñarol", "PEN", 0xFFF59E0B, espnLogo(249), 6, 2, 2, 2, 7, 7, 8),
            StandingRow(8, "Nacional", "NAC", 0xFF1D4ED8, espnLogo(250), 6, 2, 1, 3, 6, 8, 7)
        )
        League.COPA_DO_BRASIL -> listOf(
            StandingRow(1, "Flamengo", "FLA", 0xFFE53935, espnLogo(819), 8, 6, 1, 1, 16, 5, 19),
            StandingRow(2, "Atlético-MG", "CAM", 0xFF263238, espnLogo(7632), 8, 5, 2, 1, 14, 6, 17),
            StandingRow(3, "Corinthians", "COR", 0xFF455A64, espnLogo(874), 8, 4, 3, 1, 12, 7, 15),
            StandingRow(4, "Vasco", "VAS", 0xFF334155, espnLogo(3454), 8, 4, 2, 2, 11, 8, 14),
            StandingRow(5, "Palmeiras", "PAL", 0xFF2E7D32, espnLogo(2029), 6, 4, 1, 1, 10, 4, 13),
            StandingRow(6, "São Paulo", "SAO", 0xFFD32F2F, espnLogo(2026), 6, 3, 2, 1, 8, 5, 11),
            StandingRow(7, "Bahia", "BAH", 0xFF0284C7, espnLogo(9967), 6, 3, 1, 2, 9, 7, 10),
            StandingRow(8, "Cruzeiro", "CRU", 0xFF2563EB, espnLogo(2022), 6, 2, 2, 2, 7, 6, 8)
        )
        League.SUL_AMERICANA -> listOf(
            StandingRow(1, "Cruzeiro", "CRU", 0xFF2563EB, espnLogo(2022), 6, 4, 2, 0, 11, 3, 14),
            StandingRow(2, "Racing", "RAC", 0xFF0EA5E9, espnLogo(15), 6, 4, 1, 1, 12, 5, 13),
            StandingRow(3, "Corinthians", "COR", 0xFF455A64, espnLogo(874), 6, 4, 1, 1, 10, 4, 13),
            StandingRow(4, "Internacional", "INT", 0xFFDC2626, espnLogo(1936), 6, 3, 2, 1, 9, 5, 11),
            StandingRow(5, "Lanús", "LAN", 0xFF991B1B, espnLogo(12), 6, 3, 1, 2, 8, 6, 10),
            StandingRow(6, "Athletico-PR", "CAP", 0xFFEF4444, espnLogo(1934), 6, 2, 2, 2, 7, 6, 8),
            StandingRow(7, "LDU Quito", "LDU", 0xFF475569, espnLogo(209), 6, 2, 1, 3, 6, 8, 7),
            StandingRow(8, "Cerro Porteño", "CER", 0xFF1D4ED8, espnLogo(232), 6, 1, 2, 3, 5, 9, 5)
        )
    }
}
