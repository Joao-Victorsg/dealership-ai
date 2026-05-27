package handler

import (
	"bytes"
	"context"
	"lambda-update-car-status/internal/observability"
	"lambda-update-car-status/internal/service"
	"testing"
	"time"

	"github.com/aws/aws-lambda-go/events"
)

type fakeProcessor struct{ out service.ProcessingResult }

func (f fakeProcessor) Process(context.Context, service.SaleEvent) service.ProcessingResult {
	return f.out
}
func TestHandleBatchAckSuccess(t *testing.T) {
	h := NewSQSHandler(fakeProcessor{out: service.ProcessingResult{SaleID: "s", CarID: "c", Outcome: service.OutcomeSuccess}}, observability.NewJSONLogger(&bytes.Buffer{}), time.Now)
	ev := events.SQSEvent{Records: []events.SQSMessage{{MessageId: "1", Body: `{"SaleId":"s","CarId":"c"}`}}}
	resp, err := h.Handle(context.Background(), ev)
	if err != nil {
		t.Fatal(err)
	}
	if len(resp.BatchItemFailures) != 0 {
		t.Fatal("should ack")
	}
}
func TestHandleTransientFailureAddedToBatchFailures(t *testing.T) {
	h := NewSQSHandler(fakeProcessor{out: service.ProcessingResult{Outcome: service.OutcomeTransientFailure}}, observability.NewJSONLogger(&bytes.Buffer{}), time.Now)
	ev := events.SQSEvent{Records: []events.SQSMessage{{MessageId: "1", Body: `{"SaleId":"s","CarId":"c"}`}}}
	resp, err := h.Handle(context.Background(), ev)
	if err != nil {
		t.Fatal(err)
	}
	if len(resp.BatchItemFailures) != 1 {
		t.Fatal("expected retry item")
	}
}

func TestHandleMalformedPayloadIsSkippedWithoutFailure(t *testing.T) {
	h := NewSQSHandler(fakeProcessor{out: service.ProcessingResult{Outcome: service.OutcomeSuccess}}, observability.NewJSONLogger(&bytes.Buffer{}), time.Now)
	ev := events.SQSEvent{Records: []events.SQSMessage{{MessageId: "1", Body: `{not-json}`}}}
	resp, err := h.Handle(context.Background(), ev)
	if err != nil {
		t.Fatal(err)
	}
	if len(resp.BatchItemFailures) != 0 {
		t.Fatal("malformed payload should not be retried")
	}
}
