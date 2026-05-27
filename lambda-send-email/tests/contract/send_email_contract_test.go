package contract

import (
	"context"
	"encoding/json"
	"lambda-send-email/internal/service"
	"os"
	"path/filepath"
	"testing"
)

func TestSendEmailContractUsesInvoiceFromS3(t *testing.T) {
	fixture := filepath.Join("..", "fixtures", "send_email_input.json")
	raw, err := os.ReadFile(fixture)
	if err != nil {
		t.Fatalf("read fixture: %v", err)
	}

	var payload service.SendEmailInput
	if err := json.Unmarshal(raw, &payload); err != nil {
		t.Fatalf("unmarshal fixture: %v", err)
	}

	reader := &contractReader{content: []byte("<html>invoice</html>")}
	sender := &contractSender{}
	svc := service.NewEmailService("noreply@example.com", reader, sender)

	out, err := svc.Handle(context.Background(), payload)
	if err != nil {
		t.Fatalf("handle error: %v", err)
	}
	if out.Status != "sent" {
		t.Fatalf("unexpected output: %+v", out)
	}
	if sender.message.To != "ana@example.com" {
		t.Fatalf("unexpected recipient: %s", sender.message.To)
	}
}

type contractReader struct {
	content []byte
}

func (c *contractReader) Download(context.Context, string, string) ([]byte, error) {
	return c.content, nil
}

type contractSender struct {
	message service.EmailMessage
}

func (c *contractSender) Send(_ context.Context, message service.EmailMessage) (string, error) {
	c.message = message
	return "msg-123", nil
}
