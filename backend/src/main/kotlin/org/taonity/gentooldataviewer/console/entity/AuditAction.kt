package org.taonity.gentooldataviewer.console.entity

enum class AuditAction {
    REQUEST_ACCESS,
    APPROVE_ACCESS,
    REJECT_ACCESS,
    CHANGE_ROLE,
    EDIT_CONFIG,
    RESET_CONFIG,
    CLAIM_GENTOOL_LINK,
    RECLAIM_GENTOOL_LINK,
    ADMIN_LINK_GENTOOL_USER,
    ADMIN_UNLINK_GENTOOL_USER,
    ADMIN_IMPORT_DISCORD_USER,
    // Existing PostgreSQL audit rows may still contain these pre-direct-claim actions.
    REQUEST_GENTOOL_LINK,
    APPROVE_GENTOOL_LINK,
    REJECT_GENTOOL_LINK,
    REQUEST_RESCAN,
}
