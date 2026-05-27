package config

import (
	"fmt"
	"net/url"
	"os"
	"strconv"
	"time"
)

type Config struct {
	CarAPIBaseURL              string
	CarAPITimeout              time.Duration
	KeycloakTokenURL           string
	KeycloakClientID           string
	KeycloakSecretID           string
	KeycloakRefreshSkew        time.Duration
	BreakerMaxRequests         uint32
	BreakerInterval            time.Duration
	BreakerTimeout             time.Duration
	BreakerReadyToTripFailures uint32
	LogLevel                   string
}

func LoadFromEnv() (Config, error) {
	cfg := Config{}
	var err error
	if cfg.CarAPIBaseURL, err = requireURL("CAR_API_BASE_URL"); err != nil {
		return Config{}, err
	}
	if cfg.KeycloakTokenURL, err = requireURL("KEYCLOAK_TOKEN_URL"); err != nil {
		return Config{}, err
	}
	if cfg.KeycloakClientID, err = require("KEYCLOAK_CLIENT_ID"); err != nil {
		return Config{}, err
	}
	if cfg.KeycloakSecretID, err = require("KEYCLOAK_SECRET_ID"); err != nil {
		return Config{}, err
	}
	if cfg.CarAPITimeout, err = requireMS("CAR_API_TIMEOUT_MS"); err != nil {
		return Config{}, err
	}
	if cfg.KeycloakRefreshSkew, err = requireSeconds("KEYCLOAK_REFRESH_SKEW_SECONDS"); err != nil {
		return Config{}, err
	}
	if cfg.BreakerMaxRequests, err = requireUint("BREAKER_MAX_REQUESTS"); err != nil {
		return Config{}, err
	}
	if cfg.BreakerInterval, err = requireMS("BREAKER_INTERVAL_MS"); err != nil {
		return Config{}, err
	}
	if cfg.BreakerTimeout, err = requireMS("BREAKER_TIMEOUT_MS"); err != nil {
		return Config{}, err
	}
	if cfg.BreakerReadyToTripFailures, err = requireUint("BREAKER_READY_TO_TRIP_FAILURES"); err != nil {
		return Config{}, err
	}
	if cfg.LogLevel, err = require("LOG_LEVEL"); err != nil {
		return Config{}, err
	}
	return cfg, nil
}
func require(key string) (string, error) {
	v := os.Getenv(key)
	if v == "" {
		return "", fmt.Errorf("missing required env: %s", key)
	}
	return v, nil
}
func requireURL(key string) (string, error) {
	v, err := require(key)
	if err != nil {
		return "", err
	}
	u, err := url.ParseRequestURI(v)
	if err != nil || u.Scheme == "" || u.Host == "" {
		return "", fmt.Errorf("invalid URL env %s", key)
	}
	if u.Scheme != "http" && u.Scheme != "https" {
		return "", fmt.Errorf("invalid URL env %s", key)
	}
	return v, nil
}
func requireMS(key string) (time.Duration, error) {
	i, err := requireInt(key)
	if err != nil {
		return 0, err
	}
	return time.Duration(i) * time.Millisecond, nil
}
func requireSeconds(key string) (time.Duration, error) {
	i, err := requireInt(key)
	if err != nil {
		return 0, err
	}
	return time.Duration(i) * time.Second, nil
}
func requireInt(key string) (int64, error) {
	v, err := require(key)
	if err != nil {
		return 0, err
	}
	i, convErr := strconv.ParseInt(v, 10, 64)
	if convErr != nil || i <= 0 {
		return 0, fmt.Errorf("invalid numeric env %s", key)
	}
	return i, nil
}
func requireUint(key string) (uint32, error) {
	v, err := require(key)
	if err != nil {
		return 0, err
	}
	i, convErr := strconv.ParseUint(v, 10, 32)
	if convErr != nil || i == 0 {
		return 0, fmt.Errorf("invalid numeric env %s", key)
	}
	return uint32(i), nil
}
