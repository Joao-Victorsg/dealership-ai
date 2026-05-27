package main

import (
	"context"
	"errors"
	appclient "lambda-update-car-status/internal/client"
	appconfig "lambda-update-car-status/internal/config"
	"net/http"
	"strings"
	"testing"
	"time"

	"github.com/aws/aws-sdk-go-v2/aws"
)

func TestMainStartsLambdaWhenWiringSucceeds(t *testing.T) {
	origLoadFromEnv := loadFromEnv
	origLoadDefaultAWS := loadDefaultAWS
	origNewSecretsAPI := newSecretsAPI
	origLoadClientSecret := loadClientSecret
	origStartLambda := startLambda
	t.Cleanup(func() {
		loadFromEnv = origLoadFromEnv
		loadDefaultAWS = origLoadDefaultAWS
		newSecretsAPI = origNewSecretsAPI
		loadClientSecret = origLoadClientSecret
		startLambda = origStartLambda
	})

	loadFromEnv = func() (appconfig.Config, error) {
		return appconfig.Config{
			CarAPIBaseURL:              "https://cars.example.com",
			CarAPITimeout:              time.Second,
			KeycloakTokenURL:           "https://kc.example.com/token",
			KeycloakClientID:           "client",
			KeycloakSecretID:           "secret-id",
			KeycloakRefreshSkew:        time.Second,
			BreakerMaxRequests:         1,
			BreakerInterval:            time.Second,
			BreakerTimeout:             time.Second,
			BreakerReadyToTripFailures: 1,
		}, nil
	}
	loadDefaultAWS = func(context.Context) (aws.Config, error) { return aws.Config{}, nil }
	newSecretsAPI = func(aws.Config) appclient.SecretsAPI { return nil }
	loadClientSecret = func(context.Context, appclient.SecretsAPI, string) (string, error) { return "top-secret", nil }

	started := false
	startLambda = func(h any) {
		if h == nil {
			t.Fatal("expected handler")
		}
		started = true
	}

	if err := run(context.Background()); err != nil {
		t.Fatalf("expected successful run, got error: %v", err)
	}
	if !started {
		t.Fatal("expected lambda start")
	}
}

func TestRunReturnsErrorOnConfigurationFailure(t *testing.T) {
	origLoadFromEnv := loadFromEnv
	t.Cleanup(func() { loadFromEnv = origLoadFromEnv })
	loadFromEnv = func() (appconfig.Config, error) { return appconfig.Config{}, errors.New("bad env") }

	err := run(context.Background())
	if err == nil {
		t.Fatal("expected error")
	}
	if !strings.Contains(err.Error(), "loading config from env") {
		t.Fatalf("unexpected error: %v", err)
	}
}

func TestRunReturnsErrorOnSecretLoadFailure(t *testing.T) {
	origLoadFromEnv := loadFromEnv
	origLoadDefaultAWS := loadDefaultAWS
	origNewSecretsAPI := newSecretsAPI
	origLoadClientSecret := loadClientSecret
	origStartLambda := startLambda
	t.Cleanup(func() {
		loadFromEnv = origLoadFromEnv
		loadDefaultAWS = origLoadDefaultAWS
		newSecretsAPI = origNewSecretsAPI
		loadClientSecret = origLoadClientSecret
		startLambda = origStartLambda
	})

	loadFromEnv = func() (appconfig.Config, error) {
		return appconfig.Config{
			CarAPITimeout:              time.Second,
			KeycloakTokenURL:           "https://kc.example.com/token",
			KeycloakClientID:           "id",
			KeycloakSecretID:           "sid",
			CarAPIBaseURL:              "https://cars.example.com",
			BreakerMaxRequests:         1,
			BreakerInterval:            time.Second,
			BreakerTimeout:             time.Second,
			BreakerReadyToTripFailures: 1,
		}, nil
	}
	loadDefaultAWS = func(context.Context) (aws.Config, error) { return aws.Config{}, nil }
	newSecretsAPI = func(aws.Config) appclient.SecretsAPI { return nil }
	loadClientSecret = func(context.Context, appclient.SecretsAPI, string) (string, error) { return "", errors.New("boom") }
	startLambda = func(any) { t.Fatal("must not start") }

	err := run(context.Background())
	if err == nil {
		t.Fatal("expected error")
	}
	if !strings.Contains(err.Error(), "loading keycloak client secret") {
		t.Fatalf("unexpected error: %v", err)
	}
}

func TestMainPassesExpectedTimeoutToClients(t *testing.T) {
	origLoadFromEnv := loadFromEnv
	origLoadDefaultAWS := loadDefaultAWS
	origNewSecretsAPI := newSecretsAPI
	origLoadClientSecret := loadClientSecret
	origNewKeycloak := newKeycloak
	origNewCarAPIClient := newCarAPIClient
	origStartLambda := startLambda
	t.Cleanup(func() {
		loadFromEnv = origLoadFromEnv
		loadDefaultAWS = origLoadDefaultAWS
		newSecretsAPI = origNewSecretsAPI
		loadClientSecret = origLoadClientSecret
		newKeycloak = origNewKeycloak
		newCarAPIClient = origNewCarAPIClient
		startLambda = origStartLambda
	})

	cfg := appconfig.Config{
		CarAPIBaseURL:              "https://cars.example.com",
		CarAPITimeout:              1500 * time.Millisecond,
		KeycloakTokenURL:           "https://kc.example.com/token",
		KeycloakClientID:           "client",
		KeycloakSecretID:           "secret-id",
		KeycloakRefreshSkew:        time.Second,
		BreakerMaxRequests:         1,
		BreakerInterval:            time.Second,
		BreakerTimeout:             time.Second,
		BreakerReadyToTripFailures: 1,
	}
	loadFromEnv = func() (appconfig.Config, error) { return cfg, nil }
	loadDefaultAWS = func(context.Context) (aws.Config, error) { return aws.Config{}, nil }
	newSecretsAPI = func(aws.Config) appclient.SecretsAPI { return nil }
	loadClientSecret = func(context.Context, appclient.SecretsAPI, string) (string, error) { return "secret", nil }
	startLambda = func(any) {}

	gotKeycloakTimeout := time.Duration(0)
	newKeycloak = func(c appclient.KeycloakConfig) *appclient.KeycloakClient {
		gotKeycloakTimeout = c.HTTPClient.Timeout
		return appclient.NewKeycloakClient(appclient.KeycloakConfig{
			HTTPClient:   &http.Client{Timeout: time.Millisecond},
			TokenURL:     "https://example.com",
			ClientID:     "id",
			ClientSecret: "secret",
			RefreshSkew:  time.Second,
			Logger:       c.Logger,
		})
	}
	gotCarTimeout := time.Duration(0)
	newCarAPIClient = func(c appclient.CarAPIConfig) *appclient.CarAPIClient {
		gotCarTimeout = c.HTTPClient.Timeout
		return appclient.NewCarAPIClient(c)
	}

	if err := run(context.Background()); err != nil {
		t.Fatalf("expected successful run, got error: %v", err)
	}
	if gotKeycloakTimeout != cfg.CarAPITimeout || gotCarTimeout != cfg.CarAPITimeout {
		t.Fatalf("unexpected timeouts: keycloak=%v car=%v", gotKeycloakTimeout, gotCarTimeout)
	}
}
