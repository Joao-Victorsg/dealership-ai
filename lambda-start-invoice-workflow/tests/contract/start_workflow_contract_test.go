package contract

import (
	"context"
	"encoding/json"
	"lambda-start-invoice-workflow/internal/handler"
	"log"
	"os"
	"path/filepath"
	"testing"

	"github.com/aws/aws-lambda-go/events"
)

func TestStartWorkflowContractPayloadAndExecutionName(t *testing.T) {
	fixture := filepath.Join("..", "fixtures", "sqs_sale_event.json")
	raw, err := os.ReadFile(fixture)
	if err != nil {
		t.Fatalf("read fixture: %v", err)
	}

	var sqsEvent events.SQSEvent
	if err := json.Unmarshal(raw, &sqsEvent); err != nil {
		t.Fatalf("unmarshal fixture: %v", err)
	}

	starter := &contractStarter{}
	h := handler.NewSQSHandler(starter, log.Default())

	resp, err := h.Handle(context.Background(), sqsEvent)
	if err != nil {
		t.Fatalf("handle error: %v", err)
	}
	if len(resp.BatchItemFailures) != 0 {
		t.Fatal("expected no failures")
	}
	if starter.executionName != "sale-123" {
		t.Fatalf("unexpected execution name: %s", starter.executionName)
	}
	if starter.payload == "" {
		t.Fatal("expected workflow payload")
	}
}

type contractStarter struct {
	executionName string
	payload       string
}

func (s *contractStarter) StartWorkflow(_ context.Context, executionName string, payload string) error {
	s.executionName = executionName
	s.payload = payload
	return nil
}
