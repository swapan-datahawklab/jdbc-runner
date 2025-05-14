package com.example.shelldemo.vault;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpRequest.BodyPublishers;
import java.nio.charset.StandardCharsets;
import java.io.IOException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.shelldemo.vault.exception.VaultException;
import java.time.LocalDateTime;

public class VaultSecretFetcher {
    private final HttpClient client;
    private final ObjectMapper mapper;

    VaultSecretFetcher(HttpClient client, ObjectMapper mapper) {
        this.client = client != null ? client : HttpClient.newHttpClient();
        this.mapper = mapper != null ? mapper : new ObjectMapper();
    }

    public VaultSecretFetcher() {
        this(null, null);
    }

    public String fetchOraclePassword(String vaultBaseUrl, String roleId, String secretId, String dbName, String ait, String username) throws VaultException {
        String vaultUrl = String.format("https://%s", vaultBaseUrl);
        System.out.println("[DEBUG] [" + LocalDateTime.now() + "] Vault base URL: " + vaultUrl);
        String clientToken = authenticateToVault(vaultUrl, roleId, secretId);
        String secretResponse = fetchOracleSecret(vaultUrl, clientToken, dbName, ait,username);
        return parsePasswordFromResponse(secretResponse);
    }

    private String authenticateToVault(String vaultBaseUrl, String roleId, String secretId) throws VaultException {
        String loginUrl = vaultBaseUrl + "/v1/auth/approle/login";
        String loginBody = String.format("{\"role_id\":\"%s\",\"secret_id\":\"%s\"}", roleId, secretId);
        System.out.println("[DEBUG] [" + LocalDateTime.now() + "] Vault login URL: " + loginUrl);
        
        HttpRequest loginRequest = HttpRequest.newBuilder()
                .uri(URI.create(loginUrl))
                .header("content-type", "application/json")
                .POST(BodyPublishers.ofString(loginBody, StandardCharsets.UTF_8))
                .build();
                
        try {
            HttpResponse<String> loginResponse = client.send(loginRequest, HttpResponse.BodyHandlers.ofString());
            System.out.println("[DEBUG] [" + LocalDateTime.now() + "] Vault login response code: " + loginResponse.statusCode());
            if (loginResponse.statusCode() != 200) {
                System.out.println("[ERROR] [" + LocalDateTime.now() + "] Vault login failed. Response body: " + loginResponse.body());
                throw new VaultException("Vault login failed: " + loginResponse.body(), vaultBaseUrl, null);
            }
            String clientToken = mapper.readTree(loginResponse.body())
                .at("/auth/client_token")
                .asText();
            if (clientToken == null || clientToken.isEmpty()) {
                System.out.println("[ERROR] [" + LocalDateTime.now() + "] No client token received from Vault. Response body: " + loginResponse.body());
                throw new VaultException("No client token received from Vault", vaultBaseUrl, null);
            }
            return clientToken;
        } catch (IOException e) {
            System.out.println("[ERROR] [" + LocalDateTime.now() + "] IOException during Vault login: " + e.getMessage());
            throw new VaultException("Failed to authenticate to Vault", e, vaultBaseUrl, null);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("[ERROR] [" + LocalDateTime.now() + "] InterruptedException during Vault login: " + e.getMessage());
            throw new VaultException("Interrupted while authenticating to Vault", e, vaultBaseUrl, null);
        }
    }

    private String fetchOracleSecret(String vaultBaseUrl, String clientToken, String dbName, String ait, String username) throws VaultException {
        String secretPath = String.format("%s/v1/secrets/database/oracle/static-creds/%s-%s-%s", vaultBaseUrl, ait, dbName,username);
        System.out.println("[DEBUG] [" + LocalDateTime.now() + "] Vault secret fetch URL: " + secretPath);
        HttpRequest secretRequest = HttpRequest.newBuilder()
                .uri(URI.create(secretPath))
                .header("x-vault-token", clientToken)
                .GET()
                .build();
        try {
            HttpResponse<String> secretResponse = client.send(secretRequest, HttpResponse.BodyHandlers.ofString());
            System.out.println("[DEBUG] [" + LocalDateTime.now() + "] Vault secret fetch response code: " + secretResponse.statusCode());
            if (secretResponse.statusCode() != 200) {
                System.out.println("[ERROR] [" + LocalDateTime.now() + "] Vault secret fetch failed. Response body: " + secretResponse.body());
                throw new VaultException("Vault secret fetch failed: " + secretResponse.body(), vaultBaseUrl, secretPath);
            }
            return secretResponse.body();
        } catch (IOException e) {
            System.out.println("[ERROR] [" + LocalDateTime.now() + "] IOException during Vault secret fetch: " + e.getMessage());
            throw new VaultException("Failed to fetch Vault secret", e, vaultBaseUrl, secretPath);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("[ERROR] [" + LocalDateTime.now() + "] InterruptedException during Vault secret fetch: " + e.getMessage());
            throw new VaultException("Interrupted while fetching Vault secret", e, vaultBaseUrl, secretPath);
        }
    }

    private String parsePasswordFromResponse(String secretResponseBody) throws VaultException {
        try {
            String password = mapper.readTree(secretResponseBody).at("/data/password").asText();
            if (password == null || password.isEmpty()) {
                throw new VaultException("No password found in Vault secret", null, null);
            }
            return password;
        } catch (IOException e) {
            throw new VaultException("Failed to parse Vault response", e, null, null);
        }
    }
} 