package service

import (
	"context"
	"errors"
	"testing"
)

func TestHandleDownloadsInvoiceAndSendsEmail(t *testing.T) {
	reader := &fakeReader{
		content: []byte("<html>invoice</html>"),
	}
	sender := &fakeSender{
		messageID: "msg-123",
	}
	svc := NewEmailService("noreply@example.com", reader, sender)

	out, err := svc.Handle(context.Background(), SendEmailInput{
		SaleID:         "sale-123",
		InvoiceBucket:  "invoice-bucket",
		InvoiceKey:     "invoices/sale-123.html",
		RecipientEmail: "ana@example.com",
		Subject:        "Your invoice",
	})
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	if out.MessageID != "msg-123" || out.Status != "sent" {
		t.Fatalf("unexpected output: %+v", out)
	}
	if sender.message.To != "ana@example.com" {
		t.Fatalf("unexpected recipient: %s", sender.message.To)
	}
	if sender.message.HTMLBody != "<html>invoice</html>" {
		t.Fatal("expected html body from s3 content")
	}
}

func TestHandleFailsWhenMissingInvoiceLocation(t *testing.T) {
	svc := NewEmailService("noreply@example.com", &fakeReader{}, &fakeSender{})

	_, err := svc.Handle(context.Background(), SendEmailInput{
		RecipientEmail: "ana@example.com",
	})
	if err == nil {
		t.Fatal("expected error")
	}
}

func TestHandlePropagatesDownloadError(t *testing.T) {
	svc := NewEmailService("noreply@example.com", &fakeReader{err: errors.New("s3 down")}, &fakeSender{})

	_, err := svc.Handle(context.Background(), SendEmailInput{
		SaleID:         "sale-123",
		InvoiceBucket:  "invoice-bucket",
		InvoiceKey:     "invoices/sale-123.html",
		RecipientEmail: "ana@example.com",
	})
	if err == nil {
		t.Fatal("expected error")
	}
}

type fakeReader struct {
	content []byte
	err     error
}

func (f *fakeReader) Download(context.Context, string, string) ([]byte, error) {
	if f.err != nil {
		return nil, f.err
	}
	return f.content, nil
}

type fakeSender struct {
	message   EmailMessage
	messageID string
	err       error
}

func (f *fakeSender) Send(_ context.Context, message EmailMessage) (string, error) {
	if f.err != nil {
		return "", f.err
	}
	f.message = message
	return f.messageID, nil
}
