package com.simcoder.uber.Login;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.onesignal.OneSignal;
import com.simcoder.uber.Customer.CustomerMapActivity;
import com.simcoder.uber.Driver.DriverMapActivity;
import com.simcoder.uber.R;
import com.stripe.android.PaymentConfiguration;


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

        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() != null) {
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
        String userID = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference users = FirebaseDatabase.getInstance().getReference().child("Users");
        Task<DataSnapshot> customerTask = users.child("Customers").child(userID).get();
        Task<DataSnapshot> driverTask = users.child("Drivers").child(userID).get();

        Tasks.whenAllSuccess(customerTask, driverTask).addOnSuccessListener(results -> {
            DataSnapshot customer = (DataSnapshot) results.get(0);
            DataSnapshot driver = (DataSnapshot) results.get(1);

            if (hasAccountData(customer)) {
                openAccount(CustomerMapActivity.class, "Customers");
            } else if (hasAccountData(driver)) {
                openAccount(DriverMapActivity.class, "Drivers");
            } else {
                openAccount(DetailsActivity.class, null);
            }
        }).addOnFailureListener(error -> openAuthentication());
    }

    private boolean hasAccountData(DataSnapshot snapshot) {
        return snapshot.exists() && snapshot.getChildrenCount() > 0;
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
        FirebaseAuth.getInstance().signOut();
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
        OneSignal.sendTag("User_ID", FirebaseAuth.getInstance().getCurrentUser().getUid());
        OneSignal.setEmail(FirebaseAuth.getInstance().getCurrentUser().getEmail());
        //OneSignal.setInFocusDisplaying(OneSignal.OSInFocusDisplayOption.Notification);
        OneSignal.idsAvailable((userId, registrationId) -> FirebaseDatabase.getInstance().getReference().child("Users").child(type).child(FirebaseAuth.getInstance().getCurrentUser().getUid()).child("notificationKey").setValue(userId));
        PaymentConfiguration.init(
                getApplicationContext(),
                getResources().getString(R.string.publishablekey)
        );
    }
}
