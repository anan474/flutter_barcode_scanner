package com.amolg.flutterbarcodescanner;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import com.amolg.flutterbarcodescanner.camera.GraphicOverlay;
import com.google.android.gms.vision.barcode.Barcode;

public class BarcodeGraphic extends GraphicOverlay.Graphic {
    private int mId;
    private final Paint mRectPaint;
    private final Paint mTextPaint;
    private volatile Barcode mBarcode;

    BarcodeGraphic(GraphicOverlay overlay) {
        super(overlay);

        mRectPaint = new Paint();
        mRectPaint.setColor(Color.RED);
        mRectPaint.setStyle(Paint.Style.STROKE);
        mRectPaint.setStrokeWidth(4.0f);

        mTextPaint = new Paint();
        mTextPaint.setColor(Color.RED);
        mTextPaint.setTextSize(36.0f);
    }

    public int getId() {
        return mId;
    }

    public void setId(int id) {
        this.mId = id;
    }

    public Barcode getBarcode() {
        return mBarcode;
    }

    void updateItem(Barcode barcode) {
        mBarcode = barcode;
        // Corrected: call postInvalidate on the overlay
        getOverlay().postInvalidate();
    }

    @Override
    public void draw(Canvas canvas) {
        Barcode barcode = mBarcode;
        if (barcode == null) {
            return;
        }

        RectF rect = new RectF(barcode.getBoundingBox());
        // Corrected: call coordinate translation on the overlay
        rect.left = getOverlay().translateX(rect.left);
        rect.top = getOverlay().translateY(rect.top);
        rect.right = getOverlay().translateX(rect.right);
        rect.bottom = getOverlay().translateY(rect.bottom);
        canvas.drawRect(rect, mRectPaint);

        canvas.drawText(barcode.rawValue, rect.left, rect.bottom, mTextPaint);
    }
}