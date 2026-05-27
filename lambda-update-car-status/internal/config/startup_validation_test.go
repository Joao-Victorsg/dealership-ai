package config

import "testing"

func TestLoadFromEnvInvalidEndpointScheme(t *testing.T) {
	withValidEnv(t)
	t.Setenv("KEYCLOAK_TOKEN_URL", "ftp://bad")
	_, err := LoadFromEnv()
	if err == nil {
		t.Fatal("expected invalid url")
	}
}
