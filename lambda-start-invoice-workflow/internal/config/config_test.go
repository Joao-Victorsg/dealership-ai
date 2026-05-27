package config

import (
	"os"
	"testing"
)

func TestLoadFromEnvSuccess(t *testing.T) {
	t.Setenv("STATE_MACHINE_ARN", "arn:aws:states:us-east-1:000000000000:stateMachine:invoice-workflow")

	cfg, err := LoadFromEnv()
	if err != nil {
		t.Fatalf("expected success, got error: %v", err)
	}
	if cfg.StateMachineARN == "" {
		t.Fatal("expected state machine arn")
	}
}

func TestLoadFromEnvMissingStateMachineArn(t *testing.T) {
	if err := os.Unsetenv("STATE_MACHINE_ARN"); err != nil {
		t.Fatalf("unset env: %v", err)
	}

	_, err := LoadFromEnv()
	if err == nil {
		t.Fatal("expected error")
	}
}
