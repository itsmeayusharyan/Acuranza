package com.acuranza.app.repo

import androidx.room.withTransaction
import com.acuranza.app.data.AcuranzaDatabase
import com.acuranza.app.domain.SmsMapping

/** Gives uncategorised imported transactions a real category; see [SmsMapping.recategorised]. */
object AutoCategoriser {
    /** Returns how many transactions changed category. */
    suspend fun run(db: AcuranzaDatabase): Int {
        val rules = db.merchantRules().all().associate { it.merchant to it.categoryKey }
        val updates = db.transactions().recategoriseCandidates().mapNotNull { SmsMapping.recategorised(it, rules) }
        if (updates.isEmpty()) return 0
        // One transaction, so screens recompute once rather than once per row.
        db.withTransaction { updates.forEach { db.transactions().update(it) } }
        return updates.size
    }
}
