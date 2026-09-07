# Adding or Changing OAuth2 Providers

Provider work is cross-cutting. Treat registration, identity, persistence, local stubs, UI, and tests as one change. The current implementation uses Discord OAuth2.

## Impact checklist

1. Add matching production and stub profiles. Mirror [`application-prod-discord.yaml`](../backend/src/main/resources/application-prod-discord.yaml) and [`application-stub-discord.yaml`](../backend/src/main/resources/application-stub-discord.yaml); keep client credentials and provider endpoints configurable.
2. Define the provider attribute mapping for subject, display name, and picture. [`AuthenticatedUserPrincipal`](../backend/src/main/kotlin/org/taonity/gentooldataviewer/security/principal/AuthenticatedUserPrincipal.kt) is provider-neutral.
3. Update the provider mapping in [`OAuth2UserPersistenceService`](../backend/src/main/kotlin/org/taonity/gentooldataviewer/security/service/OAuth2UserPersistenceService.kt). Add an OIDC service only when the provider actually supports OIDC.
4. Keep persisted IDs provider-namespaced and review [`UserEntity`](../backend/src/main/kotlin/org/taonity/gentooldataviewer/user/entity/UserEntity.kt), uniqueness rules, account-linking behavior, and Flyway migrations. Follow [Database](DATABASE.md).
5. Add the provider authorization URL to the public home login action. Keep OAuth initiation on the backend at `/oauth2/authorization/{registrationId}`.
6. Add or replace a stub module and provider resources. Use the existing [Discord WireMock resources](../discord-stubs/src/main/resources/wiremock/discord/) as the local profile pattern.
7. Test attribute mapping, repeated login, identity collisions, authorization redirects, callback behavior, and the frontend login action. Follow [Testing](TESTING.md).
8. Update the profile matrix and deployment environment documentation when profile names or required values change.

## Registration shape

Keep production and stub values in separate profile files.

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          provider-id:
            client-id: ${PROVIDER_CLIENT_ID}
            client-secret: ${PROVIDER_CLIENT_SECRET}
            scope: openid,profile
        provider:
          provider-id:
            issuer-uri: ${PROVIDER_ISSUER_URI}

```

Standard authorization and callback endpoints are handled by Spring Security; add custom routing only when the provider requires it.
