package client

import (
	"context"
	"encoding/json"
	"errors"
	"fmt"

	"github.com/aws/aws-sdk-go-v2/aws"
	"github.com/aws/aws-sdk-go-v2/service/secretsmanager"
)

type secretPayload struct {
	ClientSecret string `json:"client_secret"`
}
type SecretsAPI interface {
	GetSecretValue(ctx context.Context, params *secretsmanager.GetSecretValueInput, optFns ...func(*secretsmanager.Options)) (*secretsmanager.GetSecretValueOutput, error)
}

func LoadClientSecret(ctx context.Context, api SecretsAPI, secretID string) (string, error) {
	out, err := api.GetSecretValue(ctx, &secretsmanager.GetSecretValueInput{SecretId: aws.String(secretID)})
	if err != nil {
		return "", fmt.Errorf("getting secret value for %q: %w", secretID, err)
	}
	if out.SecretString == nil {
		return "", errors.New("secret string is empty")
	}
	var p secretPayload
	if err := json.Unmarshal([]byte(*out.SecretString), &p); err != nil {
		return "", fmt.Errorf("invalid secret payload: %w", err)
	}
	if p.ClientSecret == "" {
		return "", errors.New("client_secret is required")
	}
	return p.ClientSecret, nil
}
