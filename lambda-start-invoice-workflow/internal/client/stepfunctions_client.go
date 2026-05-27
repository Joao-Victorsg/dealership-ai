package client

import (
	"context"
	"errors"
	"fmt"

	"github.com/aws/aws-sdk-go-v2/aws"
	"github.com/aws/aws-sdk-go-v2/service/sfn"
	sfntypes "github.com/aws/aws-sdk-go-v2/service/sfn/types"
)

var ErrExecutionAlreadyExists = errors.New("execution already exists")

type StartExecutionAPI interface {
	StartExecution(ctx context.Context, params *sfn.StartExecutionInput, optFns ...func(*sfn.Options)) (*sfn.StartExecutionOutput, error)
}

type WorkflowClient struct {
	api             StartExecutionAPI
	stateMachineARN string
}

func NewWorkflowClient(api StartExecutionAPI, stateMachineARN string) *WorkflowClient {
	return &WorkflowClient{
		api:             api,
		stateMachineARN: stateMachineARN,
	}
}

func (c *WorkflowClient) StartWorkflow(ctx context.Context, executionName string, payload string) error {
	_, err := c.api.StartExecution(ctx, &sfn.StartExecutionInput{
		StateMachineArn: aws.String(c.stateMachineARN),
		Name:            aws.String(executionName),
		Input:           aws.String(payload),
	})
	if err != nil {
		var duplicate *sfntypes.ExecutionAlreadyExists
		if errors.As(err, &duplicate) {
			return ErrExecutionAlreadyExists
		}
		return fmt.Errorf("start execution: %w", err)
	}
	return nil
}
