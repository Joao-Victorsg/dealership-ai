package main

import (
	"context"
	"errors"
	"lambda-invoice-processor/internal/client"
	appconfig "lambda-invoice-processor/internal/config"
	"strings"
	"testing"

	"github.com/aws/aws-sdk-go-v2/aws"
	"github.com/aws/aws-sdk-go-v2/service/s3"
)

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

func TestRunStartsLambda(t *testing.T) {
	origLoadFromEnv := loadFromEnv
	origLoadDefaultAWS := loadDefaultAWS
	origNewS3API := newS3API
	origStartLambda := startLambda
	t.Cleanup(func() {
		loadFromEnv = origLoadFromEnv
		loadDefaultAWS = origLoadDefaultAWS
		newS3API = origNewS3API
		startLambda = origStartLambda
	})

	loadFromEnv = func() (appconfig.Config, error) {
		return appconfig.Config{InvoiceBucketName: "invoice-bucket"}, nil
	}
	loadDefaultAWS = func(context.Context) (aws.Config, error) { return aws.Config{}, nil }
	newS3API = func(aws.Config) client.PutObjectAPI { return s3PutAPIStub{} }

	started := false
	startLambda = func(h any) {
		if h == nil {
			t.Fatal("expected handler")
		}
		started = true
	}

	if err := run(context.Background()); err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	if !started {
		t.Fatal("expected lambda start")
	}
}

type s3PutAPIStub struct{}

func (s3PutAPIStub) PutObject(context.Context, *s3.PutObjectInput, ...func(*s3.Options)) (*s3.PutObjectOutput, error) {
	return &s3.PutObjectOutput{}, nil
}
