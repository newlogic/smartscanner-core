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
package org.idpass.smartscanner.lib.utils.extension

import android.content.res.Resources


fun Double.Companion.noValue() = 0.0

fun Int.Companion.noValue() = 0

/**
 * Converts pixel to dp
 */
val Int.toDp: Int get() = (this / Resources.getSystem().displayMetrics.density).toInt()

/**
 * Converts dp to pixel
 */
val Int.toPx: Int get() = (this * Resources.getSystem().displayMetrics.density).toInt()