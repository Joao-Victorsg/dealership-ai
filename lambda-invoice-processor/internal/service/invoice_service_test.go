package service

import (
	"context"
	"strings"
	"testing"
	"time"

	"github.com/shopspring/decimal"
)

func TestHandleGeneratesInvoiceAndUploadsToS3(t *testing.T) {
	uploader := &fakeUploader{}
	svc := NewInvoiceService("invoice-bucket", uploader)

	payload := SaleEventPayload{
		SaleID:       "sale-123",
		SaleValue:    decimal.RequireFromString("110.00"),
		RegisteredAt: time.Date(2026, 5, 22, 19, 35, 44, 0, time.UTC),
		ClientSnapshot: ClientSnapshot{
			FirstName: "Ana",
			LastName:  "Silva",
			Email:     "ana@example.com",
			Address: AddressSnapshot{
				Street:   "Main St",
				Number:   "100",
				City:     "Sao Paulo",
				State:    "SP",
				Postcode: "01000-000",
			},
		},
		CarSnapshot: CarSnapshot{
			Manufacturer: "BMW",
			Model:        "320i",
			VIN:          "VIN123",
			ListedValue:  decimal.RequireFromString("100.00"),
		},
	}

	out, err := svc.Handle(context.Background(), payload)
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	if out.InvoiceKey != "invoices/sale-123.html" {
		t.Fatalf("unexpected invoice key: %s", out.InvoiceKey)
	}
	if out.TaxValue != "10.00" {
		t.Fatalf("unexpected tax value: %s", out.TaxValue)
	}
	if uploader.bucket != "invoice-bucket" {
		t.Fatalf("unexpected bucket: %s", uploader.bucket)
	}
	if !strings.Contains(string(uploader.content), "Dealership Invoice") {
		t.Fatal("expected html invoice content")
	}
}

func TestHandleReturnsErrorWhenEmailMissing(t *testing.T) {
	uploader := &fakeUploader{}
	svc := NewInvoiceService("invoice-bucket", uploader)

	_, err := svc.Handle(context.Background(), SaleEventPayload{
		SaleID:      "sale-123",
		SaleValue:   decimal.RequireFromString("100.00"),
		CarSnapshot: CarSnapshot{ListedValue: decimal.RequireFromString("90.00")},
	})
	if err == nil {
		t.Fatal("expected error")
	}
}

type fakeUploader struct {
	bucket      string
	key         string
	contentType string
	content     []byte
}

func (f *fakeUploader) Upload(_ context.Context, bucket string, key string, content []byte, contentType string) error {
	f.bucket = bucket
	f.key = key
	f.contentType = contentType
	f.content = append([]byte(nil), content...)
	return nil
}
