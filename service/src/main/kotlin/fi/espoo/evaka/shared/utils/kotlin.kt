package fi.espoo.evaka.shared.utils

inline fun <T> T.applyIf(condition: Boolean, block: T.() -> Unit): T =
    if (condition) this.apply(block) else this
