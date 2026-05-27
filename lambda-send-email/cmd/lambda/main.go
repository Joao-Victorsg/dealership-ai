package main

import (
	"context"
	"fmt"
	"lambda-send-email/internal/client"
	"lambda-send-email/internal/config"
	"lambda-send-email/internal/service"
	"log"
	"os"

	"github.com/aws/aws-lambda-go/lambda"
	"github.com/aws/aws-sdk-go-v2/aws"
	awsconfig "github.com/aws/aws-sdk-go-v2/config"
	"github.com/aws/aws-sdk-go-v2/service/s3"
	"github.com/aws/aws-sdk-go-v2/service/sesv2"
)

var (
	loadFromEnv     = config.LoadFromEnv
	loadDefaultAWS  = func(ctx context.Context) (aws.Config, error) { return awsconfig.LoadDefaultConfig(ctx) }
	newS3API        = func(cfg aws.Config) client.GetObjectAPI { return s3.NewFromConfig(cfg) }
	newSESV2API     = func(cfg aws.Config) client.SendEmailAPI { return sesv2.NewFromConfig(cfg) }
	newS3Client     = client.NewS3Client
	newSESClient    = client.NewSESClient
	newEmailService = service.NewEmailService
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

	awsCfg, err := loadDefaultAWS(ctx)
	if err != nil {
		return fmt.Errorf("loading aws default config: %w", err)
	}

	reader := newS3Client(newS3API(awsCfg))
	sender := newSESClient(newSESV2API(awsCfg))
	emailService := newEmailService(cfg.EmailFrom, reader, sender)

	startLambda(emailService.Handle)
	return nil
}
