//go:build integration

package integration

import (
	"bytes"
	"lambda-update-car-status/internal/observability"
	"strings"
	"testing"
)

func TestUS3OutboundCallTimingFields(t *testing.T) {
	buf := &bytes.Buffer{}
	l := observability.NewJSONLogger(buf)
	l.DependencyTiming("keycloak", 12, 200)
	l.DependencyTiming("car_api", 8, 409)
	out := buf.String()
	if !strings.Contains(out, "call_duration_ms") || !strings.Contains(out, "dependency") {
		t.Fatal("missing timing fields")
	}
}
