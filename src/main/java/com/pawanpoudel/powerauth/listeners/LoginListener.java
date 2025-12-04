package com.pawanpoudel.powerauth.listeners;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.pawanpoudel.powerauth.PowerAuth;
import com.pawanpoudel.powerauth.managers.PremiumManager;
import com.pawanpoudel.powerauth.utils.EncryptionUtil;

import javax.crypto.SecretKey;
import java.net.URL;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class LoginListener {

    private final PowerAuth plugin;
    private final PremiumManager premiumManager;
    private final ConcurrentHashMap<String, byte[]> verifyTokens = new ConcurrentHashMap<>();

    public LoginListener(PowerAuth plugin) {
        this.plugin = plugin;
        this.premiumManager = new PremiumManager();

        // 1. Intercept Login Start - Check if premium and send encryption request
        ProtocolLibrary.getProtocolManager()
                .addPacketListener(new PacketAdapter(plugin, ListenerPriority.NORMAL, PacketType.Login.Client.START) {
                    @Override
                    public void onPacketReceiving(PacketEvent event) {
                        if (event.isCancelled())
                            return;

                        String playerName = event.getPacket().getGameProfiles().read(0).getName();

                        try {
                            // Check if player is premium (blocking with timeout to prevent race condition)
                            UUID premiumUUID = premiumManager.getPremiumUUID(playerName)
                                    .get(3, java.util.concurrent.TimeUnit.SECONDS);

                            if (premiumUUID != null) {
                                // Player is premium - initiate encryption
                                byte[] verifyToken = EncryptionUtil.generateVerifyToken();
                                verifyTokens.put(playerName, verifyToken);

                                PacketContainer encryptionRequest = ProtocolLibrary.getProtocolManager()
                                        .createPacket(PacketType.Login.Server.ENCRYPTION_BEGIN);
                                encryptionRequest.getStrings().write(0, ""); // Server ID (empty string)
                                encryptionRequest.getByteArrays().write(0,
                                        EncryptionUtil.getKeyPair().getPublic().getEncoded());
                                encryptionRequest.getByteArrays().write(1, verifyToken);

                                ProtocolLibrary.getProtocolManager().sendServerPacket(event.getPlayer(),
                                        encryptionRequest);
                                event.setCancelled(true); // Cancel normal offline login
                            }
                            // If not premium, let it pass through to normal offline login
                        } catch (java.util.concurrent.TimeoutException e) {
                            // Timeout - assume offline player
                            plugin.getLogger()
                                    .warning("Premium check timeout for " + playerName + ", treating as offline");
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });

        // 2. Intercept Encryption Response - Verify and authenticate premium players
        ProtocolLibrary.getProtocolManager().addPacketListener(
                new PacketAdapter(plugin, ListenerPriority.NORMAL, PacketType.Login.Client.ENCRYPTION_BEGIN) {
                    @Override
                    public void onPacketReceiving(PacketEvent event) {
                        if (event.isCancelled())
                            return;

                        String foundPlayer = null;
                        for (String playerName : verifyTokens.keySet()) {
                            if (event.getPlayer().getName().equals(playerName)) {
                                foundPlayer = playerName;
                                break;
                            }
                        }

                        if (foundPlayer == null) {
                            event.getPlayer().kickPlayer("§cInvalid encryption response.");
                            return;
                        }

                        try {
                            PacketContainer packet = event.getPacket();
                            byte[] sharedSecretEncrypted = packet.getByteArrays().read(0);
                            byte[] verifyTokenEncrypted = packet.getByteArrays().read(1);

                            PublicKey publicKey = EncryptionUtil.getPublicKey();
                            SecretKey sharedSecret = EncryptionUtil.decryptSharedSecret(publicKey,
                                    sharedSecretEncrypted);
                            byte[] verifyToken = EncryptionUtil.decryptVerifyToken(publicKey, verifyTokenEncrypted);

                            if (sharedSecret == null || verifyToken == null) {
                                event.getPlayer().kickPlayer("§cEncryption failed.");
                                return;
                            }

                            if (!Arrays.equals(verifyToken, verifyTokens.get(foundPlayer))) {
                                event.getPlayer().kickPlayer("§cVerification token mismatch.");
                                return;
                            }

                            // Verify with Mojang Session Server
                            // We must compute the server hash exactly as the client did.
                            String serverId = EncryptionUtil.getServerIdHash("", publicKey, sharedSecret);
                            if (serverId == null) {
                                event.getPlayer().kickPlayer("§cFailed to compute server hash.");
                                return;
                            }

                            String authUrl = "https://sessionserver.mojang.com/session/minecraft/hasJoined?username="
                                    + foundPlayer + "&serverId=" + serverId;

                            URL url = new URL(authUrl);
                            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                            conn.setConnectTimeout(5000);
                            conn.setReadTimeout(5000);

                            java.util.Scanner s = new java.util.Scanner(conn.getInputStream()).useDelimiter("\\A");
                            String result = s.hasNext() ? s.next() : "";
                            s.close();

                            if (result.contains("id")) {
                                JsonObject json = new JsonParser().parse(result).getAsJsonObject();
                                String id = json.get("id").getAsString();
                                UUID uuid = UUID.fromString(
                                        id.replaceFirst("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5"));

                                // IMPORTANT: We must verify the UUID matches the one we expect (or update it)
                                // The session server returns the real UUID.

                                PacketContainer success = ProtocolLibrary.getProtocolManager()
                                        .createPacket(PacketType.Login.Server.SUCCESS);
                                success.getGameProfiles().write(0, new WrappedGameProfile(uuid, foundPlayer));
                                ProtocolLibrary.getProtocolManager().sendServerPacket(event.getPlayer(), success);

                                LoginListener.this.plugin.getSessionManager().login(uuid);
                                LoginListener.this.plugin.getDatabaseManager().setPremium(uuid, true);

                                event.setCancelled(true);
                                verifyTokens.remove(foundPlayer);
                            } else {
                                // Verification failed
                                event.getPlayer().kickPlayer("§cFailed to verify session with Mojang.");
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
    }
}
