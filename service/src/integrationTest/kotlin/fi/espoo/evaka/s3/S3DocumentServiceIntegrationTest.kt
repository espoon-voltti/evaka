// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.s3

import fi.espoo.evaka.BucketEnv
import fi.espoo.evaka.FullApplicationTest
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.presigner.S3Presigner

class S3DocumentServiceIntegrationTest : FullApplicationTest(resetDbBeforeEach = false) {
    @Autowired private lateinit var s3Client: S3Client

    @Autowired private lateinit var s3Presigner: S3Presigner

    @Autowired private lateinit var bucketEnv: BucketEnv

    private lateinit var documentClient: DocumentService

    @BeforeEach
    fun beforeEach() {
        documentClient =
            S3DocumentService(s3Client, s3Presigner, bucketEnv.copy(proxyThroughNginx = true))
    }

    @Test
    fun `redirects when not proxying through nginx`() {
        val documentClientNoProxy =
            S3DocumentService(s3Client, s3Presigner, bucketEnv.copy(proxyThroughNginx = false))
        documentClientNoProxy.upload(
            bucketEnv.data,
            Document("test", byteArrayOf(0x11, 0x22, 0x33), "text/plain")
        )

        val response = documentClientNoProxy.responseAttachment(bucketEnv.data, "test", null)
        assertEquals(HttpStatus.FOUND, response.statusCode)
        assertNotNull(response.headers["Location"])
        assertNull(response.headers["X-Accel-Redirect"])
    }

    @Test
    fun `uses X-Accel-Redirect when proxying through nginx`() {
        documentClient.upload(
            bucketEnv.data,
            Document("test", byteArrayOf(0x33, 0x22, 0x11), "text/plain")
        )

        val response = documentClient.responseAttachment(bucketEnv.data, "test", null)
        assertEquals(HttpStatus.OK, response.statusCode)
        assertNull(response.headers["Location"])
        assertNotNull(response.headers["X-Accel-Redirect"])
    }

    @Test
    fun `upload-download round trip with get`() {
        documentClient.upload(
            bucketEnv.data,
            Document("test", byteArrayOf(0x11, 0x33, 0x22), "text/plain")
        )

        val document = documentClient.get(bucketEnv.data, "test")

        assertContentEquals(byteArrayOf(0x11, 0x33, 0x22), document.bytes)
    }

    @Test
    fun `responseAttachment works without filename`() {
        documentClient.upload(
            bucketEnv.data,
            Document("test", byteArrayOf(0x22, 0x11, 0x33), "text/csv")
        )

        val response = documentClient.responseAttachment(bucketEnv.data, "test", null)
        val s3Url = responseEntityToS3URL(response)
        val (_, s3response, s3data) = http.get(s3Url).response()

        assertEquals("text/csv", s3response.headers["Content-Type"].first())
        assertContentEquals(byteArrayOf(0x22, 0x11, 0x33), s3data.get())
        assertEquals(listOf("attachment"), response.headers["Content-Disposition"])
    }

    @Test
    fun `responseAttachment works with filename`() {
        documentClient.upload(
            bucketEnv.data,
            Document("test", byteArrayOf(0x33, 0x11, 0x22), "application/pdf")
        )

        val response =
            documentClient.responseAttachment(bucketEnv.data, "test", "overridden-filename.pdf")
        val s3Url = responseEntityToS3URL(response)
        val (_, s3response, s3data) = http.get(s3Url).response()

        assertEquals("application/pdf", s3response.headers["Content-Type"].first())
        assertContentEquals(byteArrayOf(0x33, 0x11, 0x22), s3data.get())
        assertEquals(
            listOf(
                "attachment; filename=\"=?UTF-8?Q?overridden-filename.pdf?=\"; filename*=UTF-8''overridden-filename.pdf"
            ),
            response.headers["Content-Disposition"]
        )
    }

    @Test
    fun `responseInline works`() {
        documentClient.upload(
            bucketEnv.data,
            Document("test", byteArrayOf(0x12, 0x34, 0x56), "text/plain")
        )

        val response =
            documentClient.responseInline(bucketEnv.data, "test", "overridden-filename.txt")
        val s3Url = responseEntityToS3URL(response)
        val (_, s3response, s3data) = http.get(s3Url).response()

        assertEquals("text/plain", s3response.headers["Content-Type"].first())
        assertContentEquals(byteArrayOf(0x12, 0x34, 0x56), s3data.get())
        assertEquals(
            listOf(
                "inline; filename=\"=?UTF-8?Q?overridden-filename.txt?=\"; filename*=UTF-8''overridden-filename.txt"
            ),
            response.headers["Content-Disposition"]
        )
    }
}
