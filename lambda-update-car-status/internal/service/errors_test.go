package service

import (
	"errors"
	"testing"
)

func TestErrorWrappersAndPredicates(t *testing.T) {
	base := errors.New("boom")
	te := WrapTransient(base)
	pe := WrapPermanent(base)

	if te == nil || pe == nil {
		t.Fatal("expected wrapped errors")
	}
	if !IsTransient(te) || IsPermanent(te) {
		t.Fatal("unexpected transient classification")
	}
	if !IsPermanent(pe) || IsTransient(pe) {
		t.Fatal("unexpected permanent classification")
	}

	if !errors.Is(te, base) || !errors.Is(pe, base) {
		t.Fatal("expected unwrap support")
	}
	if te.Error() != "boom" || pe.Error() != "boom" {
		t.Fatal("expected delegated error message")
	}
}

func TestWrapNilReturnsNil(t *testing.T) {
	if WrapTransient(nil) != nil || WrapPermanent(nil) != nil {
		t.Fatal("expected nil")
	}
}
