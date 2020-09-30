// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.msg.service.attachments

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.S3Object
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.jupiter.api.assertThrows
import org.junit.runner.RunWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import java.io.ByteArrayInputStream

@RunWith(MockitoJUnitRunner::class)
class S3DocumentServiceTest {

    @Mock
    lateinit var s3: AmazonS3

    @InjectMocks
    lateinit var s3DocumentService: S3DocumentService

    @Test
    fun `Missing key name from s3 bucket path causes exception`() {
        assertThrows<IllegalArgumentException>("Document key is missing from documentUri [s3://bucket]") {
            s3DocumentService.getDocument("s3://bucket")
        }
    }

    @Test
    fun `when scheme is missing from s3 bucket path then exception is thrown`() {
        assertThrows<IllegalArgumentException>("Invalid S3 URI: no hostname") {
            s3DocumentService.getDocument("bucket/subfolder/file.pdf")
        }
    }

    @Test
    fun `when s3 bucket is not found from aws then exception is thrown`() {
        whenever(s3.doesBucketExistV2("bucket")).thenReturn(false)
        assertThrows<IllegalArgumentException>("Bucket [bucket] does not exist") {
            s3DocumentService.getDocument("s3://bucket/subfolder/file.pdf")
        }
    }

    @Test
    fun `get document`() {
        val bucket = "evaka-clubdecisions-dev"
        val key = "fe23ad56-4eff-11e9-be2b-b3aff839cc60"
        val s3Path = "s3://$bucket/$key"
        whenever(s3.doesBucketExistV2(bucket)).thenReturn(true)
        whenever(s3.getObject(bucket, key)).thenReturn(getS3Object())
        val bytes = s3DocumentService.getDocument(s3Path)
        assertThat(bytes).isNotEmpty()
    }

    private fun getS3Object(): S3Object {
        val targetStream = ByteArrayInputStream("pdf".toByteArray())
        val obj = S3Object()
        obj.setObjectContent(targetStream)
        return obj
    }
}
