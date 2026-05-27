package contract

import (
	"bytes"
	"context"
	"encoding/json"
	"lambda-update-car-status/internal/client"
	"lambda-update-car-status/internal/observability"
	"net/http"
	"net/http/httptest"
	"testing"

	"github.com/sony/gobreaker/v2"
)

type tokenStub struct{}

func (tokenStub) GetToken(context.Context) (string, error) { return "token", nil }
func TestCarAPIContractPatchStatusBody(t *testing.T) {
	var method, path, body string
	ts := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		method = r.Method
		path = r.URL.Path
		buf := new(bytes.Buffer)
		if _, err := buf.ReadFrom(r.Body); err != nil {
			t.Fatalf("read body: %v", err)
		}
		body = buf.String()
		w.WriteHeader(http.StatusOK)
	}))
	defer ts.Close()
	c := client.NewCarAPIClient(client.CarAPIConfig{BaseURL: ts.URL, HTTPClient: ts.Client(), TokenProvider: tokenStub{}, Breaker: gobreaker.NewCircuitBreaker[any](gobreaker.Settings{Name: "c"}), Logger: observability.NewJSONLogger(&bytes.Buffer{})})
	_, err := c.UpdateStatus(context.Background(), "car-1", "SOLD")
	if err != nil {
		t.Fatal(err)
	}
	if method != http.MethodPatch {
		t.Fatalf("expected PATCH got %s", method)
	}
	if path != "/api/v1/cars/car-1" {
		t.Fatalf("unexpected path: %s", path)
	}
	var p map[string]string
	if err := json.Unmarshal([]byte(body), &p); err != nil {
		t.Fatalf("unmarshal body: %v", err)
	}
	if p["status"] != "SOLD" {
		t.Fatal("status mismatch")
	}
}
