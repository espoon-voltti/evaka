// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.mcp

import evaka.core.mcp.McpOAuthController.Companion.isAcceptableRedirectUri
import evaka.core.mcp.McpOAuthController.Companion.redirectUriMatches
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test

/**
 * These two functions are the only thing standing between a self-registered OAuth client and an
 * open redirect from the employee UI, so every branch is pinned down here.
 */
class McpOAuthRedirectUriTest {
    @Test
    fun `https and loopback http urls and native app schemes are accepted`() {
        listOf(
                "https://example.com/callback",
                "https://example.com/callback?foo=bar",
                "http://localhost/callback",
                "http://localhost:53000/callback",
                "http://127.0.0.1:53000/callback",
                "http://[::1]:53000/callback",
                "vscode://redhat.java/oauth",
                "com.example.app:/oauth2redirect",
                "cursor://anysphere.cursor-retrieval/oauth/callback",
            )
            .forEach { assertTrue(isAcceptableRedirectUri(it), it) }
    }

    @Test
    fun `script and data urls, remote http, fragments and garbage are rejected`() {
        listOf(
                "javascript:alert(1)",
                "JavaScript:alert(1)",
                "data:text/html,<script>alert(1)</script>",
                "file:///etc/passwd",
                "vbscript:msgbox(1)",
                "http://example.com/callback",
                "http://evil.localhost/callback",
                "https://example.com/callback#fragment",
                "https:///callback",
                "not a url",
                "",
                "/relative/path",
                "custom scheme://with space",
            )
            .forEach { assertFalse(isAcceptableRedirectUri(it), it) }
    }

    @Test
    fun `a requested redirect uri must equal the registered one`() {
        assertTrue(redirectUriMatches("https://example.com/cb", "https://example.com/cb"))
        assertFalse(redirectUriMatches("https://example.com/cb", "https://example.com/cb2"))
        assertFalse(redirectUriMatches("https://example.com/cb", "https://example.com/cb?x=1"))
        assertFalse(redirectUriMatches("https://example.com/cb", "https://evil.example.com/cb"))
        assertFalse(redirectUriMatches("https://example.com:443/cb", "https://example.com/cb"))
    }

    @Test
    fun `only loopback http redirect uris may vary the port`() {
        assertTrue(redirectUriMatches("http://localhost:53000/cb", "http://localhost:61000/cb"))
        assertTrue(redirectUriMatches("http://127.0.0.1/cb", "http://127.0.0.1:61000/cb"))
        assertFalse(redirectUriMatches("http://localhost:53000/cb", "http://127.0.0.1:53000/cb"))
        assertFalse(redirectUriMatches("http://localhost:53000/cb", "http://localhost:61000/other"))
        assertFalse(redirectUriMatches("http://localhost:53000/cb", "https://localhost:53000/cb"))
        assertFalse(
            redirectUriMatches("https://example.com:8443/cb", "https://example.com:9443/cb")
        )
        assertFalse(redirectUriMatches("http://localhost:53000/cb", "not a url"))
    }
}
