package service

import (
	"bytes"
	"context"
	"fmt"
	"html/template"
	"strings"

	"github.com/shopspring/decimal"
)

const invoiceTemplate = `
<!doctype html>
<html lang="en">
<head>
  <meta charset="utf-8">
  <title>Invoice {{ .SaleID }}</title>
  <style>
    body { font-family: Arial, sans-serif; margin: 24px; color: #111827; }
    h1 { margin-bottom: 0; }
    .muted { color: #6b7280; margin-top: 4px; }
    table { width: 100%; border-collapse: collapse; margin-top: 20px; }
    td, th { border: 1px solid #e5e7eb; padding: 8px; text-align: left; }
    .totals td { font-weight: bold; }
  </style>
</head>
<body>
  <h1>Dealership Invoice</h1>
  <p class="muted">Sale ID: {{ .SaleID }}</p>
  <p class="muted">Registered at: {{ .RegisteredAt }}</p>

  <h2>Customer</h2>
  <p>{{ .ClientName }} ({{ .ClientEmail }})</p>
  <p>{{ .AddressLine }}</p>

  <h2>Vehicle</h2>
  <table>
    <tr><th>Manufacturer</th><td>{{ .Manufacturer }}</td></tr>
    <tr><th>Model</th><td>{{ .Model }}</td></tr>
    <tr><th>VIN</th><td>{{ .VIN }}</td></tr>
  </table>

  <h2>Values</h2>
  <table>
    <tr><th>Listed value</th><td>{{ .ListedValue }}</td></tr>
    <tr><th>Transaction tax</th><td>{{ .TaxValue }}</td></tr>
    <tr class="totals"><th>Total sale value</th><td>{{ .SaleValue }}</td></tr>
  </table>
</body>
</html>`

type InvoiceUploader interface {
	Upload(ctx context.Context, bucket string, key string, content []byte, contentType string) error
}

type InvoiceService struct {
	bucketName string
	uploader   InvoiceUploader
}

func NewInvoiceService(bucketName string, uploader InvoiceUploader) *InvoiceService {
	return &InvoiceService{
		bucketName: bucketName,
		uploader:   uploader,
	}
}

func (s *InvoiceService) Handle(ctx context.Context, payload SaleEventPayload) (InvoiceResult, error) {
	if strings.TrimSpace(payload.SaleID) == "" {
		return InvoiceResult{}, fmt.Errorf("missing saleId")
	}
	if strings.TrimSpace(payload.ClientSnapshot.Email) == "" {
		return InvoiceResult{}, fmt.Errorf("missing clientSnapshot.email")
	}

	listedValue := payload.CarSnapshot.ListedValue
	saleValue := payload.SaleValue
	taxValue := saleValue.Sub(listedValue)
	if taxValue.IsNegative() {
		taxValue = decimal.Zero
	}

	htmlInvoice, err := buildInvoiceHTML(payload, listedValue, taxValue)
	if err != nil {
		return InvoiceResult{}, fmt.Errorf("building invoice html: %w", err)
	}

	key := fmt.Sprintf("invoices/%s.html", strings.TrimSpace(payload.SaleID))
	if err := s.uploader.Upload(ctx, s.bucketName, key, []byte(htmlInvoice), "text/html; charset=utf-8"); err != nil {
		return InvoiceResult{}, err
	}

	return InvoiceResult{
		SaleID:         payload.SaleID,
		InvoiceBucket:  s.bucketName,
		InvoiceKey:     key,
		InvoiceURL:     fmt.Sprintf("s3://%s/%s", s.bucketName, key),
		RecipientEmail: payload.ClientSnapshot.Email,
		Subject:        fmt.Sprintf("Dealership invoice %s", payload.SaleID),
		RegisteredAt:   payload.RegisteredAt.Format("2006-01-02T15:04:05Z07:00"),
		SaleValue:      saleValue.StringFixed(2),
		ListedValue:    listedValue.StringFixed(2),
		TaxValue:       taxValue.StringFixed(2),
	}, nil
}

func buildInvoiceHTML(payload SaleEventPayload, listedValue decimal.Decimal, taxValue decimal.Decimal) (string, error) {
	tpl, err := template.New("invoice").Parse(invoiceTemplate)
	if err != nil {
		return "", err
	}

	addressParts := []string{
		payload.ClientSnapshot.Address.Street,
		payload.ClientSnapshot.Address.Number,
		payload.ClientSnapshot.Address.Complement,
		payload.ClientSnapshot.Address.Neighborhood,
		payload.ClientSnapshot.Address.City,
		payload.ClientSnapshot.Address.State,
		payload.ClientSnapshot.Address.Postcode,
	}
	filteredAddress := make([]string, 0, len(addressParts))
	for _, part := range addressParts {
		if trimmed := strings.TrimSpace(part); trimmed != "" {
			filteredAddress = append(filteredAddress, trimmed)
		}
	}

	viewModel := map[string]string{
		"SaleID":       payload.SaleID,
		"RegisteredAt": payload.RegisteredAt.Format("2006-01-02T15:04:05Z07:00"),
		"ClientName":   strings.TrimSpace(payload.ClientSnapshot.FirstName + " " + payload.ClientSnapshot.LastName),
		"ClientEmail":  payload.ClientSnapshot.Email,
		"AddressLine":  strings.Join(filteredAddress, ", "),
		"Manufacturer": payload.CarSnapshot.Manufacturer,
		"Model":        payload.CarSnapshot.Model,
		"VIN":          payload.CarSnapshot.VIN,
		"ListedValue":  listedValue.StringFixed(2),
		"TaxValue":     taxValue.StringFixed(2),
		"SaleValue":    payload.SaleValue.StringFixed(2),
	}

	var out bytes.Buffer
	if err := tpl.Execute(&out, viewModel); err != nil {
		return "", err
	}
	return out.String(), nil
}
