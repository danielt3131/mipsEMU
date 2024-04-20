/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3 or later.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package io.github.danielt3131.mipsemu.ui;

import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.Log;


/**
 * A class to calculate the needed font size for the memory display
 * <p>
 * Currently used for phones
 */
public class FontUtils {
    private float textSizePx;
    private final float memoryViewWidth;
    private Paint paint;
    private static final String binaryTestString = "0xFFFFFF: 11111111 11111111 11111111 11111111";
    public FontUtils(float textSizePx, float memoryViewWidth, Typeface typeface) {
        this.textSizePx = textSizePx;
        this.memoryViewWidth = memoryViewWidth;
        paint = new Paint();
        paint.setTypeface(typeface);
        paint.setTextSize(textSizePx);
        Log.d("FontUtils", String.format("%f | %f", textSizePx, memoryViewWidth));
    }

    public float binaryTextSize() {
        float textSize = textSizePx;
        float width = paint.measureText(binaryTestString, 0, binaryTestString.length());
        // Increase font size by 1 until exceeds the width;
        while (width < memoryViewWidth) {
            paint.setTextSize(++textSize);
            width = paint.measureText(binaryTestString, 0, binaryTestString.length());
            Log.d("Width", width + "| " + textSize);
        }
        textSize--;
        return textSize;
    }
}
