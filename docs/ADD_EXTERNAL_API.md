# Integrating an External API

Use this checklist to add a typed, configurable integration without embedding environment details in code.

## Checklist

1. Decide whether a vendor SDK is justified. Prefer Spring `RestClient` when the API is ordinary HTTP and the SDK adds no domain value.
2. Add every URL, credential, timeout, and feature switch to the relevant `application*.yaml`; bind it through typed `@ConfigurationProperties`. Follow the [backend configuration conventions](../.github/instructions/backend-conventions.instructions.md) and [`AppProperties`](../backend/src/main/kotlin/org/example/gentooldataviewer/common/config/AppProperties.kt) or nearby [`config` packages](../backend/src/main/kotlin/org/example/gentooldataviewer/config/).
3. Define one configured client bean with base URL, connection/read timeouts, authentication, and stable default headers. Do not hardcode those values in Kotlin.
4. Put remote calls behind a feature integration service. Keep remote DTOs separate from application DTOs and translate at the boundary.
5. Map timeout, connectivity, authentication, rate-limit, and invalid-response failures to deliberate application exceptions. Add HTTP mappings through [`GlobalExceptionHandler`](../backend/src/main/kotlin/org/example/gentooldataviewer/web/exception/GlobalExceptionHandler.kt) only when they cross a controller boundary.
6. Add a health indicator only when the dependency is required for readiness. [`GoogleHealthIndicator`](../backend/src/main/kotlin/org/example/gentooldataviewer/health/GoogleHealthIndicator.kt) is the current minimal `RestClient` example, not a canonical full integration.
7. For local deterministic behavior, model a stub module on [`google-stubs/pom.xml`](../google-stubs/pom.xml) and its [WireMock resources](../google-stubs/src/main/resources/wiremock/google/). Register the module and a dedicated stub profile.
8. Supply real production values through the deployment environment; never commit credentials.
9. Test configuration binding, request construction, success mapping, each supported failure class, and stub-profile startup. Add health tests only when a health indicator exists.

## Configuration shape

```yaml
integration:
  example-api:
    base-url: ${EXAMPLE_API_URL}
    api-key: ${EXAMPLE_API_KEY}
    connect-timeout: ${EXAMPLE_API_CONNECT_TIMEOUT:2s}
    read-timeout: ${EXAMPLE_API_READ_TIMEOUT:10s}
```

Optional deployment environment shape:

```env
EXAMPLE_API_URL=https://api.example.com
EXAMPLE_API_KEY=replace-at-deploy-time
EXAMPLE_API_CONNECT_TIMEOUT=2s
EXAMPLE_API_READ_TIMEOUT=10s
```
