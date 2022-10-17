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
package org.idpass.smartscanner.lib.platform.utils

import org.apache.commons.codec.binary.Base64
import java.security.KeyFactory
import java.security.interfaces.ECPublicKey
import java.security.spec.X509EncodedKeySpec
import java.util.regex.Pattern

object JWTUtils {

    const val publicKeyFromConfig = "-----BEGIN PUBLIC KEY-----\n" +
            "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEEVs/o5+uQbTjL3chynL4wXgUg2R9\n" +
            "q9UU8I5mEovUf86QZ7kOBIjJwqnzD1omageEHWwHdBO6B+dFabmdT9POxg==\n" +
            "-----END PUBLIC KEY-----"

    /**
     * Checks if string is a JWT Token or not
     *
     */
    fun String.isJWT() : Boolean {
        val pattern = "^[A-Za-z0-9-_=]+\\\\.[A-Za-z0-9-_=]+\\\\.[A-Za-z0-9-_.+/=]*\$"
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
}
