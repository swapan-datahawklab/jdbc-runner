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
        String clientToken = authenticateToVault(vaultUrl, roleId, secretId);
        String secretResponse = fetchOracleSecret(vaultUrl, clientToken, dbName, ait,username);
        return parsePasswordFromResponse(secretResponse);
    }

    private String authenticateToVault(String vaultBaseUrl, String roleId, String secretId) throws VaultException {;
        String loginUrl = vaultBaseUrl + "/v1/auth/approle/login";
        String loginBody = String.format("{\"role_id\":\"%s\",\"secret_id\":\"%s\"}", roleId, secretId);
        
        HttpRequest loginRequest = HttpRequest.newBuilder()
                .uri(URI.create(loginUrl))
                .header("content-type", "application/json")
                .POST(BodyPublishers.ofString(loginBody, StandardCharsets.UTF_8))
                .build();
                
        try {
            HttpResponse<String> loginResponse = client.send(loginRequest, HttpResponse.BodyHandlers.ofString());
            
            if (loginResponse.statusCode() != 200) {
                throw new VaultException("Vault login failed: " + loginResponse.body(), vaultBaseUrl, null);
            }
            
            String clientToken = mapper.readTree(loginResponse.body())
                .at("/auth/client_token")
                .asText();
                
            if (clientToken == null || clientToken.isEmpty()) {
                throw new VaultException("No client token received from Vault", vaultBaseUrl, null);
            }
            
            return clientToken;
            
        } catch (IOException e) {
            throw new VaultException("Failed to authenticate to Vault", e, vaultBaseUrl, null);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new VaultException("Interrupted while authenticating to Vault", e, vaultBaseUrl, null);
        }
    }

    private String fetchOracleSecret(String vaultBaseUrl, String clientToken, String dbName, String ait, String username) throws VaultException {
        String secretPath = String.format("%s/v1/secrets/database/oracle/static-creds/%s-%s-%s", vaultBaseUrl, ait, dbName,username);
        HttpRequest secretRequest = HttpRequest.newBuilder()
                .uri(URI.create(secretPath))
                .header("x-vault-token", clientToken)
                .GET()
                .build();
        try {
            HttpResponse<String> secretResponse = client.send(secretRequest, HttpResponse.BodyHandlers.ofString());
            if (secretResponse.statusCode() != 200) {
                throw new VaultException("Vault secret fetch failed: " + secretResponse.body(), vaultBaseUrl, secretPath);
            }
            return secretResponse.body();
        } catch (IOException e) {
            throw new VaultException("Failed to fetch Vault secret", e, vaultBaseUrl, secretPath);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
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