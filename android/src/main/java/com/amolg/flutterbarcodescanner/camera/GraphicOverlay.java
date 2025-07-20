/*
 * Copyright (C) The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.amolg.flutterbarcodescanner.camera;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import com.google.android.gms.vision.CameraSource;

import java.util.HashSet;
import java.util.Set;

public class GraphicOverlay<T extends GraphicOverlay.Graphic> extends View {
    private final Object mLock = new Object();
    private int mPreviewWidth;
    private float mWidthScaleFactor = 1.0f;
    private int mPreviewHeight;
    private float mHeightScaleFactor = 1.0f;
    private int mFacing = CameraSource.CAMERA_FACING_BACK;
    private final Set<T> mGraphics = new HashSet<>();

    private Paint mFinderMaskPaint;
    private Paint mFinderBorderPaint;
    private int lineColor = Color.parseColor("#FFFFFF");
    private String scanMode;


    public static abstract class Graphic {
        private final GraphicOverlay mOverlay;

        public Graphic(GraphicOverlay overlay) {
            mOverlay = overlay;
        }
        public abstract void draw(Canvas canvas);
        public GraphicOverlay getOverlay() {
            return mOverlay;
        }
    }

    public GraphicOverlay(Context context, AttributeSet attrs) {
        super(context, attrs);
        setLayerType(LAYER_TYPE_SOFTWARE, null);
        mFinderMaskPaint = new Paint();
        mFinderMaskPaint.setColor(Color.BLACK);
        mFinderMaskPaint.setStyle(Paint.Style.FILL);
        mFinderMaskPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        mFinderBorderPaint = new Paint();
        mFinderBorderPaint.setColor(lineColor);
        mFinderBorderPaint.setStrokeWidth(8);
        mFinderBorderPaint.setStyle(Paint.Style.STROKE);
    }

    public void setLineColor(String colorString) {
        try {
            lineColor = Color.parseColor(colorString);
            mFinderBorderPaint.setColor(lineColor);
        } catch (Exception e) {
            // Ignore
        }
    }

    public void setScanMode(String mode) {
        this.scanMode = mode;
    }


    public void clear() {
        synchronized (mLock) {
            mGraphics.clear();
        }
        postInvalidate();
    }

    public void add(T graphic) {
        synchronized (mLock) {
            mGraphics.add(graphic);
        }
        postInvalidate();
    }

    public void remove(T graphic) {
        synchronized (mLock) {
            mGraphics.remove(graphic);
        }
        postInvalidate();
    }

    public void setCameraInfo(int previewWidth, int previewHeight) {
        synchronized (mLock) {
            mPreviewWidth = previewWidth;
            mPreviewHeight = previewHeight;
        }
        postInvalidate();
    }

    public float translateX(float x) {
        if (mFacing == CameraSource.CAMERA_FACING_FRONT) {
            return getWidth() - (x * mWidthScaleFactor);
        } else {
            return x * mWidthScaleFactor;
        }
    }

    public float translateY(float y) {
        return y * mHeightScaleFactor;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        synchronized (mLock) {
            if ((mPreviewWidth != 0) && (mPreviewHeight != 0)) {
                mWidthScaleFactor = (float) getWidth() / (float) mPreviewWidth;
                mHeightScaleFactor = (float) getHeight() / (float) mPreviewHeight;
            }

            for (Graphic graphic : mGraphics) {
                graphic.draw(canvas);
            }
        }
        int canvasWidth = canvas.getWidth();
        int canvasHeight = canvas.getHeight();
        RectF rect = new RectF(0, 0, canvasWidth, canvasHeight);
        float rectWidth = canvasWidth * 0.8f;
        float rectHeight;
        if ("QR".equalsIgnoreCase(scanMode)) {
            rectHeight = canvasWidth * 0.8f;
        } else {
            rectHeight = canvasWidth * 0.5f;
        }
        float left = (canvasWidth - rectWidth) / 2;
        float top = (canvasHeight - rectHeight) / 2;
        float right = left + rectWidth;
        float bottom = top + rectHeight;
        canvas.drawRect(left,top,right,bottom,mFinderBorderPaint);
    }
}