package br.com.dealership.keycloak.registration;

import org.keycloak.Config;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

public final class SelfRegistrationClientGroupListenerProviderFactory implements EventListenerProviderFactory {

    public static final String ID = "dealership-self-registration-client-group";

    @Override
    public EventListenerProvider create(final KeycloakSession session) {
        return new SelfRegistrationClientGroupListenerProvider(session);
    }

    @Override
    public void init(final Config.Scope config) {
        // no-op
    }

    @Override
    public void postInit(final KeycloakSessionFactory factory) {
        // no-op
    }

    @Override
    public void close() {
        // no-op
    }

    @Override
    public String getId() {
        return ID;
    }
}
