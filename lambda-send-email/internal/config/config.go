package config

import (
	"os"
	"strings"
)

type Config struct {
	EmailFrom string
}

func LoadFromEnv() (Config, error) {
	emailFrom := strings.TrimSpace(os.Getenv("EMAIL_FROM"))
	if emailFrom == "" {
		emailFrom = "noreply@example.com"
	}
	return Config{EmailFrom: emailFrom}, nil
}
