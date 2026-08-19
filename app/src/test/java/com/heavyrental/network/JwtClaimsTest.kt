package com.heavyrental.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Base64

/**
 * Covers the client-side role gate described in specification/product/01-login.md §L1:
 * the login response has no role field, so ROLE_ADMIN/ROLE_DRIVER is read from the
 * access token's `roles` claim. Anything unreadable must fail closed.
 */
class JwtClaimsTest {

    private fun tokenWithPayload(payloadJson: String): String {
        val payload = Base64.getUrlEncoder().withoutPadding()
            .encodeToString(payloadJson.toByteArray(Charsets.UTF_8))
        return "eyJhbGciOiJIUzI1NiJ9.$payload.not-a-real-signature"
    }

    private fun tokenWithRoles(vararg roles: String) =
        tokenWithPayload("""{"sub":"admin@localhost","roles":[${roles.joinToString(",") { "\"$it\"" }}]}""")

    @Test
    fun `reads roles claim`() {
        assertEquals(listOf("ROLE_ADMIN"), JwtClaims.rolesOf(tokenWithRoles("ROLE_ADMIN")))
    }

    @Test
    fun `admin is staff`() {
        assertTrue(JwtClaims.isStaff(tokenWithRoles("ROLE_ADMIN")))
    }

    @Test
    fun `driver is staff`() {
        assertTrue(JwtClaims.isStaff(tokenWithRoles("ROLE_DRIVER")))
    }

    @Test
    fun `customer is not staff`() {
        assertFalse(JwtClaims.isStaff(tokenWithRoles("ROLE_USER")))
    }

    @Test
    fun `staff role among several is enough`() {
        assertTrue(JwtClaims.isStaff(tokenWithRoles("ROLE_USER", "ROLE_ADMIN")))
    }

    @Test
    fun `single role as bare string is accepted`() {
        val token = tokenWithPayload("""{"roles":"ROLE_ADMIN"}""")
        assertEquals(listOf("ROLE_ADMIN"), JwtClaims.rolesOf(token))
        assertTrue(JwtClaims.isStaff(token))
    }

    @Test
    fun `payload lengths needing one and two padding chars both decode`() {
        // "roles" arrays of differing length exercise both len % 4 == 2 and == 3.
        assertTrue(JwtClaims.isStaff(tokenWithRoles("ROLE_ADMIN")))
        assertTrue(JwtClaims.isStaff(tokenWithRoles("ROLE_DRIVER")))
        assertTrue(JwtClaims.isStaff(tokenWithPayload("""{"roles":["ROLE_ADMIN"],"x":"y"}""")))
        assertTrue(JwtClaims.isStaff(tokenWithPayload("""{"roles":["ROLE_ADMIN"],"x":"yz"}""")))
    }

    @Test
    fun `empty roles array is not staff`() {
        assertEquals(emptyList<String>(), JwtClaims.rolesOf(tokenWithRoles()))
        assertFalse(JwtClaims.isStaff(tokenWithRoles()))
    }

    @Test
    fun `missing roles claim fails closed`() {
        val token = tokenWithPayload("""{"sub":"someone@example.sg"}""")
        assertEquals(emptyList<String>(), JwtClaims.rolesOf(token))
        assertFalse(JwtClaims.isStaff(token))
    }

    @Test
    fun `non-string entries are skipped`() {
        val token = tokenWithPayload("""{"roles":[1,{"authority":"ROLE_ADMIN"},"ROLE_DRIVER"]}""")
        assertEquals(listOf("ROLE_DRIVER"), JwtClaims.rolesOf(token))
    }

    @Test
    fun `roles claim of the wrong type fails closed`() {
        assertFalse(JwtClaims.isStaff(tokenWithPayload("""{"roles":{"a":"ROLE_ADMIN"}}""")))
        assertFalse(JwtClaims.isStaff(tokenWithPayload("""{"roles":42}""")))
    }

    @Test
    fun `malformed tokens fail closed without throwing`() {
        listOf(
            "",
            "garbage",
            "only.two",
            "a.b.c.d",
            "eyJhbGciOiJIUzI1NiJ9.!!!not-base64!!!.sig",
            tokenWithPayload("not json at all")
        ).forEach { token ->
            assertEquals("expected no roles for <$token>", emptyList<String>(), JwtClaims.rolesOf(token))
            assertFalse("expected non-staff for <$token>", JwtClaims.isStaff(token))
        }
    }
}
