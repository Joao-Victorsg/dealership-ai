package handler

import (
	"context"
	"encoding/json"
	"lambda-update-car-status/internal/observability"
	"lambda-update-car-status/internal/service"
	"time"

	"github.com/aws/aws-lambda-go/events"
)

type Processor interface {
	Process(ctx context.Context, event service.SaleEvent) service.ProcessingResult
}
type SQSHandler struct {
	processor Processor
	logger    *observability.JSONLogger
	now       func() time.Time
}

func NewSQSHandler(processor Processor, logger *observability.JSONLogger, now func() time.Time) *SQSHandler {
	return &SQSHandler{processor: processor, logger: logger, now: now}
}
func (h *SQSHandler) Handle(ctx context.Context, ev events.SQSEvent) (events.SQSEventResponse, error) {
	failures := make([]events.SQSBatchItemFailure, 0)
	for _, rec := range ev.Records {
		start := h.now()
		var msg service.SaleEvent
		if err := json.Unmarshal([]byte(rec.Body), &msg); err != nil {
			h.logger.MessageOutcome(
				"",
				"",
				string(service.OutcomePermanentFailure),
				time.Since(start).Milliseconds(),
				0,
			)
			continue
		}

		result := h.processor.Process(ctx, msg)
		h.logger.MessageOutcome(
			result.SaleID,
			result.CarID,
			string(result.Outcome),
			time.Since(start).Milliseconds(),
			result.DownstreamStatus,
		)
		if result.Outcome == service.OutcomeTransientFailure {
			failures = append(failures, events.SQSBatchItemFailure{ItemIdentifier: rec.MessageId})
		}
	}
	return events.SQSEventResponse{BatchItemFailures: failures}, nil
}
