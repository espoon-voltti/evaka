package fi.espoo.evaka.daycare

import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.ClosedPeriod
import fi.espoo.evaka.shared.utils.zoneId
import org.jdbi.v3.core.kotlin.mapTo
import java.time.LocalDate

data class PreschoolTerm (
    val finnishPreschool: ClosedPeriod,
    val swedishPreschool: ClosedPeriod,
    val extendedTerm: ClosedPeriod,
    val applicationPeriod: ClosedPeriod
)

fun Database.Read.getPreschoolTerms(): List<PreschoolTerm>{
    return createQuery("SELECT * FROM preschool_term order by extended_term")
        .mapTo<PreschoolTerm>()
        .list()
}

fun Database.Read.getActivePreschoolTermAt(date: LocalDate): PreschoolTerm? {
    return getPreschoolTerms().firstOrNull { it.extendedTerm.includes(date) }
}

fun Database.Read.getCurrentPreschoolTerm(): PreschoolTerm? {
    return getActivePreschoolTermAt(LocalDate.now(zoneId))
}

fun Database.Read.getNextPreschoolTerm(): PreschoolTerm? {
    return getPreschoolTerms().firstOrNull { it.extendedTerm.start.isAfter(LocalDate.now(zoneId)) }
}
