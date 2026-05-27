package client

import (
	"context"
	"fmt"
	"lambda-send-email/internal/service"

	"github.com/aws/aws-sdk-go-v2/aws"
	"github.com/aws/aws-sdk-go-v2/service/sesv2"
	sesv2types "github.com/aws/aws-sdk-go-v2/service/sesv2/types"
)

type SendEmailAPI interface {
	SendEmail(ctx context.Context, params *sesv2.SendEmailInput, optFns ...func(*sesv2.Options)) (*sesv2.SendEmailOutput, error)
}

type SESClient struct {
	api SendEmailAPI
}

func NewSESClient(api SendEmailAPI) *SESClient {
	return &SESClient{api: api}
}

func (c *SESClient) Send(ctx context.Context, message service.EmailMessage) (string, error) {
	out, err := c.api.SendEmail(ctx, &sesv2.SendEmailInput{
		FromEmailAddress: aws.String(message.From),
		Destination: &sesv2types.Destination{
			ToAddresses: []string{message.To},
		},
		Content: &sesv2types.EmailContent{
			Simple: &sesv2types.Message{
				Subject: &sesv2types.Content{
					Data: aws.String(message.Subject),
				},
				Body: &sesv2types.Body{
					Html: &sesv2types.Content{
						Data: aws.String(message.HTMLBody),
					},
				},
			},
		},
	})
	if err != nil {
		return "", fmt.Errorf("sending email via sesv2: %w", err)
	}
	return aws.ToString(out.MessageId), nil
}
