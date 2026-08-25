package com.simcoder.uber.Customer;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.simcoder.uber.Objects.CustomerObject;
import com.simcoder.uber.R;
import com.simcoder.uber.SupabaseAuth;
import com.simcoder.uber.SupabaseProfiles;

import java.io.IOException;


/**
 * Activity that displays the settings to the customer
 */
public class CustomerSettingsActivity extends AppCompatActivity {

    private EditText mNameField, mPhoneField;

    private ImageView mProfileImage;

    private String userID;

    private Uri resultUri;

    CustomerObject mCustomer;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_settings);

        mNameField = findViewById(R.id.name);
        mPhoneField = findViewById(R.id.phone);

        mProfileImage = findViewById(R.id.profileImage);

        Button mConfirm = findViewById(R.id.confirm);

        userID = SupabaseAuth.getUserId(this);

        mCustomer = new CustomerObject(userID);

        getUserInfo();

        mProfileImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            startActivityForResult(intent, 1);
        });

        mConfirm.setOnClickListener(v -> saveUserInformation());


        setupToolbar();
    }

    /**
     * Sets up toolbar with custom text and a listener
     * to go back to the previous activity
     */
    private void setupToolbar() {
        Toolbar myToolbar = findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);
        getSupportActionBar().setTitle(getString(R.string.settings));
        myToolbar.setTitleTextColor(getResources().getColor(R.color.white));
        ActionBar ab = getSupportActionBar();
        assert ab != null;
        ab.setDisplayHomeAsUpEnabled(true);
        myToolbar.setNavigationOnClickListener(v -> finish());
    }

    /**
     * Fetches current user's info and populates the design elements
     */
    private void getUserInfo(){
        SupabaseProfiles.get(this, new SupabaseProfiles.Result() {
            @Override public void onSuccess(com.google.gson.JsonObject profile) {
                runOnUiThread(() -> {
                    mNameField.setText(profile.get("name").getAsString());
                    mPhoneField.setText(profile.get("phone").getAsString());
                    if (profile.has("profile_image_url") && !profile.get("profile_image_url").getAsString().equals("default"))
                        Glide.with(getApplication()).load(profile.get("profile_image_url").getAsString()).apply(RequestOptions.circleCropTransform()).into(mProfileImage);
                });
            }
            @Override public void onError(String message) {
            }
        });
    }


    /**
     * Saves current user 's info to the database.
     * If the resultUri is not null that means the profile image has been changed
     * and we need to upload it to the storage system and update the database with the new url
     */
    private void saveUserInformation() {
        String mName = mNameField.getText().toString();
        String mPhone = mPhoneField.getText().toString();

        String userInfo = "{\"name\":\"" + mName.replace("\"", "\\\"") + "\",\"phone\":\"" + mPhone.replace("\"", "\\\"") + "\"}";

        if(resultUri != null) {
            SupabaseProfiles.update(this, userInfo, new SupabaseProfiles.Result() {
                @Override public void onSuccess(com.google.gson.JsonObject profile) { SupabaseProfiles.uploadImage(CustomerSettingsActivity.this, resultUri, done()); }
                @Override public void onError(String message) { finish(); }
            });
        }else{
            SupabaseProfiles.update(this, userInfo, done());
        }

    }

    private SupabaseProfiles.Result done() {
        return new SupabaseProfiles.Result() {
            @Override public void onSuccess(com.google.gson.JsonObject profile) { runOnUiThread(CustomerSettingsActivity.this::finish); }
            @Override public void onError(String message) { runOnUiThread(CustomerSettingsActivity.this::finish); }
        };
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == 1 && resultCode == Activity.RESULT_OK){
            resultUri = data.getData();
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), resultUri);
                Glide.with(getApplication())
                        .load(bitmap) // Uri of the picture
                        .apply(RequestOptions.circleCropTransform())
                        .into(mProfileImage);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
