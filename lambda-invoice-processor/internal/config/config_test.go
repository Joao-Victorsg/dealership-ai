package config

import "testing"

func TestLoadFromEnvSuccess(t *testing.T) {
	t.Setenv("INVOICE_BUCKET_NAME", "invoice-bucket")

	cfg, err := LoadFromEnv()
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	if cfg.InvoiceBucketName != "invoice-bucket" {
		t.Fatalf("unexpected bucket name: %s", cfg.InvoiceBucketName)
	}
}

func TestLoadFromEnvMissingBucket(t *testing.T) {
	t.Setenv("INVOICE_BUCKET_NAME", "")

	_, err := LoadFromEnv()
	if err == nil {
		t.Fatal("expected error")
	}
}
