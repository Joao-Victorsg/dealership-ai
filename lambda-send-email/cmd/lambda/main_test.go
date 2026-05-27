package main

import (
	"context"
	"errors"
	"lambda-send-email/internal/client"
	appconfig "lambda-send-email/internal/config"
	"strings"
	"testing"

	"github.com/aws/aws-sdk-go-v2/aws"
	"github.com/aws/aws-sdk-go-v2/service/s3"
	"github.com/aws/aws-sdk-go-v2/service/sesv2"
)

func TestRunReturnsErrorOnConfigFailure(t *testing.T) {
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

func TestRunStartsLambda(t *testing.T) {
	origLoadFromEnv := loadFromEnv
	origLoadDefaultAWS := loadDefaultAWS
	origNewS3API := newS3API
	origNewSESV2API := newSESV2API
	origStartLambda := startLambda
	t.Cleanup(func() {
		loadFromEnv = origLoadFromEnv
		loadDefaultAWS = origLoadDefaultAWS
		newS3API = origNewS3API
		newSESV2API = origNewSESV2API
		startLambda = origStartLambda
	})

	loadFromEnv = func() (appconfig.Config, error) {
		return appconfig.Config{EmailFrom: "noreply@example.com"}, nil
	}
	loadDefaultAWS = func(context.Context) (aws.Config, error) { return aws.Config{}, nil }
	newS3API = func(aws.Config) client.GetObjectAPI { return s3GetAPIStub{} }
	newSESV2API = func(aws.Config) client.SendEmailAPI { return sesAPIStub{} }

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

type s3GetAPIStub struct{}

func (s3GetAPIStub) GetObject(context.Context, *s3.GetObjectInput, ...func(*s3.Options)) (*s3.GetObjectOutput, error) {
	return &s3.GetObjectOutput{}, nil
}

type sesAPIStub struct{}

func (sesAPIStub) SendEmail(context.Context, *sesv2.SendEmailInput, ...func(*sesv2.Options)) (*sesv2.SendEmailOutput, error) {
	return &sesv2.SendEmailOutput{}, nil
}
