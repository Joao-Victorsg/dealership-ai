package br.com.dealership.clientapi.entity;

import br.com.dealership.clientapi.client.dto.ViaCepResponse;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

import br.com.dealership.clientapi.persistence.CpfEncryptionConverter;

@Entity
@Table(name = "clients")
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "cpf")
public class Client {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "keycloak_id", nullable = false, unique = true, length = 255)
    private String keycloakId;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Convert(converter = CpfEncryptionConverter.class)
    @Column(name = "cpf", nullable = false, length = 100)
    private String cpf;

    @Column(name = "cpf_hash", nullable = false, unique = true, length = 64)
    private String cpfHash;

    @Column(name = "phone_number", nullable = false, length = 20)
    private String phoneNumber;

    @Embedded
    private Address address;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Embedded Address
    // ─────────────────────────────────────────────────────────────────────────

    @Embeddable
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Address {

        @Column(name = "postcode", nullable = false, length = 10)
        private String postcode;

        @Column(name = "street_number", nullable = false, length = 20)
        private String streetNumber;

        @Column(name = "street_name", length = 200)
        private String streetName;

        @Column(name = "city", length = 100)
        private String city;

        @Column(name = "state", length = 2)
        private String state;

        @Column(name = "address_searched", nullable = false)
        private boolean addressSearched;

        public static Address from(ViaCepResponse viacep, String postcode, String streetNumber) {
            if (viacep == null) {
                return Address.builder()
                        .postcode(postcode)
                        .streetNumber(streetNumber)
                        .streetName("")
                        .city("")
                        .state("")
                        .addressSearched(false)
                        .build();
            }
            return Address.builder()
                    .postcode(postcode)
                    .streetNumber(streetNumber)
                    .streetName(viacep.logradouro() != null ? viacep.logradouro() : "")
                    .city(viacep.localidade() != null ? viacep.localidade() : "")
                    .state(viacep.uf() != null ? viacep.uf() : "")
                    .addressSearched(true)
                    .build();
        }
    }
}
