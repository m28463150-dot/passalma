package com.simcoder.uber;

import android.content.Context;
import android.net.Uri;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStream;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public final class SupabaseProfiles {
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final OkHttpClient CLIENT = new OkHttpClient();

    private SupabaseProfiles() {
    }

    public interface Result {
        void onSuccess(JsonObject profile);
        void onError(String message);
    }

    public static void get(Context context, Result result) {
        request(context, "/rest/v1/profiles?id=eq." + SupabaseAuth.getUserId(context) + "&select=*", null, "GET",
                result);
    }

    public static void update(Context context, String fields, Result result) {
        request(context, "/rest/v1/profiles?id=eq." + SupabaseAuth.getUserId(context), fields, "PATCH", result);
    }

    public static void uploadImage(Context context, Uri uri, Result result) {
        try {
            InputStream input = context.getContentResolver().openInputStream(uri);
            if (input == null) { result.onError("Unable to read image"); return; }
            byte[] bytes = readBytes(input);
            String path = SupabaseAuth.getUserId(context) + ".jpg";
            Request upload = new Request.Builder()
                    .url(context.getString(R.string.supabase_url) + "/storage/v1/object/profile-images/" + path)
                    .addHeader("apikey", context.getString(R.string.supabase_publishable_key))
                    .addHeader("Authorization", "Bearer " + SupabaseAuth.getAccessToken(context))
                    .addHeader("Content-Type", "image/jpeg")
                    .addHeader("x-upsert", "true")
                    .put(RequestBody.create(bytes, MediaType.get("image/jpeg")))
                    .build();
            CLIENT.newCall(upload).enqueue(new Callback() {
                @Override public void onFailure(Call call, IOException error) { result.onError(error.getMessage()); }
                @Override public void onResponse(Call call, Response response) throws IOException {
                    if (!response.isSuccessful()) { result.onError("Image upload failed"); return; }
                    String url = context.getString(R.string.supabase_url) + "/storage/v1/object/public/profile-images/" + path;
                    update(context, "{\"profile_image_url\":\"" + url + "\"}", result);
                }
            });
        } catch (IOException error) {
            result.onError(error.getMessage());
        }
    }

    private static void request(Context context, String path, String body, String method, Result result) {
        Request.Builder builder = new Request.Builder()
                .url(context.getString(R.string.supabase_url) + path)
                .addHeader("apikey", context.getString(R.string.supabase_publishable_key))
                .addHeader("Authorization", "Bearer " + SupabaseAuth.getAccessToken(context))
                .addHeader("Prefer", "return=representation");
        if (body != null) builder.addHeader("Content-Type", "application/json");
        Request request = "GET".equals(method) ? builder.get().build() : builder.patch(RequestBody.create(body, JSON)).build();
        CLIENT.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException error) { result.onError(error.getMessage()); }
            @Override public void onResponse(Call call, Response response) throws IOException {
                String payload = response.body() == null ? "" : response.body().string();
                if (!response.isSuccessful()) { result.onError("Profile request failed"); return; }
                if ("GET".equals(method)) {
                    if (payload.equals("[]")) { result.onError("Profile not found"); return; }
                    result.onSuccess(JsonParser.parseString(payload).getAsJsonArray().get(0).getAsJsonObject());
                } else result.onSuccess(new JsonObject());
            }
        });
    }

    private static byte[] readBytes(InputStream input) throws IOException {
        java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int count;
        while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
        input.close();
        return output.toByteArray();
    }
}
