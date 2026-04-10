// SPDX-FileCopyrightText: 2021 City of Tampere
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.trevaka

import evaka.core.shared.db.Database
import evaka.core.shared.noopTracer
import evaka.core.vtjclient.service.persondetails.MockPersonDetailsService
import evaka.instance.tampere.database.resetTampereDatabaseForE2ETests
import java.io.FileOutputStream
import java.nio.file.Paths
import org.jdbi.v3.core.Jdbi
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.fail
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.wiremock.spring.EnableWireMock
import software.amazon.awssdk.core.ResponseInputStream
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.Delete
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.GetObjectResponse
import software.amazon.awssdk.services.s3.model.ListObjectsRequest
import software.amazon.awssdk.services.s3.model.NoSuchKeyException
import software.amazon.awssdk.services.s3.model.ObjectIdentifier

private val reportsPath: String = "${Paths.get("build").toAbsolutePath()}/reports"

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(classes = [IntegrationTestConfiguration::class])
@EnableWireMock
abstract class AbstractIntegrationTest {
    @Autowired private lateinit var jdbi: Jdbi

    protected lateinit var db: Database.Connection

    @Autowired protected lateinit var s3Client: S3Client

    @BeforeAll
    protected fun initializeJdbi() {
        db = Database(jdbi, noopTracer()).connectWithManualLifecycle()
    }

    @BeforeEach
    fun setup() {
        db.transaction { tx -> tx.resetTampereDatabaseForE2ETests() }
        clearBucket("trevaka-export-it")
        MockPersonDetailsService.reset()
    }

    @AfterAll
    protected fun afterAll() {
        db.close()
    }

    protected fun writeReportsFile(filename: String, bytes: ByteArray) {
        val filepath = "$reportsPath/$filename"
        FileOutputStream(filepath).use { it.write(bytes) }
    }

    protected fun getS3Object(bucket: String, key: String): ResponseInputStream<GetObjectResponse> =
        try {
            s3Client.getObject(GetObjectRequest.builder().bucket(bucket).key(key).build())
        } catch (exception: NoSuchKeyException) {
            val keys =
                s3Client
                    .listObjects(ListObjectsRequest.builder().bucket(bucket).build())
                    .contents()
                    .map { it.key() }
            fail("The key $key does not exist. Available keys: $keys")
        }

    private fun clearBucket(bucket: String) {
        val objectList =
            s3Client.listObjects(ListObjectsRequest.builder().bucket(bucket).build()).contents()

        if (objectList.size > 0) {
            val objectIdList = objectList.map { ObjectIdentifier.builder().key(it.key()).build() }

            s3Client.deleteObjects(
                DeleteObjectsRequest.builder()
                    .bucket(bucket)
                    .delete(Delete.builder().objects(objectIdList).build())
                    .build()
            )
        }
    }
}
