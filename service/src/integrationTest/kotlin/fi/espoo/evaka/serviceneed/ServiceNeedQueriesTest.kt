package fi.espoo.evaka.serviceneed

import fi.espoo.evaka.PureJdbiTest
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.resetDatabase
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

class ServiceNeedQueriesTest : PureJdbiTest() {

    @AfterEach
    fun afterEach() {
        db.transaction { tx -> tx.resetDatabase() }
    }

    @Test
    fun getServiceNeedOptionPublicInfos() {
        assertThat(db.read { tx -> tx.getServiceNeedOptionPublicInfos(PlacementType.values().toList()) }).isEmpty()
    }

}
