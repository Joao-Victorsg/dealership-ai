package config

import (
	"fmt"
	"os"
	"strings"
)

type Config struct {
	InvoiceBucketName string
}

func LoadFromEnv() (Config, error) {
	invoiceBucketName := strings.TrimSpace(os.Getenv("INVOICE_BUCKET_NAME"))
	if invoiceBucketName == "" {
		return Config{}, fmt.Errorf("missing required env: INVOICE_BUCKET_NAME")
	}
	return Config{
		InvoiceBucketName: invoiceBucketName,
	}, nil
}
