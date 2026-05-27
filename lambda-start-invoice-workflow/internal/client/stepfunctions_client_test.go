package client

import (
	"context"
	"errors"
	"testing"

	"github.com/aws/aws-sdk-go-v2/service/sfn"
	sfntypes "github.com/aws/aws-sdk-go-v2/service/sfn/types"
)

func TestStartWorkflowMapsDuplicateExecutionError(t *testing.T) {
	api := &fakeStartExecutionAPI{
		err: &sfntypes.ExecutionAlreadyExists{},
	}
	c := NewWorkflowClient(api, "arn:aws:states:us-east-1:000000000000:stateMachine:invoice-workflow")

	err := c.StartWorkflow(context.Background(), "sale-123", `{"saleId":"sale-123"}`)
	if !errors.Is(err, ErrExecutionAlreadyExists) {
		t.Fatalf("expected duplicate execution error, got: %v", err)
	}
}

func TestStartWorkflowCallsApiWithPayload(t *testing.T) {
	api := &fakeStartExecutionAPI{}
	c := NewWorkflowClient(api, "arn:aws:states:us-east-1:000000000000:stateMachine:invoice-workflow")

	err := c.StartWorkflow(context.Background(), "sale-123", `{"saleId":"sale-123"}`)
	if err != nil {
		t.Fatalf("expected success, got: %v", err)
	}

	if api.input == nil || api.input.Input == nil {
		t.Fatal("expected start execution input")
	}
	if got := *api.input.Input; got != `{"saleId":"sale-123"}` {
		t.Fatalf("unexpected payload: %s", got)
	}
	if api.input.Name == nil || *api.input.Name != "sale-123" {
		t.Fatalf("unexpected execution name: %+v", api.input.Name)
	}
}

type fakeStartExecutionAPI struct {
	input *sfn.StartExecutionInput
	err   error
}

func (f *fakeStartExecutionAPI) StartExecution(_ context.Context, input *sfn.StartExecutionInput, _ ...func(*sfn.Options)) (*sfn.StartExecutionOutput, error) {
	f.input = input
	return &sfn.StartExecutionOutput{}, f.err
}
