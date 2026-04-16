package br.com.dealership.car.api.domain.exception;

public final class DuplicateVinException extends CarApiException {

    private final String vin;

    public DuplicateVinException(String vin) {
        super("A car with this VIN already exists");
        this.vin = vin;
    }

    public String getVin() {
        return vin;
    }
}

