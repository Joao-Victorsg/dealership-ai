package client

import (
	"bytes"
	"context"
	"errors"
	"lambda-update-car-status/internal/observability"
	"lambda-update-car-status/internal/service"
	"net/http"
	"net/http/httptest"
	"testing"
	"time"

	"github.com/sony/gobreaker/v2"
)

type staticToken struct{}

func (staticToken) GetToken(context.Context) (string, error) { return "tok", nil }

type errToken struct{}

func (errToken) GetToken(context.Context) (string, error) { return "", errors.New("token failure") }

type roundTripFunc func(*http.Request) (*http.Response, error)

func (f roundTripFunc) RoundTrip(r *http.Request) (*http.Response, error) { return f(r) }

type timeoutErr struct{}

func (timeoutErr) Error() string   { return "timeout" }
func (timeoutErr) Timeout() bool   { return true }
func (timeoutErr) Temporary() bool { return true }

func TestCarAPIClientPatchAndAuth(t *testing.T) {
	called := false
	ts := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		called = true
		if r.Method != http.MethodPatch {
			t.Fatalf("expected patch")
		}
		if r.Header.Get("Authorization") == "" {
			t.Fatal("missing auth")
		}
		w.WriteHeader(http.StatusOK)
	}))
	defer ts.Close()
	breaker := gobreaker.NewCircuitBreaker[any](gobreaker.Settings{Name: "x"})
	c := NewCarAPIClient(CarAPIConfig{BaseURL: ts.URL, HTTPClient: ts.Client(), TokenProvider: staticToken{}, Breaker: breaker, Logger: observability.NewJSONLogger(&bytes.Buffer{})})
	_, err := c.UpdateStatus(context.Background(), "car", "Sold")
	if err != nil {
		t.Fatal(err)
	}
	if !called {
		t.Fatal("expected downstream call")
	}
}

func TestCarAPIClientStatusClassification(t *testing.T) {
	tests := []struct {
		name          string
		status        int
		wantErr       bool
		wantTransient bool
		wantPermanent bool
	}{
		{name: "conflict is success", status: 409},
		{name: "transient status", status: 429, wantErr: true, wantTransient: true},
		{name: "permanent status", status: 401, wantErr: true, wantPermanent: true},
		{name: "unexpected status", status: 500, wantErr: true, wantTransient: true},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			ts := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
				if r.URL.Path != "/api/v1/cars/car-1" {
					t.Fatalf("unexpected path: %s", r.URL.Path)
				}
				w.WriteHeader(tt.status)
			}))
			defer ts.Close()

			breaker := gobreaker.NewCircuitBreaker[any](gobreaker.Settings{Name: "x"})
			c := NewCarAPIClient(CarAPIConfig{BaseURL: ts.URL + "/", HTTPClient: ts.Client(), TokenProvider: staticToken{}, Breaker: breaker, Logger: observability.NewJSONLogger(&bytes.Buffer{})})
			sc, err := c.UpdateStatus(context.Background(), "car-1", "Sold")
			if (err != nil) != tt.wantErr {
				t.Fatalf("error mismatch: %v", err)
			}
			if sc != tt.status {
				t.Fatalf("status mismatch: got %d want %d", sc, tt.status)
			}
			if tt.wantTransient && !service.IsTransient(err) {
				t.Fatalf("expected transient, got %v", err)
			}
			if tt.wantPermanent && !service.IsPermanent(err) {
				t.Fatalf("expected permanent, got %v", err)
			}
		})
	}
}

func TestCarAPIClientTokenFailureAndBreakerOpen(t *testing.T) {
	breaker := gobreaker.NewCircuitBreaker[any](gobreaker.Settings{Name: "x"})
	client := NewCarAPIClient(CarAPIConfig{BaseURL: "https://example.com", HTTPClient: &http.Client{}, TokenProvider: errToken{}, Breaker: breaker, Logger: observability.NewJSONLogger(&bytes.Buffer{})})
	_, err := client.UpdateStatus(context.Background(), "car", "Sold")
	if err == nil || service.IsTransient(err) {
		t.Fatalf("expected non-wrapped token error, got %v", err)
	}

	openBreaker := gobreaker.NewCircuitBreaker[any](gobreaker.Settings{Name: "open", ReadyToTrip: func(gobreaker.Counts) bool { return true }})
	if _, err := openBreaker.Execute(func() (any, error) { return nil, errors.New("fail") }); err == nil {
		t.Fatal("expected breaker priming error")
	}
	openClient := NewCarAPIClient(CarAPIConfig{BaseURL: "https://example.com", HTTPClient: &http.Client{}, TokenProvider: staticToken{}, Breaker: openBreaker, Logger: observability.NewJSONLogger(&bytes.Buffer{})})
	_, err = openClient.UpdateStatus(context.Background(), "car", "Sold")
	if !service.IsTransient(err) {
		t.Fatalf("expected transient breaker error, got %v", err)
	}
}

func TestCarAPIClientHTTPDoErrorsAreTransient(t *testing.T) {
	tests := []struct {
		name string
		err  error
	}{
		{name: "timeout", err: timeoutErr{}},
		{name: "generic", err: errors.New("connection reset")},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			httpClient := &http.Client{Transport: roundTripFunc(func(*http.Request) (*http.Response, error) { return nil, tt.err })}
			breaker := gobreaker.NewCircuitBreaker[any](gobreaker.Settings{Name: "x"})
			c := NewCarAPIClient(CarAPIConfig{BaseURL: "https://example.com", HTTPClient: httpClient, TokenProvider: staticToken{}, Breaker: breaker, Logger: observability.NewJSONLogger(&bytes.Buffer{})})
			_, err := c.UpdateStatus(context.Background(), "car", "Sold")
			if !service.IsTransient(err) {
				t.Fatalf("expected transient, got %v", err)
			}
		})
	}
}

func TestKeycloakTimeoutClassified(t *testing.T) {
	ts := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		time.Sleep(200 * time.Millisecond)
		w.WriteHeader(http.StatusOK)
		if _, err := w.Write([]byte(`{"access_token":"t","expires_in":30}`)); err != nil {
			t.Fatalf("writing response: %v", err)
		}
	}))
	defer ts.Close()
	client := &http.Client{Timeout: 50 * time.Millisecond}
	kc := NewKeycloakClient(KeycloakConfig{HTTPClient: client, TokenURL: ts.URL, ClientID: "id", ClientSecret: "sec", RefreshSkew: time.Second, Logger: observability.NewJSONLogger(&bytes.Buffer{})})
	_, err := kc.GetToken(context.Background())
	if err == nil {
		t.Fatal("expected timeout")
	}
}
