package com.pawanpoudel.powerauth.managers;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class PremiumManager {

    private static final String MOJANG_API_URL = "https://api.mojang.com/users/profiles/minecraft/";

    public CompletableFuture<UUID> getPremiumUUID(String username) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                URL url = new URL(MOJANG_API_URL + username);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);

                if (connection.getResponseCode() == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    JsonObject json = new JsonParser().parse(response.toString()).getAsJsonObject();
                    String id = json.get("id").getAsString();
                    return parseUUID(id);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        });
    }

    private UUID parseUUID(String id) {
        return UUID.fromString(id.replaceFirst(
                "(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5"));
    }
}
