package service

type SaleEvent struct {
	SaleID string `json:"SaleId"`
	CarID  string `json:"CarId"`
}
type CarStatusRequest struct {
	Status string `json:"status"`
}
type Outcome string

const (
	OutcomeSuccess          Outcome = "success"
	OutcomeTransientFailure Outcome = "transient_failure"
	OutcomePermanentFailure Outcome = "permanent_failure"
)

type ProcessingResult struct {
	SaleID            string
	CarID             string
	Outcome           Outcome
	DownstreamStatus  int
	DiagnosticContext string
}
