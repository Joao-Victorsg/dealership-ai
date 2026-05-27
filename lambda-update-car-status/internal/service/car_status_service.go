package service

import (
	"context"
	"errors"
	"fmt"
	"lambda-update-car-status/internal/observability"
	"strings"
)

type CarStatusUpdater interface {
	UpdateStatus(ctx context.Context, carID, status string) (int, error)
}
type CarStatusService struct {
	client CarStatusUpdater
	logger *observability.JSONLogger
}

func NewCarStatusService(client CarStatusUpdater, logger *observability.JSONLogger) *CarStatusService {
	return &CarStatusService{client: client, logger: logger}
}
func (s *CarStatusService) Process(ctx context.Context, event SaleEvent) ProcessingResult {
	if strings.TrimSpace(event.SaleID) == "" || strings.TrimSpace(event.CarID) == "" {
		return ProcessingResult{
			SaleID:            event.SaleID,
			CarID:             event.CarID,
			Outcome:           OutcomePermanentFailure,
			DiagnosticContext: "missing SaleId or CarId",
		}
	}

	status, err := s.client.UpdateStatus(ctx, event.CarID, "SOLD")
	if err == nil {
		return ProcessingResult{
			SaleID:           event.SaleID,
			CarID:            event.CarID,
			Outcome:          OutcomeSuccess,
			DownstreamStatus: status,
		}
	}

	if IsTransient(err) {
		return ProcessingResult{
			SaleID:            event.SaleID,
			CarID:             event.CarID,
			Outcome:           OutcomeTransientFailure,
			DownstreamStatus:  status,
			DiagnosticContext: err.Error(),
		}
	}

	if errors.Is(err, context.DeadlineExceeded) {
		return ProcessingResult{
			SaleID:            event.SaleID,
			CarID:             event.CarID,
			Outcome:           OutcomeTransientFailure,
			DownstreamStatus:  status,
			DiagnosticContext: err.Error(),
		}
	}

	return ProcessingResult{
		SaleID:            event.SaleID,
		CarID:             event.CarID,
		Outcome:           OutcomePermanentFailure,
		DownstreamStatus:  status,
		DiagnosticContext: fmt.Sprintf("permanent: %v", err),
	}
}
