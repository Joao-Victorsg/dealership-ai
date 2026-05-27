//go:build integration

package integration

import (
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"lambda-update-car-status/internal/handler"
	"lambda-update-car-status/internal/observability"
	"lambda-update-car-status/internal/service"
	"os"
	"path/filepath"
	"testing"
	"time"

	"github.com/aws/aws-lambda-go/events"
)

type proc struct{ out service.ProcessingResult }

func (p proc) Process(context.Context, service.SaleEvent) service.ProcessingResult { return p.out }
func TestUS1HappyPathAnd409Ack(t *testing.T) {
	fixture := filepath.Join("..", "fixtures", "sqs_sale_event.json")
	b, err := os.ReadFile(fixture)
	if err != nil {
		t.Fatal(err)
	}
	var event events.SQSEvent
	if err := json.Unmarshal(b, &event); err != nil {
		t.Fatal(err)
	}
	for _, status := range []int{200, 409} {
		status := status
		t.Run(fmt.Sprintf("status_%d_ack", status), func(t *testing.T) {
			h := handler.NewSQSHandler(
				proc{
					out: service.ProcessingResult{
						SaleID:           "sale-123",
						CarID:            "car-456",
						Outcome:          service.OutcomeSuccess,
						DownstreamStatus: status,
					},
				},
				observability.NewJSONLogger(&bytes.Buffer{}),
				time.Now,
			)
			resp, err := h.Handle(context.Background(), event)
			if err != nil {
				t.Fatal(err)
			}
			if len(resp.BatchItemFailures) != 0 {
				t.Fatalf("status %d should ack", status)
			}
		})
	}
}
