package handler

import (
	"context"
	"errors"
	"lambda-start-invoice-workflow/internal/client"
	"log"
	"testing"

	"github.com/aws/aws-lambda-go/events"
)

func TestHandleMalformedPayloadIsAcked(t *testing.T) {
	starter := &fakeStarter{}
	h := NewSQSHandler(starter, log.Default())
	ev := events.SQSEvent{
		Records: []events.SQSMessage{{MessageId: "1", Body: `{not-json}`}},
	}

	resp, err := h.Handle(context.Background(), ev)
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	if len(resp.BatchItemFailures) != 0 {
		t.Fatal("malformed payload should be acknowledged")
	}
	if starter.calls != 0 {
		t.Fatal("starter should not be called")
	}
}

func TestHandleDuplicateExecutionIsAcked(t *testing.T) {
	starter := &fakeStarter{
		err: client.ErrExecutionAlreadyExists,
	}
	h := NewSQSHandler(starter, log.Default())
	ev := events.SQSEvent{
		Records: []events.SQSMessage{{MessageId: "1", Body: `{"saleId":"sale-123"}`}},
	}

	resp, err := h.Handle(context.Background(), ev)
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	if len(resp.BatchItemFailures) != 0 {
		t.Fatal("duplicate execution should be acknowledged")
	}
}

func TestHandleStartWorkflowFailureIsRetried(t *testing.T) {
	starter := &fakeStarter{
		err: errors.New("temporary failure"),
	}
	h := NewSQSHandler(starter, log.Default())
	ev := events.SQSEvent{
		Records: []events.SQSMessage{{MessageId: "1", Body: `{"saleId":"sale-123"}`}},
	}

	resp, err := h.Handle(context.Background(), ev)
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	if len(resp.BatchItemFailures) != 1 {
		t.Fatal("expected one batch item failure")
	}
}

func TestBuildExecutionNameSanitizesAndTruncates(t *testing.T) {
	id := " sale:123/with@chars and spaces "
	name := buildExecutionName(id)
	if name == "" {
		t.Fatal("expected execution name")
	}
	if len(name) > 80 {
		t.Fatal("execution name should be at most 80 chars")
	}
}

type fakeStarter struct {
	err   error
	calls int
}

func (f *fakeStarter) StartWorkflow(context.Context, string, string) error {
	f.calls++
	return f.err
}
