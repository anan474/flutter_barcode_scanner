package com.amolg.flutterbarcodescanner;

import com.amolg.flutterbarcodescanner.camera.GraphicOverlay;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.barcode.Barcode;

class BarcodeGraphicTracker extends Tracker<Barcode> {
    private final GraphicOverlay<BarcodeGraphic> mOverlay;
    private final BarcodeGraphic mGraphic;
    private final BarcodeUpdateListener mBarcodeUpdateListener;

    public interface BarcodeUpdateListener {
        void onBarcodeDetected(Barcode barcode);
    }

    BarcodeGraphicTracker(GraphicOverlay<BarcodeGraphic> overlay, BarcodeGraphic graphic, BarcodeUpdateListener listener) {
        mOverlay = overlay;
        mGraphic = graphic;
        mBarcodeUpdateListener = listener;
    }

    @Override
    public void onNewItem(int id, Barcode item) {
        mGraphic.setId(id);
        if (mBarcodeUpdateListener != null) {
            mBarcodeUpdateListener.onBarcodeDetected(item);
        }
    }

    @Override
    public void onUpdate(Detector.Detections<Barcode> detectionResults, Barcode item) {
        mOverlay.add(mGraphic);
        mGraphic.updateItem(item);
    }

    @Override
    public void onMissing(Detector.Detections<Barcode> detectionResults) {
        mOverlay.remove(mGraphic);
    }

    @Override
    public void onDone() {
        mOverlay.remove(mGraphic);
    }
}