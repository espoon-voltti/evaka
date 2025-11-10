// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.sftp

import fi.espoo.evaka.Sensitive
import fi.espoo.evaka.SftpEnv
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test

private val env =
    SftpEnv(
        "localhost",
        2222,
        listOf("AAAAC3NzaC1lZDI1NTE5AAAAICADdlntyAKbOUGQDkdzdhQBu12jZjb0KmxLyrklMXTq"),
        "foo",
        Sensitive("temp"),
        null,
    )
private const val password = "pass"
private val privateKey =
    """
    -----BEGIN OPENSSH PRIVATE KEY-----
    b3BlbnNzaC1rZXktdjEAAAAABG5vbmUAAAAEbm9uZQAAAAAAAAABAAAAMwAAAAtzc2gtZW
    QyNTUxOQAAACBKQkRdkXMfuHv/1PWMJ6Thchv2PeE+uEkUbbpf+ce/mQAAAJjUgHxz1IB8
    cwAAAAtzc2gtZWQyNTUxOQAAACBKQkRdkXMfuHv/1PWMJ6Thchv2PeE+uEkUbbpf+ce/mQ
    AAAEBOKiwR898c7d20IF4F4O6++awDPFfhoeDlH+t09hwEw0pCRF2Rcx+4e//U9YwnpOFy
    G/Y94T64SRRtul/5x7+ZAAAADmV2YWthX2xvY2FsL2l0AQIDBAUGBw==
    -----END OPENSSH PRIVATE KEY-----
    """
        .trimIndent()

class SftpClientTest {
    @Test
    fun `password authentication works`() {
        val client = SftpClient(env.copy(password = Sensitive(password), privateKey = null))
        "hello world".byteInputStream(Charsets.UTF_8).use { client.put(it, "upload/test.txt") }
        assertEquals("hello world", client.getAsString("upload/test.txt", Charsets.UTF_8))
    }

    @Test
    fun `private key authentication works`() {
        val client = SftpClient(env.copy(password = null, privateKey = Sensitive(privateKey)))
        "hello world".byteInputStream(Charsets.UTF_8).use { client.put(it, "upload/test.txt") }
        assertEquals("hello world", client.getAsString("upload/test.txt", Charsets.UTF_8))
    }
}
