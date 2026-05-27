package client

import (
	"bytes"
	"context"
	"lambda-update-car-status/internal/observability"
	"lambda-update-car-status/internal/service"
	"net/http"
	"net/http/httptest"
	"testing"
	"time"
)

func TestGetTokenCachesAndRefreshes(t *testing.T) {
	calls := 0
	ts := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		calls++
		if calls == 1 {
			if _, err := w.Write([]byte(`{"access_token":"t1","expires_in":1}`)); err != nil {
				t.Fatalf("writing response: %v", err)
			}
			return
		}
		if _, err := w.Write([]byte(`{"access_token":"t2","expires_in":300}`)); err != nil {
			t.Fatalf("writing response: %v", err)
		}
	}))
	defer ts.Close()
	kc := NewKeycloakClient(KeycloakConfig{HTTPClient: ts.Client(), TokenURL: ts.URL, ClientID: "id", ClientSecret: "secret", RefreshSkew: 500 * time.Millisecond, Logger: observability.NewJSONLogger(&bytes.Buffer{})})
	_, err := kc.GetToken(context.Background())
	if err != nil {
		t.Fatal(err)
	}
	time.Sleep(1200 * time.Millisecond)
	tok, err := kc.GetToken(context.Background())
	if err != nil {
		t.Fatal(err)
	}
	if tok != "t2" {
		t.Fatalf("expected refresh token, got %s", tok)
	}
}
func TestGetTokenPermanentAuthFailure(t *testing.T) {
	ts := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(http.StatusUnauthorized)
	}))
	defer ts.Close()
	kc := NewKeycloakClient(KeycloakConfig{HTTPClient: ts.Client(), TokenURL: ts.URL, ClientID: "id", ClientSecret: "secret", RefreshSkew: time.Second, Logger: observability.NewJSONLogger(&bytes.Buffer{})})
	_, err := kc.GetToken(context.Background())
	if err == nil {
		t.Fatal("expected error")
	}
	if !service.IsPermanent(err) {
		t.Fatalf("expected permanent auth failure, got %v", err)
	}
}

func TestGetTokenUsesCachedTokenBeforeRefreshWindow(t *testing.T) {
	calls := 0
	ts := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		calls++
		if _, err := w.Write([]byte(`{"access_token":"cached","expires_in":300}`)); err != nil {
			t.Fatalf("writing response: %v", err)
		}
	}))
	defer ts.Close()
	kc := NewKeycloakClient(KeycloakConfig{HTTPClient: ts.Client(), TokenURL: ts.URL, ClientID: "id", ClientSecret: "secret", RefreshSkew: time.Second, Logger: observability.NewJSONLogger(&bytes.Buffer{})})

	tok1, err := kc.GetToken(context.Background())
	if err != nil {
		t.Fatal(err)
	}
	tok2, err := kc.GetToken(context.Background())
	if err != nil {
		t.Fatal(err)
	}
	if tok1 != "cached" || tok2 != "cached" || calls != 1 {
		t.Fatalf("expected one fetch with cached token reuse, calls=%d tok1=%s tok2=%s", calls, tok1, tok2)
	}
}

func TestFetchTokenErrorBranches(t *testing.T) {
	tests := []struct {
		name          string
		status        int
		body          string
		wantPermanent bool
		wantErr       bool
	}{
		{name: "forbidden", status: http.StatusForbidden, wantPermanent: true, wantErr: true},
		{name: "server error", status: http.StatusInternalServerError, wantErr: true},
		{name: "invalid json", status: http.StatusOK, body: "{", wantErr: true},
		{name: "missing token", status: http.StatusOK, body: `{"expires_in":30}`, wantPermanent: true, wantErr: true},
		{name: "string expires_in", status: http.StatusOK, body: `{"access_token":"x","expires_in":"45"}`},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			ts := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
				w.WriteHeader(tt.status)
				if tt.body != "" {
					if _, err := w.Write([]byte(tt.body)); err != nil {
						t.Fatalf("writing response: %v", err)
					}
				}
			}))
			defer ts.Close()

			kc := NewKeycloakClient(KeycloakConfig{HTTPClient: ts.Client(), TokenURL: ts.URL, ClientID: "id", ClientSecret: "secret", RefreshSkew: time.Second, Logger: observability.NewJSONLogger(&bytes.Buffer{})})
			_, err := kc.GetToken(context.Background())
			if (err != nil) != tt.wantErr {
				t.Fatalf("err mismatch: %v", err)
			}
			if tt.wantPermanent && !service.IsPermanent(err) {
				t.Fatalf("expected permanent, got %v", err)
			}
			if tt.wantErr && !tt.wantPermanent && !service.IsTransient(err) {
				t.Fatalf("expected transient, got %v", err)
			}
		})
	}
}
