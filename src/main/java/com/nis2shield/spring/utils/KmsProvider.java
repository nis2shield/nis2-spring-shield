package com.nis2shield.spring.utils;

/**
 * Interface for Key Management Service (KMS) providers.
 * Implementations handle key storage and retrieval from external KMS systems.
 *
 * <p>Supported providers:</p>
 * <ul>
 *   <li>AWS KMS</li>
 *   <li>HashiCorp Vault</li>
 *   <li>Azure Key Vault</li>
 *   <li>Google Cloud KMS</li>
 * </ul>
 */
public interface KmsProvider {

    /**
     * Get the provider name.
     */
    String getName();

    /**
     * Retrieve a key by its identifier/alias.
     *
     * @param keyId The key identifier or alias
     * @return Base64-encoded key material
     */
    String getKey(String keyId);

    /**
     * Store a key with the given identifier.
     *
     * @param keyId The key identifier or alias
     * @param keyMaterial Base64-encoded key material
     */
    void storeKey(String keyId, String keyMaterial);

    /**
     * Rotate a key, creating a new version.
     *
     * @param keyId The key identifier
     * @return Base64-encoded new key material
     */
    String rotateKey(String keyId);

    /**
     * Check if a key exists.
     *
     * @param keyId The key identifier
     * @return true if key exists
     */
    boolean keyExists(String keyId);

    /**
     * Delete a key.
     *
     * @param keyId The key identifier
     */
    void deleteKey(String keyId);

    /**
     * Check if the provider is available and configured.
     */
    boolean isAvailable();
}
