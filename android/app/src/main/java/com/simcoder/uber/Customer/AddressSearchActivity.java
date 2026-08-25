package com.simcoder.uber.Customer;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.appcompat.app.AppCompatActivity;

import com.simcoder.uber.R;

public class AddressSearchActivity extends AppCompatActivity {

    public static final String EXTRA_NAME = "address_name";
    public static final String EXTRA_LATITUDE = "address_latitude";
    public static final String EXTRA_LONGITUDE = "address_longitude";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_address_search);

        WebView addressSearch = findViewById(R.id.address_search);
        WebSettings settings = addressSearch.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        addressSearch.setWebViewClient(new WebViewClient());
        addressSearch.addJavascriptInterface(new AddressBridge(), "Android");
        addressSearch.loadUrl("file:///android_asset/address_search.html");
    }

    private class AddressBridge {
        @JavascriptInterface
        public void selectAddress(String name, String latitude, String longitude) {
            try {
                double parsedLatitude = Double.parseDouble(latitude);
                double parsedLongitude = Double.parseDouble(longitude);
                runOnUiThread(() -> {
                    Intent result = new Intent();
                    result.putExtra(EXTRA_NAME, name);
                    result.putExtra(EXTRA_LATITUDE, parsedLatitude);
                    result.putExtra(EXTRA_LONGITUDE, parsedLongitude);
                    setResult(Activity.RESULT_OK, result);
                    finish();
                });
            } catch (NumberFormatException exception) {
                runOnUiThread(AddressSearchActivity.this::finish);
            }
        }
    }
}
