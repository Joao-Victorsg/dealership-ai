package service

type SendEmailInput struct {
	SaleID         string `json:"saleId"`
	InvoiceBucket  string `json:"invoiceBucket"`
	InvoiceKey     string `json:"invoiceKey"`
	RecipientEmail string `json:"recipientEmail"`
	Subject        string `json:"subject"`
}

type SendEmailResult struct {
	SaleID    string `json:"saleId"`
	MessageID string `json:"messageId"`
	Status    string `json:"status"`
}

type EmailMessage struct {
	From     string
	To       string
	Subject  string
	HTMLBody string
}
