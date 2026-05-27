package service

import "errors"

type TransientError struct{ Err error }

func (e TransientError) Error() string { return e.Err.Error() }
func (e TransientError) Unwrap() error { return e.Err }

type PermanentError struct{ Err error }

func (e PermanentError) Error() string { return e.Err.Error() }
func (e PermanentError) Unwrap() error { return e.Err }
func WrapTransient(err error) error {
	if err == nil {
		return nil
	}
	return TransientError{Err: err}
}
func WrapPermanent(err error) error {
	if err == nil {
		return nil
	}
	return PermanentError{Err: err}
}
func IsTransient(err error) bool { var t TransientError; return errors.As(err, &t) }
func IsPermanent(err error) bool { var p PermanentError; return errors.As(err, &p) }
