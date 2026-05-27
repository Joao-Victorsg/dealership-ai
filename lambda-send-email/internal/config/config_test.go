package config

import "testing"

func TestLoadFromEnvUsesProvidedSender(t *testing.T) {
	t.Setenv("EMAIL_FROM", "billing@example.com")

	cfg, err := LoadFromEnv()
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	if cfg.EmailFrom != "billing@example.com" {
		t.Fatalf("unexpected sender: %s", cfg.EmailFrom)
	}
}

func TestLoadFromEnvUsesDefaultSender(t *testing.T) {
	t.Setenv("EMAIL_FROM", "")

	cfg, err := LoadFromEnv()
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	if cfg.EmailFrom != "noreply@example.com" {
		t.Fatalf("unexpected sender: %s", cfg.EmailFrom)
	}
}
