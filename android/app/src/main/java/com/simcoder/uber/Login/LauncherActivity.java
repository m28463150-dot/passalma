package com.simcoder.uber.Login;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.onesignal.OneSignal;
import com.simcoder.uber.Customer.CustomerMapActivity;
import com.simcoder.uber.Driver.DriverMapActivity;
import com.simcoder.uber.R;
import com.simcoder.uber.SupabaseAuth;
import com.stripe.android.PaymentConfiguration;

import android.app.AlertDialog;


/**
 * First activity of the app.
 * <p>
 * Responsible for checking if the user is logged in or not and call
 * the AuthenticationActivity or MainActivity depending on that.
 */
public class LauncherActivity extends AppCompatActivity {

    private boolean navigationStarted;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (SupabaseAuth.isSignedIn(this)) {
            checkUserAccType();
        } else {
            Intent intent = new Intent(LauncherActivity.this, AuthenticationActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }
    }

    /**
     * Check user account type, either customer or driver.
     * If it doesn't have a type then start the DetailsActivity for the
     * user to be able to pick one.
     */
    private void checkUserAccType() {
        SupabaseAuth.getProfile(this, new SupabaseAuth.ProfileResult() {
            @Override public void onSuccess(String role) {
                runOnUiThread(() -> openAccount(role.equals("driver") ? DriverMapActivity.class : CustomerMapActivity.class, role));
            }
            @Override public void onMissing() {
                runOnUiThread(() -> openAccount(DetailsActivity.class, null));
            }
            @Override public void onError(String message) {
                runOnUiThread(() -> openAuthentication());
            }
        });
    }

    private void openAccount(Class<?> activityClass, String accountType) {
        if (navigationStarted) {
            return;
        }
        navigationStarted = true;
        if (accountType != null) {
            startApis(accountType);
        }
        Intent intent = new Intent(this, activityClass);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void openAuthentication() {
        if (navigationStarted) {
            return;
        }
        navigationStarted = true;
        SupabaseAuth.signOut(this);
        Intent intent = new Intent(this, AuthenticationActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    /**
     * starts up onesignal and stripe apis
     * @param type - type of the user (customer, driver)
     */
    void startApis(String type) {
        OneSignal.startInit(this).init();
        OneSignal.sendTag("User_ID", SupabaseAuth.getUserId(this));
        PaymentConfiguration.init(
                getApplicationContext(),
                getResources().getString(R.string.publishablekey)
        );
    }
}
