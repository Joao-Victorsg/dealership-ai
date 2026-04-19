package br.com.dealership.clientapi.repository;

import br.com.dealership.clientapi.entity.Client;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ClientRepository extends JpaRepository<Client, UUID> {

    Optional<Client> findByKeycloakId(String keycloakId);

    Optional<Client> findByCpfHash(String cpfHash);

    boolean existsByKeycloakId(String keycloakId);

    boolean existsByCpfHash(String cpfHash);
}
