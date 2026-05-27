package config

import (
	"fmt"
	"os"
	"strings"
)

type Config struct {
	StateMachineARN string
}

func LoadFromEnv() (Config, error) {
	stateMachineARN := strings.TrimSpace(os.Getenv("STATE_MACHINE_ARN"))
	if stateMachineARN == "" {
		return Config{}, fmt.Errorf("missing required env: STATE_MACHINE_ARN")
	}
	return Config{
		StateMachineARN: stateMachineARN,
	}, nil
}
