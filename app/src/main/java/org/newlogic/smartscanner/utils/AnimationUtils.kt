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
package org.newlogic.smartscanner.utils

import android.view.View
import android.view.View.VISIBLE
import android.view.animation.Animation
import android.view.animation.Interpolator
import android.view.animation.Transformation
import androidx.core.view.animation.PathInterpolatorCompat

object AnimationUtils {

    /**
     * This is the method for expanding/collapsing view
     * @param view the view to perform expand or collapse
     */
    fun expandCollapse(view: View, originalHeight : Int) {
        val expand = view.visibility == View.GONE
        //Create a path for animation
        val easeInOutQuart: Interpolator =
            PathInterpolatorCompat.create(
                0.77f,
                0f,
                0.175f,
                1f
            )
        //Measure the view
        view.measure(
            View.MeasureSpec.makeMeasureSpec(
                (view.parent as View).width,
                View.MeasureSpec.EXACTLY
            ),
            View.MeasureSpec.makeMeasureSpec(
                0,
                View.MeasureSpec.UNSPECIFIED
            )
        )

        //expand or collapse animation
        val height = view.measuredHeight
        val duration = (height / view.context.resources.displayMetrics.density).toInt()
        val animation: Animation = object : Animation() {
            override fun applyTransformation(
                interpolatedTime: Float,
                t: Transformation?
            ) {
                if (expand) {
                    view.layoutParams.height = 1
                    view.visibility = VISIBLE
                    if (interpolatedTime == 1f) {
                        view.layoutParams.height = originalHeight
                    } else {
                        view.layoutParams.height = (height * interpolatedTime).toInt()
                    }
                    view.requestLayout()
                } else {
                    if (interpolatedTime == 1f) {
                        view.visibility = View.GONE
                    } else {
                        view.layoutParams.height = height - (height * interpolatedTime).toInt()
                        view.requestLayout()
                    }
                }
            }
            override fun willChangeBounds(): Boolean {
                return true
            }
        }
        animation.interpolator = easeInOutQuart
        animation.duration = duration.toLong()
        view.startAnimation(animation)
    }
}