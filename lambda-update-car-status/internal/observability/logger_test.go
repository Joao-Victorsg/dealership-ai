package observability

import (
	"bytes"
	"encoding/json"
	"strings"
	"testing"
)

func TestLoggerRedactsSecrets(t *testing.T) {
	buf := &bytes.Buffer{}
	l := NewJSONLogger(buf)
	l.Log(Fields{"token": "Bearer abc", "secret": "client_secret=xyz"})
	out := buf.String()
	if strings.Contains(out, "abc") || strings.Contains(out, "xyz") {
		t.Fatal("secret leaked")
	}
}

func TestMessageOutcomeIncludesStatusOnlyWhenPresent(t *testing.T) {
	buf := &bytes.Buffer{}
	l := NewJSONLogger(buf)

	l.MessageOutcome("sale-1", "car-1", "success", 10, 0)
	l.MessageOutcome("sale-2", "car-2", "success", 12, 409)

	lines := strings.Split(strings.TrimSpace(buf.String()), "\n")
	if len(lines) != 2 {
		t.Fatalf("expected 2 log lines, got %d", len(lines))
	}

	var first map[string]any
	if err := json.Unmarshal([]byte(lines[0]), &first); err != nil {
		t.Fatal(err)
	}
	if _, ok := first["downstream_http_status"]; ok {
		t.Fatal("did not expect downstream_http_status for zero status")
	}

	var second map[string]any
	if err := json.Unmarshal([]byte(lines[1]), &second); err != nil {
		t.Fatal(err)
	}
	if second["downstream_http_status"].(float64) != 409 {
		t.Fatal("expected downstream_http_status=409")
	}
}

func TestDependencyTimingAndNonStringSanitize(t *testing.T) {
	buf := &bytes.Buffer{}
	l := NewJSONLogger(buf)
	l.DependencyTiming("car_api", 25, 200)
	l.Log(Fields{"count": 1})

	out := buf.String()
	if !strings.Contains(out, "\"dependency\":\"car_api\"") || !strings.Contains(out, "\"count\":1") {
		t.Fatal("expected dependency and non-string fields in output")
	}
}
