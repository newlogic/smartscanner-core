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
package org.idpass.smartscanner.lib.nfc


data class NFCResult(
        val image: String? = null,
        var givenNames: String? = null,
        var surname: String? = null,
        var nameOfHolder: String? = null,
        var dateOfBirth: String? = null,
        var gender: String? = null,
        var documentNumber: String? = null,
        var dateOfExpiry: String? = null,
        var issuingState: String? = null,
        var nationality: String? = null,
        var otherNames: String? = null,
        var custodyInformation: String? = null,
        var profession: String? = null,
        var telephone: String? = null,
        var title: String? = null,
        var dateAndTimeOfPersonalization: String? = null,
        var dateOfIssue: String? = null,
        var endorsementsAndObservations: String? = null,
        var issuingAuthority: String? = null,
        var personalizationSystemSerialNumber: String? = null,
        var taxOrExitRequirements: String? = null,
        var mrzOptional: String? = null,
        var mrzOptional2: String? = null
)