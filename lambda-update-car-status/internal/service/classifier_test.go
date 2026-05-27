package service

import (
	"errors"
	"testing"
)

func TestClassifyMatrix(t *testing.T) {
	if got := Classify(nil); got != OutcomeSuccess {
		t.Fatal(got)
	}
	if got := Classify(WrapTransient(errors.New("timeout"))); got != OutcomeTransientFailure {
		t.Fatal(got)
	}
	if got := Classify(WrapPermanent(errors.New("bad payload"))); got != OutcomePermanentFailure {
		t.Fatal(got)
	}
}
