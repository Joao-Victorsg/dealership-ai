package main

import (
	"context"
	"errors"
	"lambda-start-invoice-workflow/internal/client"
	appconfig "lambda-start-invoice-workflow/internal/config"
	"strings"
	"testing"

	"github.com/aws/aws-sdk-go-v2/aws"
	"github.com/aws/aws-sdk-go-v2/service/sfn"
)

func TestRunStartsLambdaWhenWiringSucceeds(t *testing.T) {
	origLoadFromEnv := loadFromEnv
	origLoadDefaultAWS := loadDefaultAWS
	origNewStepFunctionsAPI := newStepFunctionsAPI
	origStartLambda := startLambda
	t.Cleanup(func() {
		loadFromEnv = origLoadFromEnv
		loadDefaultAWS = origLoadDefaultAWS
		newStepFunctionsAPI = origNewStepFunctionsAPI
		startLambda = origStartLambda
	})

	loadFromEnv = func() (appconfig.Config, error) {
		return appconfig.Config{StateMachineARN: "arn:aws:states:us-east-1:000000000000:stateMachine:invoice-workflow"}, nil
	}
	loadDefaultAWS = func(context.Context) (aws.Config, error) { return aws.Config{}, nil }
	newStepFunctionsAPI = func(aws.Config) client.StartExecutionAPI { return fakeStartExecutionAPI{} }

	started := false
	startLambda = func(h any) {
		if h == nil {
			t.Fatal("expected handler")
		}
		started = true
	}

	if err := run(context.Background()); err != nil {
		t.Fatalf("expected successful run, got %v", err)
	}
	if !started {
		t.Fatal("expected lambda start")
	}
}

func TestRunReturnsErrorOnConfigFailure(t *testing.T) {
	origLoadFromEnv := loadFromEnv
	t.Cleanup(func() { loadFromEnv = origLoadFromEnv })

	loadFromEnv = func() (appconfig.Config, error) { return appconfig.Config{}, errors.New("missing env") }

	err := run(context.Background())
	if err == nil {
		t.Fatal("expected error")
	}
	if !strings.Contains(err.Error(), "loading config from env") {
		t.Fatalf("unexpected error: %v", err)
	}
}

func TestRunReturnsErrorOnAWSConfigFailure(t *testing.T) {
	origLoadFromEnv := loadFromEnv
	origLoadDefaultAWS := loadDefaultAWS
	t.Cleanup(func() {
		loadFromEnv = origLoadFromEnv
		loadDefaultAWS = origLoadDefaultAWS
	})

	loadFromEnv = func() (appconfig.Config, error) {
		return appconfig.Config{StateMachineARN: "arn:aws:states:us-east-1:000000000000:stateMachine:invoice-workflow"}, nil
	}
	loadDefaultAWS = func(context.Context) (aws.Config, error) { return aws.Config{}, errors.New("aws unavailable") }

	err := run(context.Background())
	if err == nil {
		t.Fatal("expected error")
	}
	if !strings.Contains(err.Error(), "loading aws default config") {
		t.Fatalf("unexpected error: %v", err)
	}
}

type fakeStartExecutionAPI struct{}

func (fakeStartExecutionAPI) StartExecution(context.Context, *sfn.StartExecutionInput, ...func(*sfn.Options)) (*sfn.StartExecutionOutput, error) {
	return &sfn.StartExecutionOutput{}, nil
}
