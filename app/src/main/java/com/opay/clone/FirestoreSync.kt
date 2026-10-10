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
}