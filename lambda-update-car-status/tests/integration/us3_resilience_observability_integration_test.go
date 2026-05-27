//go:build integration

package integration

import (
	"bytes"
	"context"
	"lambda-update-car-status/internal/handler"
	"lambda-update-car-status/internal/observability"
	"lambda-update-car-status/internal/service"
	"strings"
	"testing"
	"time"

	"github.com/aws/aws-lambda-go/events"
)

type transientProc struct{}

func (transientProc) Process(context.Context, service.SaleEvent) service.ProcessingResult {
	return service.ProcessingResult{SaleID: "s", CarID: "c", Outcome: service.OutcomeTransientFailure, DownstreamStatus: 503}
}
func TestUS3BreakerOpenTimeoutObservability(t *testing.T) {
	buf := &bytes.Buffer{}
	h := handler.NewSQSHandler(transientProc{}, observability.NewJSONLogger(buf), time.Now)
	_, err := h.Handle(
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
	if !strings.Contains(buf.String(), "transient_failure") {
		t.Fatal("missing outcome log")
	}
}
