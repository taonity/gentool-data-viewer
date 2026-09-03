# Adding or Changing OAuth2 Providers

Provider work is cross-cutting. Treat registration, identity, persistence, local stubs, UI, and tests as one change.

## Impact checklist

1. Add matching production and stub profiles. Mirror [`application-prod-google.yaml`](../backend/src/main/resources/application-prod-google.yaml) and [`application-stub-google.yaml`](../backend/src/main/resources/application-stub-google.yaml); keep client credentials and provider endpoints configurable.
2. Define the provider attribute mapping for subject, email, display name, and picture. Decide how missing or unverified email is handled. [`GoogleUserPrincipal`](../backend/src/main/kotlin/org/example/gentooldataviewer/security/principal/GoogleUserPrincipal.kt) shows the current Google mapping.
3. Update both persistence paths where the provider supports them: [`OAuth2UserPersistenceService`](../backend/src/main/kotlin/org/example/gentooldataviewer/security/service/OAuth2UserPersistenceService.kt) and [`OidcUserPersistenceService`](../backend/src/main/kotlin/org/example/gentooldataviewer/security/service/OidcUserPersistenceService.kt).
4. Make identity provider-agnostic before storing multiple providers. Review [`UserEntity`](../backend/src/main/kotlin/org/example/gentooldataviewer/user/entity/UserEntity.kt), uniqueness rules, account-linking behavior, and whether a Flyway migration is required. Follow [Database](DATABASE.md).
5. Add the provider login URL and button to the [login page](../frontend/src/app/%28public%29/login/page.tsx). Keep OAuth initiation on the backend at `/oauth2/authorization/{registrationId}`.
6. Add or replace a stub module and provider resources. Use the existing [Google WireMock resources](../google-stubs/src/main/resources/wiremock/google/) as the local profile pattern.
7. Test attribute mapping, OAuth2 and OIDC persistence, repeated login, identity collisions, authorization redirects, callback behavior, and the frontend login action. Follow [Testing](TESTING.md).
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
            scope: openid,email,profile
        provider:
          provider-id:
            issuer-uri: ${PROVIDER_ISSUER_URI}
```

Standard authorization and callback endpoints are handled by Spring Security; add custom routing only when the provider requires it.
