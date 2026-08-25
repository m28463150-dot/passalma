package com.simcoder.uber.Login;

import android.content.Intent;
import android.os.Bundle;

import com.addisonelliott.segmentedbutton.SegmentedButtonGroup;
import androidx.appcompat.app.AppCompatActivity;

import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.simcoder.uber.R;
import com.simcoder.uber.SupabaseAuth;


/**
 * Fragment Responsible for registering a new user
 */
public class DetailsActivity extends AppCompatActivity implements View.OnClickListener {

    private EditText mName;

    private SegmentedButtonGroup mRadioGroup;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details);
        initializeObjects();
    }


    /**
     * Register the user, but before that check if every field is correct.
     * After that registers the user and creates an entry for it oin the database
     */
    private void register() {
        if (mName.getText().length() == 0) {
            mName.setError("please fill this field");
            return;
        }


        final String name = mName.getText().toString();
        final String accountType;
        int selectId = mRadioGroup.getPosition();

        if (selectId == 1) {
            accountType = "Drivers";
        } else {
            accountType = "Customers";
        }

        String role = accountType.equals("Drivers") ? "driver" : "customer";
        SupabaseAuth.createProfile(this, name, role, new SupabaseAuth.CallbackResult() {
            @Override public void onSuccess(String userId, String email) {
                runOnUiThread(() -> {
                    Intent intent = new Intent(DetailsActivity.this, LauncherActivity.class);
                    startActivity(intent);
                    finish();
                });
            }
            @Override public void onError(String message) {
                runOnUiThread(() -> mName.setError(message));
            }
        });
    }


    /**
     * Initializes the design Elements and calls clickListeners for them
     */
    private void initializeObjects() {
        mName = findViewById(R.id.name);
        Button mRegister = findViewById(R.id.register);
        mRadioGroup = findViewById(R.id.radioRealButtonGroup);

        mRadioGroup.setPosition(0, false);
        mRegister.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.register) {
            register();
        }
    }
}