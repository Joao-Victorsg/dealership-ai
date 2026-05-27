package handler

import (
	"context"
	"encoding/json"
	"errors"
	"fmt"
	"lambda-start-invoice-workflow/internal/client"
	"log"
	"regexp"
	"strings"

	"github.com/aws/aws-lambda-go/events"
)

var executionNameSanitizer = regexp.MustCompile(`[^A-Za-z0-9_-]`)

type WorkflowStarter interface {
	StartWorkflow(ctx context.Context, executionName string, payload string) error
}

type SQSHandler struct {
	starter WorkflowStarter
	logger  *log.Logger
}

func NewSQSHandler(starter WorkflowStarter, logger *log.Logger) *SQSHandler {
	return &SQSHandler{
		starter: starter,
		logger:  logger,
	}
}

func (h *SQSHandler) Handle(ctx context.Context, ev events.SQSEvent) (events.SQSEventResponse, error) {
	failures := make([]events.SQSBatchItemFailure, 0)
	for _, record := range ev.Records {
		saleID, err := extractSaleID(record.Body)
		if err != nil {
			h.logger.Printf("dropping malformed invoice event message_id=%s error=%v", record.MessageId, err)
			continue
		}

		executionName := buildExecutionName(saleID)
		if err := h.starter.StartWorkflow(ctx, executionName, record.Body); err != nil {
			if errors.Is(err, client.ErrExecutionAlreadyExists) {
				h.logger.Printf("duplicate workflow execution sale_id=%s execution_name=%s", saleID, executionName)
				continue
			}
			h.logger.Printf("failed to start workflow sale_id=%s execution_name=%s error=%v", saleID, executionName, err)
			failures = append(failures, events.SQSBatchItemFailure{ItemIdentifier: record.MessageId})
		}
	}

	return events.SQSEventResponse{BatchItemFailures: failures}, nil
}

type saleEventIdentifier struct {
	SaleID       string `json:"saleId"`
	LegacySaleID string `json:"SaleId"`
}

func extractSaleID(raw string) (string, error) {
	var payload saleEventIdentifier
	if err := json.Unmarshal([]byte(raw), &payload); err != nil {
		return "", fmt.Errorf("invalid json payload: %w", err)
	}
	saleID := strings.TrimSpace(payload.SaleID)
	if saleID == "" {
		saleID = strings.TrimSpace(payload.LegacySaleID)
	}
	if saleID == "" {
		return "", fmt.Errorf("missing saleId")
	}
	return saleID, nil
}

func buildExecutionName(saleID string) string {
	name := executionNameSanitizer.ReplaceAllString(strings.TrimSpace(saleID), "-")
	name = strings.Trim(name, "-_")
	if name == "" {
		name = "sale"
	}
	if len(name) > 80 {
		name = name[:80]
	}
	return name
}
