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
package com.amolg.flutterbarcodescanner;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.amolg.flutterbarcodescanner.camera.CameraSource;
import com.amolg.flutterbarcodescanner.camera.CameraSourcePreview;
import com.amolg.flutterbarcodescanner.camera.GraphicOverlay;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.vision.MultiProcessor;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;

import java.io.IOException;

public class BarcodeCaptureActivity extends AppCompatActivity implements BarcodeGraphicTracker.BarcodeUpdateListener {
    private static final int RC_HANDLE_GMS = 9001;
    private static final int RC_HANDLE_CAMERA_PERM = 2;

    public static final String SCAN_RESULT = "SCAN_RESULT";
    public static final String SCAN_MODE = "SCAN_MODE";
    public static final String LINE_COLOR = "LINE_COLOR";
    public static final String CANCEL_BUTTON_TEXT = "CANCEL_BUTTON_TEXT";
    public static final String SHOW_FLASH_ICON = "SHOW_FLASH_ICON";
    public static final String CONTINUOUS_SCAN = "CONTINUOUS_SCAN";
    public static final int REQUEST_CODE = 1001;

    private CameraSource mCameraSource;
    private CameraSourcePreview mPreview;
    private GraphicOverlay<BarcodeGraphic> mGraphicOverlay;
    private ImageView imgViewBarcodeCaptureUseFlash;
    private boolean isContinuousScan = false;
    private boolean isShowFlashIcon = false;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.barcode_capture);

        mPreview = findViewById(R.id.preview);
        mGraphicOverlay = findViewById(R.id.graphicOverlay);
        imgViewBarcodeCaptureUseFlash = findViewById(R.id.imgViewBarcodeCaptureUseFlash);
        TextView txtViewCancel = findViewById(R.id.txtViewCancel);

        Intent intent = getIntent();
        if (intent != null) {
            isShowFlashIcon = intent.getBooleanExtra(SHOW_FLASH_ICON, false);
            isContinuousScan = intent.getBooleanExtra(CONTINUOUS_SCAN, false);
            String cancelButtonText = intent.getStringExtra(CANCEL_BUTTON_TEXT);
            String lineColor = intent.getStringExtra(LINE_COLOR);

            if (cancelButtonText != null) {
                txtViewCancel.setText(cancelButtonText);
            }
            if (lineColor != null) {
                mGraphicOverlay.setLineColor(lineColor);
            }
        }

        imgViewBarcodeCaptureUseFlash.setVisibility(isShowFlashIcon ? View.VISIBLE : View.GONE);
        imgViewBarcodeCaptureUseFlash.setOnClickListener(v -> {
            if (mCameraSource != null) {
                if (mCameraSource.getFlashMode() != null && mCameraSource.getFlashMode().equals(CameraSource.FLASH_MODE_TORCH)) {
                    mCameraSource.setFlashMode(CameraSource.FLASH_MODE_OFF);
                    imgViewBarcodeCaptureUseFlash.setBackgroundResource(R.drawable.ic_flash_off);
                } else {
                    mCameraSource.setFlashMode(CameraSource.FLASH_MODE_TORCH);
                    imgViewBarcodeCaptureUseFlash.setBackgroundResource(R.drawable.ic_flash_on);
                }
            }
        });

        txtViewCancel.setOnClickListener(v -> {
            setResult(Activity.RESULT_CANCELED);
            finish();
        });

        int rc = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        if (rc == PackageManager.PERMISSION_GRANTED) {
            createCameraSource();
        } else {
            requestCameraPermission();
        }
    }

    private void requestCameraPermission() {
        final String[] permissions = new String[]{Manifest.permission.CAMERA};
        ActivityCompat.requestPermissions(this, permissions, RC_HANDLE_CAMERA_PERM);
    }

    @SuppressLint("InlinedApi")
    private void createCameraSource() {
        BarcodeDetector barcodeDetector = new BarcodeDetector.Builder(this).build();
        BarcodeTrackerFactory barcodeFactory = new BarcodeTrackerFactory(mGraphicOverlay, this);
        barcodeDetector.setProcessor(new MultiProcessor.Builder<>(barcodeFactory).build());

        if (!barcodeDetector.isOperational()) {
            IntentFilter lowstorageFilter = new IntentFilter(Intent.ACTION_DEVICE_STORAGE_LOW);
            boolean hasLowStorage = registerReceiver(null, lowstorageFilter) != null;
            if (hasLowStorage) {
                Toast.makeText(this, "Low Storage", Toast.LENGTH_LONG).show();
            }
        }

        CameraSource.Builder builder = new CameraSource.Builder(getApplicationContext(), barcodeDetector)
                .setFacing(CameraSource.CAMERA_FACING_BACK)
                .setRequestedPreviewSize(1600, 1024)
                .setRequestedFps(15.0f);

        mCameraSource = builder.build();
    }

    @Override
    protected void onResume() {
        super.onResume();
        startCameraSource();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mPreview != null) {
            mPreview.stop();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mPreview != null) {
            mPreview.release();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == RC_HANDLE_CAMERA_PERM) {
            if (grantResults.length != 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                createCameraSource();
                return;
            }
            Toast.makeText(this, "Camera permission is required.", Toast.LENGTH_LONG).show();
            finish();
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void startCameraSource() throws SecurityException {
        int code = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(getApplicationContext());
        if (code != ConnectionResult.SUCCESS) {
            GoogleApiAvailability.getInstance().getErrorDialog(this, code, RC_HANDLE_GMS).show();
        }

        if (mCameraSource != null) {
            try {
                mPreview.start(mCameraSource, mGraphicOverlay);
            } catch (IOException e) {
                mCameraSource.release();
                mCameraSource = null;
            }
        }
    }

    @Override
    public void onBarcodeDetected(Barcode barcode) {
        Intent data = new Intent();
        data.putExtra(SCAN_RESULT, barcode.rawValue);
        setResult(Activity.RESULT_OK, data);
        if (!isContinuousScan) {
            finish();
        }
    }
}