package com.simcoder.uber.Login;

import android.os.Bundle;

import androidx.annotation.Nullable;

import com.google.android.material.snackbar.Snackbar;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.simcoder.uber.R;
import com.simcoder.uber.SupabaseAuth;

import org.jetbrains.annotations.NotNull;

/**
 * Fragment Responsible for Logging in an existing user
 */
public class LoginFragment extends Fragment implements View.OnClickListener {

    private EditText mEmail, mPassword;


    private View view;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NotNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if (view == null)
            view = inflater.inflate(R.layout.fragment_login, container, false);
        else
            container.removeView(view);


        return view;
    }

    @Override
    public void onViewCreated(@NotNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initializeObjects();
    }

    /**
     * Sends an email to the email that's on the email input for the user to reset the password
     */
    private void forgotPassword() {
        if (mEmail.getText().toString().trim().length() > 0)
            SupabaseAuth.resetPassword(requireContext(), mEmail.getText().toString(), new SupabaseAuth.CallbackResult() {
                        @Override public void onSuccess(String userId, String email) {
                            Snackbar.make(view.findViewById(R.id.layout), "Email Sent", Snackbar.LENGTH_LONG).show();
                        }
                        @Override public void onError(String message) {
                            Snackbar.make(view.findViewById(R.id.layout), "Something went wrong", Snackbar.LENGTH_LONG).show();
                        }
                    });
    }

    /**
     * Logs in the user
     */
    private void login() {
        final String email = mEmail.getText().toString();
        final String password = mPassword.getText().toString();

        if(mEmail.getText().length()==0) {
            mEmail.setError("please fill this field");
            return;
        }
        if(mPassword.getText().length()==0) {
            mPassword.setError("please fill this field");
            return;
        }

        SupabaseAuth.signIn(requireContext(), email, password, new SupabaseAuth.CallbackResult() {
            @Override public void onSuccess(String userId, String email) {
                requireActivity().runOnUiThread(() -> requireActivity().recreate());
            }
            @Override public void onError(String message) {
                requireActivity().runOnUiThread(() -> Snackbar.make(view.findViewById(R.id.layout), message, Snackbar.LENGTH_SHORT).show());
            }
        });
    }


    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.forgotButton:
                forgotPassword();
                break;
            case R.id.login:
                login();
                break;
        }
    }


    /**
     * Initializes the design Elements and calls clickListeners for them
     */
    private void initializeObjects() {
        mEmail = view.findViewById(R.id.email);
        mPassword = view.findViewById(R.id.password);
        TextView mForgotButton = view.findViewById(R.id.forgotButton);
        Button mLogin = view.findViewById(R.id.login);


        mForgotButton.setOnClickListener(this);
        mLogin.setOnClickListener(this);

    }
}