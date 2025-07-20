package com.amolg.flutterbarcodescanner;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import androidx.annotation.NonNull;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry;
import io.flutter.plugin.common.PluginRegistry.Registrar;

public class FlutterBarcodeScannerPlugin implements FlutterPlugin, ActivityAware, MethodCallHandler, PluginRegistry.ActivityResultListener {
    private static final String CHANNEL = "flutter_barcode_scanner";
    private Activity activity;
    private Result pendingResult;
    private MethodChannel channel;

    // --- V1 Embedding Registration ---
    // This is still needed for apps that haven't migrated to V2.
    public static void registerWith(Registrar registrar) {
        final FlutterBarcodeScannerPlugin instance = new FlutterBarcodeScannerPlugin();
        instance.onAttachedToEngine(registrar.messenger());
        instance.activity = registrar.activity();
        registrar.addActivityResultListener(instance);
    }

    // --- V2 Embedding Registration ---
    @Override
    public void onAttachedToEngine(@NonNull FlutterPluginBinding binding) {
        channel = new MethodChannel(binding.getBinaryMessenger(), CHANNEL);
        channel.setMethodCallHandler(this);
    }

    @Override
    public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
        channel.setMethodCallHandler(null);
    }

    @Override
    public void onAttachedToActivity(@NonNull ActivityPluginBinding binding) {
        activity = binding.getActivity();
        binding.addActivityResultListener(this);
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
    // --- End V2 Embedding ---

    @Override
    public void onMethodCall(MethodCall call, @NonNull Result result) {
        if (pendingResult != null) {
            result.error("ALREADY_ACTIVE", "Barcode scanning is already active", null);
            return;
        }

        pendingResult = result;

        if (call.method.equals("scanBarcode")) {
            String-v scanMode = call.argument("scanMode");
            String lineColor = call.argument("lineColor");
            String cancelButtonText = call.argument("cancelButtonText");
            boolean isShowFlashIcon = call.argument("isShowFlashIcon");

            if (activity == null) {
                result.error("NO_ACTIVITY", "Plugin is not attached to an activity.", null);
                return;
            }

            Intent intent = new Intent(activity, BarcodeScannerActivity.class);
            intent.putExtra(BarcodeScannerActivity.SCAN_MODE, scanMode);
            intent.putExtra(BarcodeScannerActivity.LINE_COLOR, lineColor);
            intent.putExtra(BarcodeScannerActivity.CANCEL_BUTTON_TEXT, cancelButtonText);
            intent.putExtra(BarcodeScannerActivity.SHOW_FLASH_ICON, isShowFlashIcon);
            activity.startActivityForResult(intent, BarcodeScannerActivity.REQUEST_CODE);
        } else {
            result.notImplemented();
        }
    }

    @Override
    public boolean onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == BarcodeScannerActivity.REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                if (data != null) {
                    String-v barcode = data.getStringExtra(BarcodeScannerActivity.SCAN_RESULT);
                    pendingResult.success(barcode);
                } else {
                    pendingResult.success("-1");
                }
            } else {
                pendingResult.success("-1");
            }
            pendingResult = null;
            return true;
        }
        return false;
    }
}