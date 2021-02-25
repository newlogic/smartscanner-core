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

import android.os.Parcel
import android.os.Parcelable
import java.util.*

class AdditionalPersonDetails : Parcelable {

    var custodyInformation: String? = null
    var fullDateOfBirth: String? = null
    var nameOfHolder: String? = null
    var otherNames: List<String>? = null
    var otherValidTDNumbers: List<String>? = null
    var permanentAddress: List<String>? = null
    var personalNumber: String? = null
    var personalSummary: String? = null
    var placeOfBirth: List<String>? = null
    var profession: String? = null
    var proofOfCitizenship: ByteArray? = null
    var tag: Int = 0
    var tagPresenceList: List<Int>? = null
    var telephone: String? = null
    var title: String? = null

    constructor() {
        otherNames = ArrayList()
        otherValidTDNumbers = ArrayList()
        permanentAddress = ArrayList()
        placeOfBirth = ArrayList()
        tagPresenceList = ArrayList()
    }

    constructor(`in`: Parcel) {

        otherNames = ArrayList()
        otherValidTDNumbers = ArrayList()
        permanentAddress = ArrayList()
        placeOfBirth = ArrayList()
        tagPresenceList = ArrayList()

        this.custodyInformation = if (`in`.readInt() == 1) `in`.readString() else null
        this.fullDateOfBirth = if (`in`.readInt() == 1) `in`.readString() else null
        this.nameOfHolder = if (`in`.readInt() == 1) `in`.readString() else null
        if (`in`.readInt() == 1) {
            `in`.readList(otherNames as ArrayList<String>, String::class.java.classLoader)
        }
        if (`in`.readInt() == 1) {
            `in`.readList(otherValidTDNumbers as ArrayList<String>, String::class.java.classLoader)
        }
        if (`in`.readInt() == 1) {
            `in`.readList(permanentAddress as ArrayList<String>, String::class.java.classLoader)
        }
        this.personalNumber = if (`in`.readInt() == 1) `in`.readString() else null
        this.personalSummary = if (`in`.readInt() == 1) `in`.readString() else null
        if (`in`.readInt() == 1) {
            `in`.readList(placeOfBirth as ArrayList<String>, String::class.java.classLoader)
        }
        this.profession = if (`in`.readInt() == 1) `in`.readString() else null
        if (`in`.readInt() == 1) {
            this.proofOfCitizenship = ByteArray(`in`.readInt())
            `in`.readByteArray(this.proofOfCitizenship!!)
        }
        tag = `in`.readInt()
        if (`in`.readInt() == 1) {
            `in`.readList(tagPresenceList as ArrayList<Int>, Int::class.java.classLoader)
        }

        this.telephone = if (`in`.readInt() == 1) `in`.readString() else null
        this.title = if (`in`.readInt() == 1) `in`.readString() else null
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(if (custodyInformation != null) 1 else 0)
        if (custodyInformation != null) {
            dest.writeString(custodyInformation)
        }

        dest.writeInt(if (fullDateOfBirth != null) 1 else 0)
        if (fullDateOfBirth != null) {
            dest.writeString(fullDateOfBirth)
        }

        dest.writeInt(if (nameOfHolder != null) 1 else 0)
        if (nameOfHolder != null) {
            dest.writeString(nameOfHolder)
        }
        dest.writeInt(if (otherNames != null) 1 else 0)
        if (otherNames != null) {
            dest.writeList(otherNames)
        }

        dest.writeInt(if (otherValidTDNumbers != null) 1 else 0)
        if (otherValidTDNumbers != null) {
            dest.writeList(otherValidTDNumbers)
        }

        dest.writeInt(if (permanentAddress != null) 1 else 0)
        if (permanentAddress != null) {
            dest.writeList(permanentAddress)
        }

        dest.writeInt(if (personalNumber != null) 1 else 0)
        if (personalNumber != null) {
            dest.writeString(personalNumber)
        }

        dest.writeInt(if (personalSummary != null) 1 else 0)
        if (personalSummary != null) {
            dest.writeString(personalSummary)
        }

        dest.writeInt(if (placeOfBirth != null) 1 else 0)
        if (placeOfBirth != null) {
            dest.writeList(placeOfBirth)
        }

        dest.writeInt(if (profession != null) 1 else 0)
        if (profession != null) {
            dest.writeString(profession)
        }

        dest.writeInt(if (proofOfCitizenship != null) 1 else 0)
        if (proofOfCitizenship != null) {
            dest.writeInt(proofOfCitizenship!!.size)
            dest.writeByteArray(proofOfCitizenship)
        }

        dest.writeInt(tag)
        dest.writeInt(if (tagPresenceList != null) 1 else 0)
        if (tagPresenceList != null) {
            dest.writeList(tagPresenceList)
        }

        dest.writeInt(if (telephone != null) 1 else 0)
        if (telephone != null) {
            dest.writeString(telephone)
        }

        dest.writeInt(if (title != null) 1 else 0)
        if (title != null) {
            dest.writeString(title)
        }
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<*> = object : Parcelable.Creator<AdditionalPersonDetails> {
            override fun createFromParcel(pc: Parcel): AdditionalPersonDetails {
                return AdditionalPersonDetails(pc)
            }
            override fun newArray(size: Int): Array<AdditionalPersonDetails?> {
                return arrayOfNulls(size)
            }
        }
    }
}