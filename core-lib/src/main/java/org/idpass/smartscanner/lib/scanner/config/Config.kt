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
package org.idpass.smartscanner.lib.scanner.config

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.idpass.smartscanner.lib.platform.extension.empty

@Parcelize
data class Config(
    val background: String? = null,
    val branding: Boolean? = null,
    val font: String? = null,
    val imageResultType: String? = null,
    val isManualCapture: Boolean? = null,
    val header: String? = null,
    val subHeader: String? = null,
    val label: String? = null,
    val orientation: String? = null
) : Parcelable {
    companion object {
        val default = Config(
            background = String.empty(),
            branding = true,
            font = String.empty(),
            imageResultType = ImageResultType.PATH.value,
            isManualCapture = false,
            header = String.empty(),
            subHeader = String.empty(),
            orientation = Orientation.PORTRAIT.value,
            label = String.empty()
        )
    }
}