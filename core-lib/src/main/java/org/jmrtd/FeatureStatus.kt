/*
 * JMRTD - A Java API for accessing machine readable travel documents.
 *
 * Copyright (C) 2006 - 2018  The JMRTD team
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
 *
 */
package org.jmrtd

import android.os.Parcel
import android.os.Parcelable

/**
 * Security features of this identity document.
 *
 * @author The JMRTD team (info@jmrtd.org)
 *
 * @version $Revision: 1559 $
 */
class FeatureStatus : Parcelable {

    private var hasSAC: Verdict? = null
    private var hasBAC: Verdict? = null
    private var hasAA: Verdict? = null
    private var hasEAC: Verdict? = null
    private var hasCA: Verdict? = null

    /**
     * Outcome of a feature presence check.
     *
     * @author The JMRTD team (info@jmrtd.org)
     *
     * @version $Revision: 1559 $
     */
    enum class Verdict {
        UNKNOWN, /* Presence unknown */
        PRESENT, /* Present */
        NOT_PRESENT
        /* Not present */
    }

    constructor() {
        this.hasSAC = Verdict.UNKNOWN
        this.hasBAC = Verdict.UNKNOWN
        this.hasAA = Verdict.UNKNOWN
        this.hasEAC = Verdict.UNKNOWN
        this.hasCA = Verdict.UNKNOWN
    }

    fun setSAC(hasSAC: Verdict) {
        this.hasSAC = hasSAC
    }

    fun hasSAC(): Verdict? {
        return hasSAC
    }


    fun setBAC(hasBAC: Verdict) {
        this.hasBAC = hasBAC
    }

    fun hasBAC(): Verdict? {
        return hasBAC
    }

    fun setAA(hasAA: Verdict) {
        this.hasAA = hasAA
    }

    fun hasAA(): Verdict? {
        return hasAA
    }

    fun setEAC(hasEAC: Verdict) {
        this.hasEAC = hasEAC
    }

    fun hasEAC(): Verdict? {
        return hasEAC
    }

    fun setCA(hasCA: Verdict) {
        this.hasCA = hasCA
    }

    fun hasCA(): Verdict? {
        return hasCA
    }

    constructor(`in`: Parcel) {
        this.hasSAC = if (`in`.readInt() == 1){ Verdict.valueOf(`in`.readString()!!) } else { null }
        this.hasBAC = if (`in`.readInt() == 1){Verdict.valueOf(`in`.readString()!!) } else { null }
        this.hasAA = if (`in`.readInt() == 1){Verdict.valueOf(`in`.readString()!!) } else { null }
        this.hasEAC = if (`in`.readInt() == 1){Verdict.valueOf(`in`.readString()!!) } else { null }
        this.hasCA = if (`in`.readInt() == 1){Verdict.valueOf(`in`.readString()!!) } else { null }
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(if (this.hasSAC!=null) 1 else 0)
        if (this.hasSAC!=null) {
            dest.writeString(this.hasSAC?.name)
        }
        dest.writeInt(if (this.hasBAC!=null) 1 else 0)
        if (this.hasBAC!=null) {
            dest.writeString(this.hasBAC?.name)
        }
        dest.writeInt(if (this.hasAA!=null) 1 else 0)
        if (this.hasAA!=null) {
            dest.writeString(this.hasAA?.name)
        }
        dest.writeInt(if (this.hasEAC!=null) 1 else 0)
        if (this.hasEAC!=null) {
            dest.writeString(this.hasEAC?.name)
        }
        dest.writeInt(if (this.hasCA!=null) 1 else 0)
        if (this.hasCA!=null) {
            dest.writeString(this.hasCA?.name)
        }
    }

    fun summary(ident:String): String {
        return "$ident features: hasSAC = $hasSAC, hasBAC = $hasBAC, hasAA = $hasAA, hasEAC = $hasEAC, hasCA = $hasCA"
    }
    
    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<*> = object : Parcelable.Creator<FeatureStatus> {
            override fun createFromParcel(pc: Parcel): FeatureStatus {
                return FeatureStatus(pc)
            }

            override fun newArray(size: Int): Array<FeatureStatus?> {
                return arrayOfNulls(size)
            }
        }
    }
}