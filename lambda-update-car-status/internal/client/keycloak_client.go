package client

import (
	"context"
	"encoding/json"
	"fmt"
	"lambda-update-car-status/internal/observability"
	"lambda-update-car-status/internal/service"
	"net/http"
	"net/url"
	"strconv"
	"strings"
	"sync"
	"time"
)

type TokenProvider interface {
	GetToken(ctx context.Context) (string, error)
}
type KeycloakConfig struct {
	HTTPClient   *http.Client
	TokenURL     string
	ClientID     string
	ClientSecret string
	RefreshSkew  time.Duration
	Logger       *observability.JSONLogger
}
type KeycloakClient struct {
	cfg       KeycloakConfig
	mu        sync.Mutex
	token     string
	expiresAt time.Time
}

func NewKeycloakClient(cfg KeycloakConfig) *KeycloakClient { return &KeycloakClient{cfg: cfg} }
func (k *KeycloakClient) GetToken(ctx context.Context) (string, error) {
	k.mu.Lock()
	if k.token != "" && time.Now().Before(k.expiresAt.Add(-k.cfg.RefreshSkew)) {
		t := k.token
		k.mu.Unlock()
		return t, nil
	}
	k.mu.Unlock()
	token, exp, err := k.fetchToken(ctx)
	if err != nil {
		return "", err
	}
	k.mu.Lock()
	k.token = token
	k.expiresAt = exp
	k.mu.Unlock()
	return token, nil
}
func (k *KeycloakClient) fetchToken(ctx context.Context) (string, time.Time, error) {
	start := time.Now()
	form := url.Values{}
	form.Set("grant_type", "client_credentials")
	form.Set("client_id", k.cfg.ClientID)
	form.Set("client_secret", k.cfg.ClientSecret)
	req, _ := http.NewRequestWithContext(ctx, http.MethodPost, k.cfg.TokenURL, strings.NewReader(form.Encode()))
	req.Header.Set("Content-Type", "application/x-www-form-urlencoded")
	resp, err := k.cfg.HTTPClient.Do(req)
	if err != nil {
		return "", time.Time{}, service.WrapTransient(err)
	}
	defer func() { _ = resp.Body.Close() }()
	k.cfg.Logger.DependencyTiming("keycloak", time.Since(start).Milliseconds(), resp.StatusCode)
	if resp.StatusCode == http.StatusUnauthorized || resp.StatusCode == http.StatusForbidden {
		return "", time.Time{}, service.WrapPermanent(fmt.Errorf("auth failure: %d", resp.StatusCode))
	}
	if resp.StatusCode < 200 || resp.StatusCode >= 300 {
		return "", time.Time{}, service.WrapTransient(fmt.Errorf("token status: %d", resp.StatusCode))
	}
	var payload map[string]any
	if err := json.NewDecoder(resp.Body).Decode(&payload); err != nil {
		return "", time.Time{}, service.WrapTransient(err)
	}
	t, _ := payload["access_token"].(string)
	if t == "" {
		return "", time.Time{}, service.WrapPermanent(fmt.Errorf("missing access_token"))
	}
	expiresIn := int64(300)
	switch v := payload["expires_in"].(type) {
	case float64:
		expiresIn = int64(v)
	case string:
		if i, err := strconv.ParseInt(v, 10, 64); err == nil {
			expiresIn = i
		}
	}
	return t, time.Now().Add(time.Duration(expiresIn) * time.Second), nil
}
