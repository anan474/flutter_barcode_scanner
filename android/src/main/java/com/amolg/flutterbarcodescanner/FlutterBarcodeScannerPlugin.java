package com.amolg.flutterbarcodescanner;

import android.app.Activity;
import android.content.Intent;
import androidx.annotation.NonNull;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.ActivityResultListener;

public class FlutterBarcodeScannerPlugin implements FlutterPlugin, ActivityAware, MethodCallHandler, ActivityResultListener {
    private static final String CHANNEL = "flutter_barcode_scanner";
    private Activity activity;
    private Result pendingResult;
    private MethodChannel channel;

    @Override
    public void onAttachedToEngine(@NonNull FlutterPluginBinding binding) {
        channel = new MethodChannel(binding.getBinaryMessenger(), CHANNEL);
        channel.setMethodCallHandler(this);
    }

    @Override
    public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
        if (call.method.equals("scanBarcode")) {
            if (pendingResult != null) {
                return;
            }
            pendingResult = result;
            if (activity == null) {
                result.error("NO_ACTIVITY", "Plugin is not attached to an activity.", null);
                return;
            }

            Intent intent = new Intent(activity, BarcodeCaptureActivity.class);

            // --- THE DEFINITIVE FIX IS HERE ---
            // Get the integer index (0 for QR, 1 for Barcode)
            int scanModeInt = call.argument("scanMode");
            String scanMode;

            // Convert the integer to the String the activity expects
            if (scanModeInt == 0) {
                scanMode = "QR";
            } else {
                scanMode = "BARCODE";
            }
            // --- END OF FIX ---

            intent.putExtra(BarcodeCaptureActivity.SCAN_MODE, scanMode);
            intent.putExtra(BarcodeCaptureActivity.LINE_COLOR, (String) call.argument("lineColor"));
            intent.putExtra(BarcodeCaptureActivity.CANCEL_BUTTON_TEXT, (String) call.argument("cancelButtonText"));
            intent.putExtra(BarcodeCaptureActivity.SHOW_FLASH_ICON, (Boolean) call.argument("isShowFlashIcon"));
            intent.putExtra(BarcodeCaptureActivity.CONTINUOUS_SCAN, false);

            activity.startActivityForResult(intent, BarcodeCaptureActivity.REQUEST_CODE);
        } else {
            result.notImplemented();
        }
    }

    @Override
    public boolean onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == BarcodeCaptureActivity.REQUEST_CODE) {
            if (pendingResult != null) {
                if (resultCode == Activity.RESULT_OK && data != null) {
                    String barcode = data.getStringExtra(BarcodeCaptureActivity.SCAN_RESULT);
                    pendingResult.success(barcode);
                } else {
                    pendingResult.success("-1");
                }
                pendingResult = null;
            }
            return true;
        }
        return false;
    }

    // --- ActivityAware Lifecycle Methods ---
    @Override
    public void onAttachedToActivity(@NonNull ActivityPluginBinding binding) {
        activity = binding.getActivity();
        binding.addActivityResultListener(this);
    }

    @Override
    public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
        channel.setMethodCallHandler(null);
    }

    @Override
    public void onDetachedFromActivity() {
        activity = null;
    }

    @Override
    public void onDetachedFromActivityForConfigChanges() {
        onDetachedFromActivity();
    }

    @Override
    public void onReattachedToActivityForConfigChanges(@NonNull ActivityPluginBinding binding) {
        onAttachedToActivity(binding);
    }
}