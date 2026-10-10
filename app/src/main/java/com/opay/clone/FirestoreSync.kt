package com.opay.clone

import android.content.Context
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

/**
 * FirestoreSync
 *
 * Thin wrapper over Firestore so AccountStore can mirror every local write
 * to the cloud. This is what makes cross-device funding work: two phones
 * that both know the same phone number both see the same account document.
 *
 * Firestore layout:
 *   collection "accounts"
 *     document   <normalized phone>     e.g. "08012345678"
 *       name         : String
 *       balance      : String             (keep as string so it round-trips with AccountStore)
 *       password     : String
 *       deviceId     : String
 *       pendingDeposit: Boolean
 *       updatedAt    : Long               (ms since epoch, for debugging)
 */
object FirestoreSync {

    private const val TAG = "FirestoreSync"
    private const val COLLECTION = "accounts"

    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    /** Push the full account doc to Firestore. Fire-and-forget. */
    fun push(account: Account, ctx: Context? = null) {
        try {
            val data = mapOf(
                "phone" to account.phone,
                "name" to account.name,
                "balance" to account.balance,
                "password" to account.password,
                "deviceId" to (account.deviceId ?: ""),
                "pendingDeposit" to account.pendingDeposit,
                "updatedAt" to System.currentTimeMillis()
            )
            db.collection(COLLECTION)
                .document(account.phone)
                .set(data, SetOptions.merge())
                .addOnFailureListener { e -> Log.e(TAG, "push failed: ${e.message}") }
        } catch (e: Throwable) {
            Log.e(TAG, "push crashed: ${e.message}")
        }
    }

    /** Push just the balance — cheaper for the admin funding path. */
    fun pushBalance(phone: String, balance: String) {
        try {
            db.collection(COLLECTION)
                .document(phone)
                .set(mapOf(
                    "balance" to balance,
                    "pendingDeposit" to false,
                    "updatedAt" to System.currentTimeMillis()
                ), SetOptions.merge())
                .addOnFailureListener { e -> Log.e(TAG, "pushBalance failed: ${e.message}") }
        } catch (e: Throwable) {
            Log.e(TAG, "pushBalance crashed: ${e.message}")
        }
    }

    /**
     * Cross-device lookup. Reads an account from Firestore. If found, calls
     * onResult with the Account. If not found, calls onResult(null).
     */
    fun lookup(phone: String, onResult: (Account?) -> Unit) {
        try {
            db.collection(COLLECTION)
                .document(phone)
                .get()
                .addOnSuccessListener { doc ->
                    if (!doc.exists()) {
                        onResult(null)
                        return@addOnSuccessListener
                    }
                    val acct = Account(
                        phone = doc.getString("phone") ?: phone,
                        name = doc.getString("name") ?: "",
                        balance = doc.getString("balance") ?: "0.00",
                        password = doc.getString("password") ?: "",
                        deviceId = doc.getString("deviceId")?.takeIf { it.isNotEmpty() },
                        pendingDeposit = doc.getBoolean("pendingDeposit") ?: false
                    )
                    onResult(acct)
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "lookup failed: ${e.message}")
                    onResult(null)
                }
        } catch (e: Throwable) {
            Log.e(TAG, "lookup crashed: ${e.message}")
            onResult(null)
        }
    }

    /**
     * When a phone finishes onboarding on a new device, hydrate local storage
     * from Firestore if the account already exists in the cloud.
     */
    fun hydrate(ctx: Context, phone: String, onDone: (Boolean) -> Unit) {
        lookup(phone) { acct ->
            if (acct == null) {
                onDone(false)
                return@lookup
            }
            // Write the cloud account into local storage
            AccountStore.writeLocal(ctx, acct)
            onDone(true)
        }
    }

    // ============================================================
    // Transaction mirroring — cross-device transaction history.
    // ============================================================

    private const val TXN_ROOT = "transactions"

    /**
     * Push a single transaction to Firestore under transactions/{phone}/items/{auto-id}.
     * phone must be the owner's normalized phone number.
     */
    fun pushTransaction(phone: String, entry: org.json.JSONObject) {
        try {
            val doc = hashMapOf<String, Any?>(
                "type" to entry.optString("type"),
                "title" to entry.optString("title"),
                "amount" to entry.optDouble("amount", 0.0),
                "recipient" to entry.optString("recipient"),
                "senderName" to entry.optString("senderName"),
                "account" to entry.optString("account"),
                "timestamp" to entry.optString("timestamp"),
                "date" to entry.optString("date"),
                "status" to entry.optString("status"),
                "ownerPhone" to phone,
                "uploadedAt" to System.currentTimeMillis()
            )
            db.collection(TXN_ROOT)
                .document(phone)
                .collection("items")
                .add(doc)
                .addOnFailureListener { e -> Log.e(TAG, "pushTransaction failed: ${e.message}") }
        } catch (e: Throwable) {
            Log.e(TAG, "pushTransaction crashed: ${e.message}")
        }
    }

    /**
     * Read the full transaction history for a phone from Firestore and return
     * them as a JSON array string in the same shape TxnLog uses locally.
     * Calls onResult with the JSON string, or "[]" if none.
     */
    fun fetchTransactions(phone: String, onResult: (String) -> Unit) {
        try {
            db.collection(TXN_ROOT)
                .document(phone)
                .collection("items")
                .get()
                .addOnSuccessListener { snap ->
                    val arr = org.json.JSONArray()
                    // Sort oldest-first to match local storage order
                    val docs = snap.documents.sortedBy { it.getString("timestamp") ?: "" }
                    for (d in docs) {
                        val o = org.json.JSONObject()
                        o.put("type",         d.getString("type") ?: "")
                        o.put("title",        d.getString("title") ?: "")
                        o.put("amount",       d.getDouble("amount") ?: 0.0)
                        o.put("recipient",    d.getString("recipient") ?: "")
                        o.put("senderName",   d.getString("senderName") ?: "")
                        o.put("account",      d.getString("account") ?: "")
                        o.put("timestamp",    d.getString("timestamp") ?: "")
                        o.put("date",         d.getString("date") ?: "")
                        o.put("status",       d.getString("status") ?: "Successful")
                        arr.put(o)
                    }
                    onResult(arr.toString())
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "fetchTransactions failed: ${e.message}")
                    onResult("[]")
                }
        } catch (e: Throwable) {
            Log.e(TAG, "fetchTransactions crashed: ${e.message}")
            onResult("[]")
        }
    }
}