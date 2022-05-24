// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.s3

import fi.espoo.evaka.BucketEnv
import fi.espoo.evaka.FullApplicationTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class DocumentServiceIntegrationTest : FullApplicationTest(resetDbBeforeEach = false) {
    @Autowired
    private lateinit var s3Client: S3Client

    @Autowired
    private lateinit var s3Presigner: S3Presigner

    @Autowired
    private lateinit var bucketEnv: BucketEnv

    private lateinit var documentClient: DocumentService

    @BeforeEach
    fun beforeEach() {
        documentClient = DocumentService(s3Client, s3Presigner, proxyThroughNginx = true)
    }

    @Test
    fun `redirects when not proxying through nginx`() {
        val documentClientNoProxy = DocumentService(s3Client, s3Presigner, proxyThroughNginx = false)
        documentClientNoProxy.upload(bucketEnv.data, Document("test", byteArrayOf(0x11, 0x22, 0x33), "text/plain"))

        val response = documentClientNoProxy.responseAttachment(bucketEnv.data, "test", null)
        assertEquals(HttpStatus.FOUND, response.statusCode)
        assertNotNull(response.headers["Location"])
        assertNull(response.headers["X-Accel-Redirect"])
    }

    @Test
    fun `uses X-Accel-Redirect when proxying through nginx`() {
        documentClient.upload(bucketEnv.data, Document("test", byteArrayOf(0x33, 0x22, 0x11), "text/plain"))

        val response = documentClient.responseAttachment(bucketEnv.data, "test", null)
        assertEquals(HttpStatus.OK, response.statusCode)
        assertNull(response.headers["Location"])
        assertNotNull(response.headers["X-Accel-Redirect"])
    }

    @Test
    fun `upload-download round trip with get`() {
        documentClient.upload(bucketEnv.data, Document("test", byteArrayOf(0x11, 0x33, 0x22), "text/plain"))

        val document = documentClient.get(bucketEnv.data, "test")

        assertContentEquals(byteArrayOf(0x11, 0x33, 0x22), document.bytes)
    }

    @Test
    fun `responseAttachment works without filename`() {
        documentClient.upload(bucketEnv.data, Document("test", byteArrayOf(0x22, 0x11, 0x33), "text/csv"))

        val response = documentClient.responseAttachment(bucketEnv.data, "test", null)
        val s3Url = responseEntityToS3URL(response)
        val (_, s3response, s3data) = http.get(s3Url).response()

        assertEquals("text/csv", s3response.headers["Content-Type"].first())
        assertEquals("attachment", s3response.headers["Content-Disposition"].first())
        assertContentEquals(byteArrayOf(0x22, 0x11, 0x33), s3data.get())
    }

    @Test
    fun `responseAttachment works with filename`() {
        documentClient.upload(bucketEnv.data, Document("test", byteArrayOf(0x33, 0x11, 0x22), "application/pdf"))

        val response = documentClient.responseAttachment(bucketEnv.data, "test", "overridden-filename.pdf")
        val s3Url = responseEntityToS3URL(response)
        val (_, s3response, s3data) = http.get(s3Url).response()

        assertEquals("application/pdf", s3response.headers["Content-Type"].first())
        assertEquals("attachment; filename=\"overridden-filename.pdf\"", s3response.headers["Content-Disposition"].first())
        assertContentEquals(byteArrayOf(0x33, 0x11, 0x22), s3data.get())
    }

    @Test
    fun `responseInline works`() {
        documentClient.upload(bucketEnv.data, Document("test", byteArrayOf(0x12, 0x34, 0x56), "text/plain"))

        val response = documentClient.responseInline(bucketEnv.data, "test")
        val s3Url = responseEntityToS3URL(response)
        val (_, s3response, s3data) = http.get(s3Url).response()

        assertEquals("text/plain", s3response.headers["Content-Type"].first())
        assertEquals(listOf(), s3response.headers["Content-Disposition"])
        assertContentEquals(byteArrayOf(0x12, 0x34, 0x56), s3data.get())
    }
}
