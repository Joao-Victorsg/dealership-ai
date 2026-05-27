//go:build integration

package integration

import (
	"bytes"
	"context"
	"lambda-update-car-status/internal/handler"
	"lambda-update-car-status/internal/observability"
	"lambda-update-car-status/internal/service"
	"testing"
	"time"

	"github.com/aws/aws-lambda-go/events"
)

type multiProc struct{ out service.Outcome }

func (m multiProc) Process(context.Context, service.SaleEvent) service.ProcessingResult {
	return service.ProcessingResult{Outcome: m.out, SaleID: "s", CarID: "c"}
}
func TestRegressionCriticalPaths(t *testing.T) {
	cases := []struct {
		name         string
		outcome      service.Outcome
		wantFailures int
	}{
		{name: "success", outcome: service.OutcomeSuccess, wantFailures: 0},
		{name: "transient_failure", outcome: service.OutcomeTransientFailure, wantFailures: 1},
		{name: "permanent_failure", outcome: service.OutcomePermanentFailure, wantFailures: 0},
	}
	for _, tt := range cases {
		tt := tt
		t.Run(tt.name, func(t *testing.T) {
			h := handler.NewSQSHandler(
				multiProc{out: tt.outcome},
				observability.NewJSONLogger(&bytes.Buffer{}),
				time.Now,
			)
			resp, err := h.Handle(
				context.Background(),
				events.SQSEvent{
					Records: []events.SQSMessage{
						{MessageId: "1", Body: `{"SaleId":"s","CarId":"c"}`},
					},
				},
			)
			if err != nil {
				t.Fatal(err)
			}
			if got := len(resp.BatchItemFailures); got != tt.wantFailures {
				t.Fatalf("unexpected failures count: got %d want %d", got, tt.wantFailures)
			}
		})
	}
}
