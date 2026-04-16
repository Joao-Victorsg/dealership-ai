package br.com.dealership.car.api.domain.enums;

public enum CarSortField {

    REGISTRATION_DATE("registrationDate"),
    LISTED_VALUE("listedValue"),
    MANUFACTURING_YEAR("manufacturingYear"),
    MODEL("model"),
    MANUFACTURER("manufacturer");

    private final String fieldName;

    CarSortField(String fieldName) {
        this.fieldName = fieldName;
    }

    public String fieldName() {
        return fieldName;
    }
}

