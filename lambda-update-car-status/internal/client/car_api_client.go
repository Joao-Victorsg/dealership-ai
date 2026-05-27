package client

import (
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"lambda-update-car-status/internal/observability"
	"lambda-update-car-status/internal/service"
	"net"
	"net/http"
	"strings"
	"time"

	"github.com/sony/gobreaker/v2"
)

type CarAPIConfig struct {
	BaseURL       string
	HTTPClient    *http.Client
	TokenProvider TokenProvider
	Breaker       *gobreaker.CircuitBreaker[any]
	Logger        *observability.JSONLogger
}
type CarAPIClient struct{ cfg CarAPIConfig }

func NewCarAPIClient(cfg CarAPIConfig) *CarAPIClient { return &CarAPIClient{cfg: cfg} }
func (c *CarAPIClient) UpdateStatus(ctx context.Context, carID, status string) (int, error) {
	start := time.Now()
	result, err := c.cfg.Breaker.Execute(func() (any, error) {
		token, err := c.cfg.TokenProvider.GetToken(ctx)
		if err != nil {
			return nil, err
		}
		payload, _ := json.Marshal(map[string]string{"status": status})
		url := strings.TrimRight(c.cfg.BaseURL, "/") + "/api/v1/cars/" + carID
		req, _ := http.NewRequestWithContext(ctx, http.MethodPatch, url, bytes.NewReader(payload))
		req.Header.Set("Authorization", "Bearer "+token)
		req.Header.Set("Content-Type", "application/json")
		resp, err := c.cfg.HTTPClient.Do(req)
		if err != nil {
			if ne, ok := err.(net.Error); ok && ne.Timeout() {
				return nil, service.WrapTransient(err)
			}
			return nil, service.WrapTransient(err)
		}
		defer func() { _ = resp.Body.Close() }()
		c.cfg.Logger.DependencyTiming("car_api", time.Since(start).Milliseconds(), resp.StatusCode)
		sc := resp.StatusCode
		if (sc >= 200 && sc < 300) || sc == http.StatusConflict {
			return sc, nil
		}
		if sc == 429 || sc == 502 || sc == 503 || sc == 504 {
			return sc, service.WrapTransient(fmt.Errorf("transient status: %d", sc))
		}
		if sc == 400 || sc == 401 || sc == 403 || sc == 404 {
			return sc, service.WrapPermanent(fmt.Errorf("permanent status: %d", sc))
		}
		return sc, service.WrapTransient(fmt.Errorf("unexpected status: %d", sc))
	})
	if err != nil {
		if err == gobreaker.ErrOpenState || err == gobreaker.ErrTooManyRequests {
			return 0, service.WrapTransient(err)
		}
		if v, ok := result.(int); ok {
			return v, err
		}
		return 0, err
	}
	return result.(int), nil
}
