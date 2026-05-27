//go:build integration

package integration

import (
	"bytes"
	"context"
	"encoding/json"
	"lambda-update-car-status/internal/handler"
	"lambda-update-car-status/internal/observability"
	"lambda-update-car-status/internal/service"
	"os"
	"path/filepath"
	"testing"
	"time"

	"github.com/aws/aws-lambda-go/events"
)

type seqProc struct{ i int }

func (s *seqProc) Process(context.Context, service.SaleEvent) service.ProcessingResult {
	s.i++
	if s.i == 1 {
		return service.ProcessingResult{Outcome: service.OutcomeTransientFailure}
	}
	return service.ProcessingResult{Outcome: service.OutcomeSuccess}
}
func TestUS2ReplayAndClassification(t *testing.T) {
	b, err := os.ReadFile(filepath.Join("..", "fixtures", "sqs_sale_event_duplicate.json"))
	if err != nil {
		t.Fatal(err)
	}
	var event events.SQSEvent
	if err := json.Unmarshal(b, &event); err != nil {
		t.Fatal(err)
	}
	p := &seqProc{}
	h := handler.NewSQSHandler(p, observability.NewJSONLogger(&bytes.Buffer{}), time.Now)
	resp, err := h.Handle(context.Background(), event)
	if err != nil {
		t.Fatal(err)
	}
	if len(resp.BatchItemFailures) != 1 {
		t.Fatal("expected one transient failure")
	}
}
