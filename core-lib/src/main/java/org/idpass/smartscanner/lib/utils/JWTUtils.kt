/*
 * Copyright (C) 2020 Newlogic Pte. Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 *
 */
package org.idpass.smartscanner.lib.utils

import io.jsonwebtoken.*
import org.apache.commons.codec.binary.Base64
import org.json.JSONObject
import java.security.*
import java.security.interfaces.ECPublicKey
import java.security.spec.X509EncodedKeySpec
import java.util.regex.Pattern

object JWTUtils {

    const val configurationPublicKey = "-----BEGIN PUBLIC KEY-----\n" +
            "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEHmAB470dV0Zay8SSo0lSKXaYr2Rj\n" +
            "3niL8ZxPRZfQvmbscSgA8i27z7uf0Z5svVfMa5O7gNDuWQ3FCeRJxDSy/g==\n" +
            "-----END PUBLIC KEY-----"

    /**
     * Checks if string is a JWT Token or not
     *
     */
    fun String.isJWT() : Boolean {
        val pattern = "(^(?:[\\w-]*\\.){2}[\\w-]*\$)"
        val r: Pattern = Pattern.compile(pattern)
        return r.matcher(this).find()
    }

    /**
     * Removes encapsulation boundaries, newlines, and whitespace
     *
     */
    fun String.removeEncapsulationBoundaries(): String {
        return this.replace("\n".toRegex(), "")
            .replace(" ".toRegex(), "")
            .replace("-{5}[a-zA-Z]*-{5}".toRegex(), "")
    }

    fun String.addEncapsulationBoundaries(): String {
        val text = this.removeEncapsulationBoundaries()
        val builder = StringBuilder()
        builder.append("-----BEGIN PUBLIC KEY-----\n")
            .append(text)
            .append("\n-----END PUBLIC KEY-----")

        return builder.toString()
    }

    fun String.isDefaultConfigPublicKey(): Boolean {
        return (this.removeEncapsulationBoundaries()) == (configurationPublicKey.removeEncapsulationBoundaries())
    }

    /**
     * Generate public key from ECDSASHA256 algo
     *
     */
    fun generatePublicKey(publicKeyString : String) : ECPublicKey {
        val keyPairGenerator : KeyFactory = KeyFactory.getInstance("EC")
        val keySpecPublic = X509EncodedKeySpec(Base64.decodeBase64(publicKeyString))
        return keyPairGenerator.generatePublic(keySpecPublic) as ECPublicKey
    }

    fun lookupVerificationKey(keyId : String?, publicKeyScanned: String?) : Key {
        return if (keyId == "CONF" || publicKeyScanned.isNullOrEmpty()) {
            generatePublicKey(configurationPublicKey.removeEncapsulationBoundaries())
        } else {
            generatePublicKey(publicKeyScanned.removeEncapsulationBoundaries())
        }
    }

    /**
     * Get the value from rawValue with verifying signed key provided
     * @param publicKey by default will use system config public key, but can add manually add to specify
     * which public key to use
     */
    fun getWithSignedKey(rawValue: String, publicKey: String?): Jws<Claims> {
        val parser = Jwts.parserBuilder()
            .setSigningKeyResolver(object : SigningKeyResolverAdapter() {
                override fun resolveSigningKey(
                    header: JwsHeader<out JwsHeader<*>>?,
                    claims: Claims?
                ): Key {
                    return lookupVerificationKey(header?.keyId, publicKey)
                }
            }).build()
        return parser.parseClaimsJws(rawValue)
    }

    fun Jws<Claims>.getJsonBody(): JSONObject {
        val json = JSONObject()
        this.body.entries.iterator().forEach { (key, value) -> json.put(key, value) }
        return json
    }

    fun Jws<Claims>.getJsonHeader(): JSONObject {
        val json = JSONObject()
        this.header.entries.iterator().forEach { (key, value) -> json.put(key, value) }
        return json
    }

}
