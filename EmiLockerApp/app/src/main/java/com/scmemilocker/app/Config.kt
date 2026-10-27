package com.scmemilocker.app

/**
 * POC configuration.
 *
 * FRP_ACCOUNT_IDS: the numeric Google account ID(s) of the LENDER's recovery
 * account (NOT the email). After a recovery-mode factory reset, the phone will
 * refuse setup until someone signs in with one of these accounts.
 * Leave empty to disable Factory Reset Protection. See POC_SETUP.md section 8.
 */
object Config {
    val FRP_ACCOUNT_IDS: List<String> = listOf(
        "" // e.g. "112233445566778899001"
    )
}
