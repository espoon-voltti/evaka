package fi.espoo.evaka.daycare

import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.ClosedPeriod
import fi.espoo.evaka.shared.utils.zoneId
import org.jdbi.v3.core.kotlin.mapTo
import java.time.LocalDate

data class PreschoolTerm(
    /*The period during which finnish speaking preschool is arranged.*/
    val finnishPreschool: ClosedPeriod,
    /*The period during which swedish speaking preschool is arranged.*/
    val swedishPreschool: ClosedPeriod,
    /*Children going to preschool may apply for connected daycare slightly outside the actual preschool term,
    usually starting few weeks earlier. That is then essentially normal daycare with discounted price.
    This period defines when that connected daycare is available.*/
    val extendedTerm: ClosedPeriod,
    /*The official application period. The end date is not enforced though, but applications are accepted
    until end of term.*/
    val applicationPeriod: ClosedPeriod
) {
    fun isApplicationAccepted(date: LocalDate) = ClosedPeriod(applicationPeriod.start, extendedTerm.end).includes(date)
}

fun Database.Read.getPreschoolTerms(): List<PreschoolTerm> {
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
