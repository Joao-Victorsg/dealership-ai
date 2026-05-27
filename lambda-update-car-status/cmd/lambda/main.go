package main

import (
	"context"
	"fmt"
	appclient "lambda-update-car-status/internal/client"
	appconfig "lambda-update-car-status/internal/config"
	"lambda-update-car-status/internal/handler"
	"lambda-update-car-status/internal/observability"
	"lambda-update-car-status/internal/service"
	"log"
	"net/http"
	"os"
	"time"

	"github.com/aws/aws-lambda-go/lambda"
	"github.com/aws/aws-sdk-go-v2/aws"
	"github.com/aws/aws-sdk-go-v2/config"
	"github.com/aws/aws-sdk-go-v2/service/secretsmanager"
	"github.com/sony/gobreaker/v2"
)

var (
	loadFromEnv      = appconfig.LoadFromEnv
	loadDefaultAWS   = func(ctx context.Context) (aws.Config, error) { return config.LoadDefaultConfig(ctx) }
	newSecretsAPI    = func(cfg aws.Config) appclient.SecretsAPI { return secretsmanager.NewFromConfig(cfg) }
	loadClientSecret = appclient.LoadClientSecret
	newKeycloak      = appclient.NewKeycloakClient
	newBreaker       = func(settings gobreaker.Settings) *gobreaker.CircuitBreaker[any] {
		return gobreaker.NewCircuitBreaker[any](settings)
	}
	newCarAPIClient = appclient.NewCarAPIClient
	newService      = service.NewCarStatusService
	newSQSHandler   = handler.NewSQSHandler
	startLambda     = func(h any) { lambda.Start(h) }
	exitProcess     = os.Exit
)

func main() {
	if err := run(context.Background()); err != nil {
		log.Printf("startup failed: %v", err)
		exitProcess(1)
	}
}

func run(ctx context.Context) error {
	cfg, err := loadFromEnv()
	if err != nil {
		return fmt.Errorf("loading config from env: %w", err)
	}
	logger := observability.NewJSONLogger(os.Stdout)
	awsCfg, err := loadDefaultAWS(ctx)
	if err != nil {
		return fmt.Errorf("loading aws default config: %w", err)
	}
	secretsAPI := newSecretsAPI(awsCfg)
	clientSecret, err := loadClientSecret(ctx, secretsAPI, cfg.KeycloakSecretID)
	if err != nil {
		return fmt.Errorf("loading keycloak client secret: %w", err)
	}
	tokenProvider := newKeycloak(appclient.KeycloakConfig{
		HTTPClient:   &http.Client{Timeout: cfg.CarAPITimeout},
		TokenURL:     cfg.KeycloakTokenURL,
		ClientID:     cfg.KeycloakClientID,
		ClientSecret: clientSecret,
		RefreshSkew:  cfg.KeycloakRefreshSkew,
		Logger:       logger,
	})
	breaker := newBreaker(gobreaker.Settings{
		Name:        "car-api",
		Interval:    cfg.BreakerInterval,
		Timeout:     cfg.BreakerTimeout,
		MaxRequests: cfg.BreakerMaxRequests,
		ReadyToTrip: func(counts gobreaker.Counts) bool {
			return counts.ConsecutiveFailures >= cfg.BreakerReadyToTripFailures
		},
	})
	carClient := newCarAPIClient(appclient.CarAPIConfig{
		BaseURL:       cfg.CarAPIBaseURL,
		HTTPClient:    &http.Client{Timeout: cfg.CarAPITimeout},
		TokenProvider: tokenProvider,
		Breaker:       breaker,
		Logger:        logger,
	})
	svc := newService(carClient, logger)
	h := newSQSHandler(svc, logger, time.Now)
	startLambda(h.Handle)
	return nil
}
