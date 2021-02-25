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
package org.idpass.smartscanner.lib.nfc.details

import android.graphics.Bitmap
import android.os.Parcel
import android.os.Parcelable
import java.util.*

class AdditionalDocumentDetails : Parcelable {

    var endorsementsAndObservations: String? = null
    var dateAndTimeOfPersonalization: String? = null
    var dateOfIssue: String? = null
    var imageOfFront: Bitmap? = null
    var imageOfRear: Bitmap? = null
    var issuingAuthority: String? = null
    var namesOfOtherPersons: List<String>? = null
    var personalizationSystemSerialNumber: String? = null
    var taxOrExitRequirements: String? = null
    var tag: Int = 0
    var tagPresenceList: List<Int>? = null


    constructor() {
        namesOfOtherPersons = ArrayList()
        tagPresenceList = ArrayList()
    }

    constructor(`in`: Parcel) {

        namesOfOtherPersons = ArrayList()
        tagPresenceList = ArrayList()

        this.endorsementsAndObservations = if (`in`.readInt() == 1) `in`.readString() else null
        this.dateAndTimeOfPersonalization = if (`in`.readInt() == 1) `in`.readString() else null
        this.dateOfIssue = if (`in`.readInt() == 1) `in`.readString() else null

        this.imageOfFront = if (`in`.readInt() == 1) `in`.readParcelable(Bitmap::class.java.classLoader) else null
        this.imageOfRear = if (`in`.readInt() == 1) `in`.readParcelable(Bitmap::class.java.classLoader) else null
        this.issuingAuthority = if (`in`.readInt() == 1) `in`.readString() else null

        if (`in`.readInt() == 1) {
            `in`.readList(namesOfOtherPersons as ArrayList<String>, String::class.java.classLoader)
        }

        this.personalizationSystemSerialNumber = if (`in`.readInt() == 1) `in`.readString() else null
        this.taxOrExitRequirements = if (`in`.readInt() == 1) `in`.readString() else null

        tag = `in`.readInt()
        if (`in`.readInt() == 1) {
            `in`.readList(tagPresenceList as ArrayList<Int>, Int::class.java.classLoader)
        }
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(if (endorsementsAndObservations != null) 1 else 0)
        if (endorsementsAndObservations != null) {
            dest.writeString(endorsementsAndObservations)
        }

        dest.writeInt(if (dateAndTimeOfPersonalization != null) 1 else 0)
        if (dateAndTimeOfPersonalization != null) {
            dest.writeString(dateAndTimeOfPersonalization)
        }

        dest.writeInt(if (dateOfIssue != null) 1 else 0)
        if (dateOfIssue != null) {
            dest.writeString(dateOfIssue)
        }

        dest.writeInt(if (imageOfFront != null) 1 else 0)
        if (imageOfFront != null) {
            dest.writeParcelable(imageOfFront, flags)
        }

        dest.writeInt(if (imageOfRear != null) 1 else 0)
        if (imageOfRear != null) {
            dest.writeParcelable(imageOfRear, flags)
        }

        dest.writeInt(if (issuingAuthority != null) 1 else 0)
        if (issuingAuthority != null) {
            dest.writeString(issuingAuthority)
        }

        dest.writeInt(if (namesOfOtherPersons != null) 1 else 0)
        if (namesOfOtherPersons != null) {
            dest.writeList(namesOfOtherPersons)
        }

        dest.writeInt(if (personalizationSystemSerialNumber != null) 1 else 0)
        if (personalizationSystemSerialNumber != null) {
            dest.writeString(personalizationSystemSerialNumber)
        }

        dest.writeInt(if (taxOrExitRequirements != null) 1 else 0)
        if (taxOrExitRequirements != null) {
            dest.writeString(taxOrExitRequirements)
        }

        dest.writeInt(tag)
        dest.writeInt(if (tagPresenceList != null) 1 else 0)
        if (tagPresenceList != null) {
            dest.writeList(tagPresenceList)
        }
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<*> = object : Parcelable.Creator<AdditionalDocumentDetails> {
            override fun createFromParcel(pc: Parcel): AdditionalDocumentDetails {
                return AdditionalDocumentDetails(pc)
            }

            override fun newArray(size: Int): Array<AdditionalDocumentDetails?> {
                return arrayOfNulls(size)
            }
        }
    }
}
