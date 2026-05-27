package service

func Classify(err error) Outcome {
	if err == nil {
		return OutcomeSuccess
	}
	if IsTransient(err) {
		return OutcomeTransientFailure
	}
	return OutcomePermanentFailure
}
