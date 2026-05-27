package observability

import (
	"encoding/json"
	"io"
	"strings"
	"sync"
)

type JSONLogger struct {
	w io.Writer
	m sync.Mutex
}

func NewJSONLogger(w io.Writer) *JSONLogger { return &JSONLogger{w: w} }

type Fields map[string]any

func (l *JSONLogger) Log(fields Fields) {
	l.m.Lock()
	defer l.m.Unlock()

	copy := Fields{}
	for k, v := range fields {
		copy[k] = sanitize(v)
	}
    
	b, _ := json.Marshal(copy)
	_, _ = l.w.Write(append(b, '\n'))
}

func (l *JSONLogger) MessageOutcome(saleID, carID, outcome string, durationMS int64, status int) {
	f := Fields{"sale_id": saleID, "car_id": carID, "outcome": outcome, "duration_ms": durationMS}
	if status > 0 {
		f["downstream_http_status"] = status
	}
	l.Log(f)
}

func (l *JSONLogger) DependencyTiming(dependency string, callDurationMS int64, status int) {
	l.Log(Fields{"dependency": dependency, "call_duration_ms": callDurationMS, "http_status": status})
}

func sanitize(v any) any {
	s, ok := v.(string)
	if !ok {
		return v
	}
	lower := strings.ToLower(s)
	if strings.Contains(lower, "bearer ") || strings.Contains(lower, "access_token") || strings.Contains(lower, "client_secret") {
		return "[REDACTED]"
	}
	return s
}
