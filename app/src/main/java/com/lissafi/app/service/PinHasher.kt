package com.lissafi.app.service

import java.security.MessageDigest

/**
 * Hachage du PIN admin au repos : on ne stocke jamais le PIN en clair (ni
 * dans SQLite ni dans les logs de synchro). SHA-256 + sel statique — le PIN
 * étant un code 4 chiffres, ce n'est pas une KDF lente (bcrypt/Argon2), mais
 * cela suffit pour une donnée vestigiale non utilisée comme authentifiant ;
 * surtout, il n'est plus jamais synchronisé vers le cloud.
 */
object PinHasher {

    private const val SALT = "lissafi::admin-pin::v1"

    fun hash(pin: String): String {
        val input = SALT + pin
        val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    fun verify(pin: String, storedHash: String): Boolean =
        storedHash.isNotBlank() && hash(pin) == storedHash
}
