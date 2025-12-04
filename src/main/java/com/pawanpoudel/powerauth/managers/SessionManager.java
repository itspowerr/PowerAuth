package com.pawanpoudel.powerauth.managers;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SessionManager {

    private final Set<UUID> loggedInPlayers = ConcurrentHashMap.newKeySet();

    public void login(UUID uuid) {
        loggedInPlayers.add(uuid);
    }

    public void logout(UUID uuid) {
        loggedInPlayers.remove(uuid);
    }

    public boolean isLoggedIn(UUID uuid) {
        return loggedInPlayers.contains(uuid);
    }
}
