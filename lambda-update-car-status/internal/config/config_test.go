package config

import "testing"

func withValidEnv(t *testing.T) {
	t.Setenv("CAR_API_BASE_URL", "https://car.example.com")
	t.Setenv("KEYCLOAK_TOKEN_URL", "https://kc.example.com/token")
	t.Setenv("KEYCLOAK_CLIENT_ID", "client")
	t.Setenv("KEYCLOAK_SECRET_ID", "secret-id")
	t.Setenv("CAR_API_TIMEOUT_MS", "500")
	t.Setenv("KEYCLOAK_REFRESH_SKEW_SECONDS", "30")
	t.Setenv("BREAKER_MAX_REQUESTS", "1")
	t.Setenv("BREAKER_INTERVAL_MS", "1000")
	t.Setenv("BREAKER_TIMEOUT_MS", "2000")
	t.Setenv("BREAKER_READY_TO_TRIP_FAILURES", "2")
	t.Setenv("LOG_LEVEL", "info")
}
func TestLoadFromEnvSuccess(t *testing.T) {
	withValidEnv(t)
	cfg, err := LoadFromEnv()
	if err != nil {
		t.Fatalf("unexpected err: %v", err)
	}
	if cfg.KeycloakSecretID != "secret-id" {
		t.Fatal("expected secret id")
	}
}
func TestLoadFromEnvMissingRequired(t *testing.T) {
	withValidEnv(t)
	t.Setenv("KEYCLOAK_SECRET_ID", "")
	_, err := LoadFromEnv()
	if err == nil {
		t.Fatal("expected error")
	}
}
func TestLoadFromEnvInvalidNumbers(t *testing.T) {
	withValidEnv(t)
	t.Setenv("CAR_API_TIMEOUT_MS", "x")
	_, err := LoadFromEnv()
	if err == nil {
		t.Fatal("expected error")
	}
}
func TestLoadFromEnvInvalidURLs(t *testing.T) {
	withValidEnv(t)
	t.Setenv("CAR_API_BASE_URL", "not-url")
	_, err := LoadFromEnv()
	if err == nil {
		t.Fatal("expected error")
	}
}

func TestLoadFromEnvRejectsZeroAndNegativeNumericValues(t *testing.T) {
	tests := []struct {
		key   string
		value string
	}{
		{key: "KEYCLOAK_REFRESH_SKEW_SECONDS", value: "0"},
		{key: "BREAKER_MAX_REQUESTS", value: "0"},
		{key: "BREAKER_READY_TO_TRIP_FAILURES", value: "0"},
		{key: "BREAKER_TIMEOUT_MS", value: "-1"},
	}

	for _, tt := range tests {
		t.Run(tt.key, func(t *testing.T) {
			withValidEnv(t)
			t.Setenv(tt.key, tt.value)
			if _, err := LoadFromEnv(); err == nil {
				t.Fatalf("expected error for %s=%s", tt.key, tt.value)
			}
		})
	}
}

func TestLoadFromEnvRejectsUnsupportedURLSchemeForCarAPI(t *testing.T) {
	withValidEnv(t)
	t.Setenv("CAR_API_BASE_URL", "ftp://cars.example.com")
	if _, err := LoadFromEnv(); err == nil {
		t.Fatal("expected invalid URL scheme error")
	}
}
