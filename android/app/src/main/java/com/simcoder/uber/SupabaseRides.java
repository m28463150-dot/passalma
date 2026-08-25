package com.simcoder.uber;

import android.content.Context;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public final class SupabaseRides {
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final OkHttpClient CLIENT = new OkHttpClient();

    private SupabaseRides() {
    }

    public interface Result {
        void onSuccess(String payload);
        void onError(String message);
    }

    public static void findAvailableDrivers(Context context, double latitude, double longitude,
                                            double radiusKm, Result result) {
        String body = "{\"pickup_lat\":" + latitude + ",\"pickup_lng\":" + longitude
                + ",\"radius_km\":" + radiusKm + "}";
        rpc(context, "find_available_drivers", body, result);
    }

    public static void acceptRide(Context context, String rideId, Result result) {
        rpc(context, "accept_ride", "{\"ride_uuid\":\"" + escape(rideId) + "\"}", result);
    }

    public static void updateRideStatus(Context context, String rideId, String status, Result result) {
        Request request = request(context, "/rest/v1/rides?id=eq." + rideId,
                "{\"status\":\"" + escape(status) + "\"}", "PATCH");
        CLIENT.newCall(request).enqueue(callback(result));
    }

    public static void updateDriverLocation(Context context, double latitude, double longitude,
                                            boolean working, Result result) {
        String body = "{\"driver_id\":\"" + SupabaseAuth.getUserId(context) + "\",\"location\":\"SRID=4326;POINT("
                + longitude + " " + latitude + ")\",\"is_working\":" + working + "}";
        Request request = new Request.Builder()
            .url(context.getString(R.string.supabase_url) + "/rest/v1/driver_locations?on_conflict=driver_id")
            .addHeader("apikey", context.getString(R.string.supabase_publishable_key))
            .addHeader("Authorization", "Bearer " + SupabaseAuth.getAccessToken(context))
            .addHeader("Content-Type", "application/json")
            .addHeader("Prefer", "resolution=merge-duplicates,return=representation")
            .post(RequestBody.create(body, JSON))
            .build();
        CLIENT.newCall(request).enqueue(callback(result));
    }

    public static void fetchRequestedRides(Context context, Result result) {
        Request request = new Request.Builder()
                .url(context.getString(R.string.supabase_url) + "/rest/v1/rides?status=eq.requested&driver_id=is.null&select=*")
                .addHeader("apikey", context.getString(R.string.supabase_publishable_key))
                .addHeader("Authorization", "Bearer " + SupabaseAuth.getAccessToken(context))
                .build();
        CLIENT.newCall(request).enqueue(callback(result));
    }

        public static void fetchHistory(Context context, String column, Result result) {
        Request request = new Request.Builder()
            .url(context.getString(R.string.supabase_url) + "/rest/v1/rides?" + column + "=eq."
                + SupabaseAuth.getUserId(context) + "&status=neq.cancelled&order=created_at.desc&select=*")
            .addHeader("apikey", context.getString(R.string.supabase_publishable_key))
            .addHeader("Authorization", "Bearer " + SupabaseAuth.getAccessToken(context))
            .build();
        CLIENT.newCall(request).enqueue(callback(result));
        }

    private static void rpc(Context context, String name, String body, Result result) {
        Request request = request(context, "/rest/v1/rpc/" + name, body, "POST");
        CLIENT.newCall(request).enqueue(callback(result));
    }

    public static void fetchRide(Context context, String rideId, Result result) {
        Request request = new Request.Builder()
                .url(context.getString(R.string.supabase_url) + "/rest/v1/rides?id=eq." + rideId + "&select=*")
                .addHeader("apikey", context.getString(R.string.supabase_publishable_key))
                .addHeader("Authorization", "Bearer " + SupabaseAuth.getAccessToken(context))
                .build();
        CLIENT.newCall(request).enqueue(callback(result));
    }

    public static void fetchDriverLocation(Context context, String driverId, Result result) {
        Request request = new Request.Builder()
                .url(context.getString(R.string.supabase_url) + "/rest/v1/driver_locations?driver_id=eq." + driverId + "&select=*")
                .addHeader("apikey", context.getString(R.string.supabase_publishable_key))
                .addHeader("Authorization", "Bearer " + SupabaseAuth.getAccessToken(context))
                .build();
        CLIENT.newCall(request).enqueue(callback(result));
    }

    public static void fetchLatestDriverRide(Context context, Result result) {
        Request request = new Request.Builder()
                .url(context.getString(R.string.supabase_url) + "/rest/v1/rides?driver_id=eq."
                        + SupabaseAuth.getUserId(context) + "&order=created_at.desc&limit=1&select=*")
                .addHeader("apikey", context.getString(R.string.supabase_publishable_key))
                .addHeader("Authorization", "Bearer " + SupabaseAuth.getAccessToken(context))
                .build();
        CLIENT.newCall(request).enqueue(callback(result));
    }

    private static Request request(Context context, String path, String body, String method) {
        Request.Builder builder = new Request.Builder()
                .url(context.getString(R.string.supabase_url) + path)
                .addHeader("apikey", context.getString(R.string.supabase_publishable_key))
                .addHeader("Authorization", "Bearer " + SupabaseAuth.getAccessToken(context))
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=representation");
        RequestBody requestBody = RequestBody.create(body, JSON);
        return "PATCH".equals(method) ? builder.patch(requestBody).build() : builder.post(requestBody).build();
    }

    private static Callback callback(Result result) {
        return new Callback() {
            @Override public void onFailure(Call call, IOException error) {
                result.onError(error.getMessage() == null ? "Network error" : error.getMessage());
            }
            @Override public void onResponse(Call call, Response response) throws IOException {
                String payload = response.body() == null ? "" : response.body().string();
                if (response.isSuccessful()) result.onSuccess(payload);
                else result.onError(readError(payload));
            }
        };
    }

    private static String readError(String payload) {
        try {
            JsonObject error = JsonParser.parseString(payload).getAsJsonObject();
            if (error.has("message")) return error.get("message").getAsString();
            if (error.has("hint")) return error.get("hint").getAsString();
        } catch (Exception ignored) {
        }
        return "Ride request failed";
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}