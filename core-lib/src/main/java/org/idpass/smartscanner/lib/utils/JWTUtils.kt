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

import org.apache.commons.codec.binary.Base64
import java.nio.charset.Charset
import java.security.*
import java.security.interfaces.ECPublicKey
import java.security.spec.X509EncodedKeySpec
import java.util.regex.Pattern

object JWTUtils {

    const val configurationPublicKey = "-----BEGIN PUBLIC KEY-----\n" +
            "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEEVs/o5+uQbTjL3chynL4wXgUg2R9\n" +
            "q9UU8I5mEovUf86QZ7kOBIjJwqnzD1omageEHWwHdBO6B+dFabmdT9POxg==\n" +
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

    /**
     * Generate public key from ECDSASHA256 algo
     *
     */
    fun generatePublicKey(publicKeyString : String) : ECPublicKey {
        val keyPairGenerator : KeyFactory = KeyFactory.getInstance("EC")
        val keySpecPublic = X509EncodedKeySpec(Base64.decodeBase64(publicKeyString))
        return keyPairGenerator.generatePublic(keySpecPublic) as ECPublicKey
    }

    fun lookupVerificationKey(keyId: String?, publicKey: String): Key {
        // TODO remove usage of hardcoded keys?
        val key: String = if (keyId == "CONF") publicKey else configurationPublicKey
        return generatePublicKey(key.removeEncapsulationBoundaries())
    }

    /**
     * verify JWT via signature
     *
     */
    // TODO fix verify signature algo, as it always returns false
    @Throws(NoSuchAlgorithmException::class, InvalidKeyException::class, SignatureException::class)
    fun verifySignature(jwt: String, key: String = configurationPublicKey): Boolean {
        val splitJwt = jwt.split(".")
        val headerStr = splitJwt[0]
        val payloadStr = splitJwt[1]
        val signatureStr = splitJwt[2]
        val signature = Signature.getInstance("SHA256withECDSA")
        signature.initVerify(generatePublicKey(key.removeEncapsulationBoundaries()))
        signature.update("$headerStr.$payloadStr".toByteArray(Charset.forName("UTF-8")))
        return signature.verify(Base64.decodeBase64(signatureStr))
    }
}
