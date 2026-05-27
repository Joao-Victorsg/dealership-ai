package service

import (
	"context"
	"fmt"
	"strings"
)

type InvoiceReader interface {
	Download(ctx context.Context, bucket string, key string) ([]byte, error)
}

type EmailSender interface {
	Send(ctx context.Context, message EmailMessage) (string, error)
}

type EmailService struct {
	emailFrom string
	reader    InvoiceReader
	sender    EmailSender
}

func NewEmailService(emailFrom string, reader InvoiceReader, sender EmailSender) *EmailService {
	return &EmailService{
		emailFrom: emailFrom,
		reader:    reader,
		sender:    sender,
	}
}

func (s *EmailService) Handle(ctx context.Context, payload SendEmailInput) (SendEmailResult, error) {
	if strings.TrimSpace(payload.InvoiceBucket) == "" || strings.TrimSpace(payload.InvoiceKey) == "" {
		return SendEmailResult{}, fmt.Errorf("missing invoice location")
	}
	if strings.TrimSpace(payload.RecipientEmail) == "" {
		return SendEmailResult{}, fmt.Errorf("missing recipient email")
	}

	invoiceHTML, err := s.reader.Download(ctx, payload.InvoiceBucket, payload.InvoiceKey)
	if err != nil {
		return SendEmailResult{}, err
	}

	subject := strings.TrimSpace(payload.Subject)
	if subject == "" {
		subject = fmt.Sprintf("Dealership invoice %s", payload.SaleID)
	}

	messageID, err := s.sender.Send(ctx, EmailMessage{
		From:     s.emailFrom,
		To:       payload.RecipientEmail,
		Subject:  subject,
		HTMLBody: string(invoiceHTML),
	})
	if err != nil {
		return SendEmailResult{}, err
	}

	return SendEmailResult{
		SaleID:    payload.SaleID,
		MessageID: messageID,
		Status:    "sent",
	}, nil
}
