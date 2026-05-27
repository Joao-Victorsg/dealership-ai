package contract

import (
	"context"
	"encoding/json"
	"lambda-invoice-processor/internal/service"
	"os"
	"path/filepath"
	"testing"
)

func TestInvoiceProcessorContractOutputShape(t *testing.T) {
	fixture := filepath.Join("..", "fixtures", "sale_event_payload.json")
	raw, err := os.ReadFile(fixture)
	if err != nil {
		t.Fatalf("read fixture: %v", err)
	}

	var payload service.SaleEventPayload
	if err := json.Unmarshal(raw, &payload); err != nil {
		t.Fatalf("unmarshal fixture: %v", err)
	}

	uploader := &contractUploader{}
	svc := service.NewInvoiceService("invoice-bucket", uploader)

	out, err := svc.Handle(context.Background(), payload)
	if err != nil {
		t.Fatalf("handle error: %v", err)
	}

	if out.SaleID == "" || out.InvoiceBucket == "" || out.InvoiceKey == "" || out.RecipientEmail == "" {
		t.Fatalf("missing required fields in output: %+v", out)
	}
}

type contractUploader struct{}

func (contractUploader) Upload(context.Context, string, string, []byte, string) error {
	return nil
}
