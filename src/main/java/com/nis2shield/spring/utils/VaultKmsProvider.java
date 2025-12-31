package com.nis2shield.spring.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

/**
 * HashiCorp Vault KMS Provider implementation.
 * Integrates with Vault's Transit secrets engine for key management.
 *
 * <p>Configuration example in application.yml:</p>
 * <pre>
 * nis2:
 *   kms:
 *     provider: vault
 *     vault:
 *       address: https://vault.example.com:8200
 *       token: hvs.your-vault-token
 *       transitPath: transit
 *       keyName: nis2-encryption-key
 * </pre>
 *
 * <p>Vault setup commands:</p>
 * <pre>
 * vault secrets enable transit
 * vault write -f transit/keys/nis2-encryption-key type=aes256-gcm96
 * </pre>
 */
public class VaultKmsProvider implements KmsProvider {

    private static final Logger log = LoggerFactory.getLogger(VaultKmsProvider.class);

    private final String vaultAddress;
    private final String token;
    private final String transitPath;
    private final HttpClient httpClient;

    public VaultKmsProvider(String vaultAddress, String token, String transitPath) {
        this.vaultAddress = vaultAddress.endsWith("/") ? vaultAddress.substring(0, vaultAddress.length() - 1) : vaultAddress;
        this.token = token;
        this.transitPath = transitPath != null ? transitPath : "transit";
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    @Override
    public String getName() {
        return "HashiCorp Vault";
    }

    @Override
    public String getKey(String keyId) {
        // Vault Transit doesn't return raw key material for security
        // Instead, use encrypt/decrypt operations
        log.warn("Vault Transit engine doesn't expose raw key material. Use encrypt/decrypt APIs instead.");
        return null;
    }

    @Override
    public void storeKey(String keyId, String keyMaterial) {
        // Vault manages keys internally - we create keys, not store external material
        log.info("Creating key {} in Vault Transit engine", keyId);
        
        try {
            String url = String.format("%s/v1/%s/keys/%s", vaultAddress, transitPath, keyId);
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("X-Vault-Token", token)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString("{\"type\": \"aes256-gcm96\"}"))
                    .timeout(Duration.ofSeconds(10))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200 || response.statusCode() == 204) {
                log.info("Key {} created successfully in Vault", keyId);
            } else {
                log.error("Failed to create key in Vault: {}", response.body());
            }
        } catch (Exception e) {
            log.error("Failed to store key in Vault: {}", e.getMessage());
        }
    }

    @Override
    public String rotateKey(String keyId) {
        log.info("Rotating key {} in Vault Transit engine", keyId);
        
        try {
            String url = String.format("%s/v1/%s/keys/%s/rotate", vaultAddress, transitPath, keyId);
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("X-Vault-Token", token)
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .timeout(Duration.ofSeconds(10))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200 || response.statusCode() == 204) {
                log.info("Key {} rotated successfully in Vault", keyId);
                return "rotated"; // Vault manages the actual key material
            } else {
                log.error("Failed to rotate key in Vault: {}", response.body());
                return null;
            }
        } catch (Exception e) {
            log.error("Failed to rotate key in Vault: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public boolean keyExists(String keyId) {
        try {
            String url = String.format("%s/v1/%s/keys/%s", vaultAddress, transitPath, keyId);
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("X-Vault-Token", token)
                    .GET()
                    .timeout(Duration.ofSeconds(10))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;
        } catch (Exception e) {
            log.error("Failed to check key existence in Vault: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public void deleteKey(String keyId) {
        log.warn("Key deletion in Vault requires deletion_allowed=true. Enabling and deleting key {}", keyId);
        
        try {
            // First, enable deletion
            String configUrl = String.format("%s/v1/%s/keys/%s/config", vaultAddress, transitPath, keyId);
            
            HttpRequest configRequest = HttpRequest.newBuilder()
                    .uri(URI.create(configUrl))
                    .header("X-Vault-Token", token)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString("{\"deletion_allowed\": true}"))
                    .timeout(Duration.ofSeconds(10))
                    .build();

            httpClient.send(configRequest, HttpResponse.BodyHandlers.ofString());

            // Then delete
            String deleteUrl = String.format("%s/v1/%s/keys/%s", vaultAddress, transitPath, keyId);
            
            HttpRequest deleteRequest = HttpRequest.newBuilder()
                    .uri(URI.create(deleteUrl))
                    .header("X-Vault-Token", token)
                    .DELETE()
                    .timeout(Duration.ofSeconds(10))
                    .build();

            HttpResponse<String> response = httpClient.send(deleteRequest, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 204) {
                log.info("Key {} deleted from Vault", keyId);
            } else {
                log.error("Failed to delete key from Vault: {}", response.body());
            }
        } catch (Exception e) {
            log.error("Failed to delete key from Vault: {}", e.getMessage());
        }
    }

    @Override
    public boolean isAvailable() {
        try {
            String url = String.format("%s/v1/sys/health", vaultAddress);
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .timeout(Duration.ofSeconds(5))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;
        } catch (Exception e) {
            log.debug("Vault not available: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Encrypt data using Vault Transit.
     *
     * @param keyId The key name
     * @param plaintext Base64-encoded plaintext
     * @return Vault ciphertext (vault:v1:...)
     */
    public String encrypt(String keyId, String plaintext) {
        try {
            String url = String.format("%s/v1/%s/encrypt/%s", vaultAddress, transitPath, keyId);
            String body = String.format("{\"plaintext\": \"%s\"}", plaintext);
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("X-Vault-Token", token)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .timeout(Duration.ofSeconds(10))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                // Parse ciphertext from response
                // Response format: {"data": {"ciphertext": "vault:v1:..."}}
                String responseBody = response.body();
                int start = responseBody.indexOf("\"ciphertext\":\"") + 14;
                int end = responseBody.indexOf("\"", start);
                return responseBody.substring(start, end);
            }
            return null;
        } catch (Exception e) {
            log.error("Failed to encrypt with Vault: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Decrypt data using Vault Transit.
     *
     * @param keyId The key name
     * @param ciphertext Vault ciphertext (vault:v1:...)
     * @return Base64-encoded plaintext
     */
    public String decrypt(String keyId, String ciphertext) {
        try {
            String url = String.format("%s/v1/%s/decrypt/%s", vaultAddress, transitPath, keyId);
            String body = String.format("{\"ciphertext\": \"%s\"}", ciphertext);
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("X-Vault-Token", token)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .timeout(Duration.ofSeconds(10))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                // Parse plaintext from response
                String responseBody = response.body();
                int start = responseBody.indexOf("\"plaintext\":\"") + 13;
                int end = responseBody.indexOf("\"", start);
                return responseBody.substring(start, end);
            }
            return null;
        } catch (Exception e) {
            log.error("Failed to decrypt with Vault: {}", e.getMessage());
            return null;
        }
    }
}
