package service

import (
	"bytes"
	"context"
	"errors"
	"fmt"
	"lambda-update-car-status/internal/observability"
	"strings"
	"testing"
)

type fakeCarAPI struct {
	status int
	err    error
	calls  int
}

func (f *fakeCarAPI) UpdateStatus(context.Context, string, string) (int, error) {
	f.calls++
	return f.status, f.err
}
func TestProcess2xxAnd409Success(t *testing.T) {
	for _, sc := range []int{200, 409} {
		sc := sc
		t.Run(fmt.Sprintf("status_%d_success", sc), func(t *testing.T) {
			api := &fakeCarAPI{status: sc}
			svc := NewCarStatusService(api, observability.NewJSONLogger(&bytes.Buffer{}))
			res := svc.Process(context.Background(), SaleEvent{SaleID: "s", CarID: "c"})
			if res.Outcome != OutcomeSuccess {
				t.Fatalf("status %d failed", sc)
			}
		})
	}
}
func TestProcessSingleAttemptNoBusinessRetry(t *testing.T) {
	api := &fakeCarAPI{status: 503, err: WrapTransient(errors.New("down"))}
	svc := NewCarStatusService(api, observability.NewJSONLogger(&bytes.Buffer{}))
	res := svc.Process(context.Background(), SaleEvent{SaleID: "s", CarID: "c"})
	if res.Outcome != OutcomeTransientFailure {
		t.Fatal("expected transient")
	}
	if api.calls != 1 {
		t.Fatalf("expected single call, got %d", api.calls)
	}
}

func TestProcessMissingIDsIsPermanentWithoutCallingClient(t *testing.T) {
	api := &fakeCarAPI{}
	svc := NewCarStatusService(api, observability.NewJSONLogger(&bytes.Buffer{}))
	res := svc.Process(context.Background(), SaleEvent{SaleID: "", CarID: "   "})
	if res.Outcome != OutcomePermanentFailure {
		t.Fatal("expected permanent")
	}
	if api.calls != 0 {
		t.Fatal("client should not be called")
	}
}

func TestProcessDeadlineExceededIsTransient(t *testing.T) {
	api := &fakeCarAPI{err: context.DeadlineExceeded}
	svc := NewCarStatusService(api, observability.NewJSONLogger(&bytes.Buffer{}))
	res := svc.Process(context.Background(), SaleEvent{SaleID: "s", CarID: "c"})
	if res.Outcome != OutcomeTransientFailure {
		t.Fatal("expected transient")
	}
}

func TestProcessPermanentError(t *testing.T) {
	api := &fakeCarAPI{status: 400, err: WrapPermanent(errors.New("bad request"))}
	svc := NewCarStatusService(api, observability.NewJSONLogger(&bytes.Buffer{}))
	res := svc.Process(context.Background(), SaleEvent{SaleID: "s", CarID: "c"})
	if res.Outcome != OutcomePermanentFailure {
		t.Fatal("expected permanent")
	}
	if !strings.Contains(res.DiagnosticContext, "permanent") {
		t.Fatal("expected permanent diagnostic context")
	}
}
