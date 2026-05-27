package service

import (
	"time"

	"github.com/shopspring/decimal"
)

type SaleEventPayload struct {
	SaleID         string          `json:"saleId"`
	CarID          string          `json:"carId"`
	ClientID       string          `json:"clientId"`
	SaleValue      decimal.Decimal `json:"saleValue"`
	RegisteredAt   time.Time       `json:"registeredAt"`
	ClientSnapshot ClientSnapshot  `json:"clientSnapshot"`
	CarSnapshot    CarSnapshot     `json:"carSnapshot"`
}

type ClientSnapshot struct {
	FirstName string          `json:"firstName"`
	LastName  string          `json:"lastName"`
	CPF       string          `json:"cpf"`
	Email     string          `json:"email"`
	Address   AddressSnapshot `json:"address"`
}

type AddressSnapshot struct {
	Street       string `json:"street"`
	Number       string `json:"number"`
	Complement   string `json:"complement"`
	Neighborhood string `json:"neighborhood"`
	City         string `json:"city"`
	State        string `json:"state"`
	Postcode     string `json:"postcode"`
}

type CarSnapshot struct {
	Model             string          `json:"model"`
	Manufacturer      string          `json:"manufacturer"`
	ExternalColor     string          `json:"externalColor"`
	InternalColor     string          `json:"internalColor"`
	ManufacturingYear int             `json:"manufacturingYear"`
	Type              string          `json:"type"`
	Category          string          `json:"category"`
	VIN               string          `json:"vin"`
	ListedValue       decimal.Decimal `json:"listedValue"`
	Status            string          `json:"status"`
}

type InvoiceResult struct {
	SaleID         string `json:"saleId"`
	InvoiceBucket  string `json:"invoiceBucket"`
	InvoiceKey     string `json:"invoiceKey"`
	InvoiceURL     string `json:"invoiceUrl"`
	RecipientEmail string `json:"recipientEmail"`
	Subject        string `json:"subject"`
	RegisteredAt   string `json:"registeredAt"`
	SaleValue      string `json:"saleValue"`
	ListedValue    string `json:"listedValue"`
	TaxValue       string `json:"taxValue"`
}
