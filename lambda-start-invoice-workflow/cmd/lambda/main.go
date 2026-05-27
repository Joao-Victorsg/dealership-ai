package main

import (
	"context"
	"fmt"
	"lambda-start-invoice-workflow/internal/client"
	"lambda-start-invoice-workflow/internal/config"
	"lambda-start-invoice-workflow/internal/handler"
	"log"
	"os"

	"github.com/aws/aws-lambda-go/lambda"
	"github.com/aws/aws-sdk-go-v2/aws"
	awsconfig "github.com/aws/aws-sdk-go-v2/config"
	"github.com/aws/aws-sdk-go-v2/service/sfn"
)

var (
	loadFromEnv         = config.LoadFromEnv
	loadDefaultAWS      = func(ctx context.Context) (aws.Config, error) { return awsconfig.LoadDefaultConfig(ctx) }
	newStepFunctionsAPI = func(cfg aws.Config) client.StartExecutionAPI { return sfn.NewFromConfig(cfg) }
	newWorkflowClient   = client.NewWorkflowClient
	newLogger           = func() *log.Logger { return log.New(os.Stdout, "", log.LstdFlags) }
	startLambda         = func(h any) { lambda.Start(h) }
	exitProcess         = os.Exit
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

	stepFunctionsAPI := newStepFunctionsAPI(awsCfg)
	workflowClient := newWorkflowClient(stepFunctionsAPI, cfg.StateMachineARN)
	sqsHandler := handler.NewSQSHandler(workflowClient, newLogger())

	startLambda(sqsHandler.Handle)
	return nil
}
