package main

import (
	"context"
	"fmt"
	"lambda-invoice-processor/internal/client"
	"lambda-invoice-processor/internal/config"
	"lambda-invoice-processor/internal/service"
	"log"
	"os"

	"github.com/aws/aws-lambda-go/lambda"
	"github.com/aws/aws-sdk-go-v2/aws"
	awsconfig "github.com/aws/aws-sdk-go-v2/config"
	"github.com/aws/aws-sdk-go-v2/service/s3"
)

var (
	loadFromEnv       = config.LoadFromEnv
	loadDefaultAWS    = func(ctx context.Context) (aws.Config, error) { return awsconfig.LoadDefaultConfig(ctx) }
	newS3API          = func(cfg aws.Config) client.PutObjectAPI { return s3.NewFromConfig(cfg) }
	newS3Client       = client.NewS3Client
	newInvoiceService = service.NewInvoiceService
	startLambda       = func(h any) { lambda.Start(h) }
	exitProcess       = os.Exit
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

	uploader := newS3Client(newS3API(awsCfg))
	invoiceService := newInvoiceService(cfg.InvoiceBucketName, uploader)

	startLambda(invoiceService.Handle)
	return nil
}
