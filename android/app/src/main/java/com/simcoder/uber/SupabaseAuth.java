package com.simcoder.uber;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.IOException;

public final class SupabaseAuth {
    private static final String PREFS = "supabase_session";
    private static final String ACCESS_TOKEN = "access_token";
    private static final String USER_ID = "user_id";
    private static final String USER_EMAIL = "user_email";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final OkHttpClient CLIENT = new OkHttpClient();

    private SupabaseAuth() {
    }

    public interface CallbackResult {
        void onSuccess(String userId, String email);
        void onError(String message);
    }

    public interface ProfileResult {
        void onSuccess(String role);
        void onMissing();
        void onError(String message);
    }

    public static void signIn(Context context, String email, String password, CallbackResult callback) {
        requestAuth(context, "token?grant_type=password", email, password, callback);
    }

    public static void signUp(Context context, String email, String password, CallbackResult callback) {
        requestAuth(context, "signup", email, password, callback);
    }

    public static void signInWithGoogle(Context context, String idToken, CallbackResult callback) {
        String body = "{\"provider\":\"google\",\"id_token\":\"" + escape(idToken) + "\"}";
        Request request = new Request.Builder()
                .url(context.getString(R.string.supabase_url) + "/auth/v1/token?grant_type=id_token")
                .addHeader("apikey", context.getString(R.string.supabase_publishable_key))
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(body, JSON))
                .build();
        enqueueSession(context, request, callback);
    }

    public static void resetPassword(Context context, String email, CallbackResult callback) {
        Request request = new Request.Builder()
                .url(context.getString(R.string.supabase_url) + "/auth/v1/recover")
                .addHeader("apikey", context.getString(R.string.supabase_publishable_key))
                .post(RequestBody.create("{\"email\":\"" + escape(email) + "\"}", JSON))
                .build();
        CLIENT.newCall(request).enqueue(result(callback));
    }

    public static boolean isSignedIn(Context context) {
        return getAccessToken(context) != null;
    }

    public static String getAccessToken(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(ACCESS_TOKEN, null);
    }

    public static String getUserId(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(USER_ID, null);
    }

        public static String getUserEmail(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(USER_EMAIL, "");
        }

        public static void createProfile(Context context, String name, String role, CallbackResult callback) {
        String body = "{\"id\":\"" + escape(getUserId(context)) + "\",\"role\":\"" + escape(role)
            + "\",\"name\":\"" + escape(name) + "\",\"email\":\"" + escape(getUserEmail(context)) + "\"}";
        Request request = new Request.Builder()
            .url(context.getString(R.string.supabase_url) + "/rest/v1/profiles")
            .addHeader("apikey", context.getString(R.string.supabase_publishable_key))
            .addHeader("Authorization", "Bearer " + getAccessToken(context))
            .addHeader("Prefer", "return=minimal")
            .post(RequestBody.create(body, JSON))
            .build();
        CLIENT.newCall(request).enqueue(result(callback));
        }

    public static void signOut(Context context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply();
    }

    public static void getProfile(Context context, ProfileResult callback) {
        Request request = new Request.Builder()
                .url(context.getString(R.string.supabase_url) + "/rest/v1/profiles?id=eq." + getUserId(context) + "&select=role")
                .addHeader("apikey", context.getString(R.string.supabase_publishable_key))
                .addHeader("Authorization", "Bearer " + getAccessToken(context))
                .build();
        CLIENT.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException error) {
                callback.onError(error.getMessage() == null ? "Network error" : error.getMessage());
            }
            @Override public void onResponse(Call call, Response response) throws IOException {
                String payload = response.body() == null ? "" : response.body().string();
                if (!response.isSuccessful()) {
                    callback.onError(readError(payload));
                    return;
                }
                if (payload.equals("[]")) {
                    callback.onMissing();
                    return;
                }
                JsonObject profile = JsonParser.parseString(payload).getAsJsonArray().get(0).getAsJsonObject();
                callback.onSuccess(profile.get("role").getAsString());
            }
        });
    }

    private static void requestAuth(Context context, String path, String email, String password,
                                    CallbackResult callback) {
        String body = "{\"email\":\"" + escape(email) + "\",\"password\":\"" + escape(password) + "\"}";
        Request request = new Request.Builder()
                .url(context.getString(R.string.supabase_url) + "/auth/v1/" + path)
                .addHeader("apikey", context.getString(R.string.supabase_publishable_key))
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(body, JSON))
                .build();
        enqueueSession(context, request, callback);
    }

    private static void enqueueSession(Context context, Request request, CallbackResult callback) {
        CLIENT.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException error) {
                callback.onError(error.getMessage() == null ? "Network error" : error.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String payload = response.body() == null ? "" : response.body().string();
                if (!response.isSuccessful()) {
                    callback.onError(readError(payload));
                    return;
                }
                JsonObject json = JsonParser.parseString(payload).getAsJsonObject();
                String token = json.has("access_token") ? json.get("access_token").getAsString() : null;
                JsonObject user = json.has("user") ? json.getAsJsonObject("user") : null;
                String userId = user != null && user.has("id") ? user.get("id").getAsString() : null;
                String userEmail = user != null && user.has("email") ? user.get("email").getAsString() : null;
                if (token == null || userId == null) {
                    callback.onError("Email confirmation may be required");
                    return;
                }
                context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                        .putString(ACCESS_TOKEN, token)
                        .putString(USER_ID, userId)
                    .putString(USER_EMAIL, userEmail == null ? "" : userEmail)
                        .apply();
                callback.onSuccess(userId, userEmail);
            }
        });
    }

    private static Callback result(CallbackResult callback) {
        return new Callback() {
            @Override
            public void onFailure(Call call, IOException error) {
                callback.onError(error.getMessage() == null ? "Network error" : error.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String payload = response.body() == null ? "" : response.body().string();
                if (!response.isSuccessful()) {
                    callback.onError(readError(payload));
                    return;
                }
                callback.onSuccess(null, null);
            }
        };
    }

    private static String readError(String payload) {
        try {
            JsonObject json = JsonParser.parseString(payload).getAsJsonObject();
            if (json.has("msg")) return json.get("msg").getAsString();
            if (json.has("error_description")) return json.get("error_description").getAsString();
        } catch (Exception ignored) {
        }
        return "Authentication error";
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
