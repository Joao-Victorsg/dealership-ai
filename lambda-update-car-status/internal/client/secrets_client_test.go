package client

import (
	"context"
	"errors"
	"testing"

	"github.com/aws/aws-sdk-go-v2/service/secretsmanager"
)

type fakeSecrets struct {
	value    string
	err      error
	nilValue bool
}

func (f fakeSecrets) GetSecretValue(_ context.Context, _ *secretsmanager.GetSecretValueInput, _ ...func(*secretsmanager.Options)) (*secretsmanager.GetSecretValueOutput, error) {
	if f.err != nil {
		return nil, f.err
	}
	if f.nilValue {
		return &secretsmanager.GetSecretValueOutput{SecretString: nil}, nil
	}
	return &secretsmanager.GetSecretValueOutput{SecretString: &f.value}, nil
}
func TestLoadClientSecretValidation(t *testing.T) {
	_, err := LoadClientSecret(context.Background(), fakeSecrets{value: `{"nope":"x"}`}, "id")
	if err == nil {
		t.Fatal("expected error")
	}
}
func TestLoadClientSecretError(t *testing.T) {
	_, err := LoadClientSecret(context.Background(), fakeSecrets{err: errors.New("boom")}, "id")
	if err == nil {
		t.Fatal("expected error")
	}
}

func TestLoadClientSecretSuccess(t *testing.T) {
	secret, err := LoadClientSecret(context.Background(), fakeSecrets{value: `{"client_secret":"s3cr3t"}`}, "id")
	if err != nil {
		t.Fatal(err)
	}
	if secret != "s3cr3t" {
		t.Fatalf("unexpected secret: %s", secret)
	}
}

func TestLoadClientSecretNilAndInvalidJSON(t *testing.T) {
	if _, err := LoadClientSecret(context.Background(), fakeSecrets{nilValue: true}, "id"); err == nil {
		t.Fatal("expected nil secret string error")
	}
	if _, err := LoadClientSecret(context.Background(), fakeSecrets{value: "{"}, "id"); err == nil {
		t.Fatal("expected invalid payload error")
	}
}
