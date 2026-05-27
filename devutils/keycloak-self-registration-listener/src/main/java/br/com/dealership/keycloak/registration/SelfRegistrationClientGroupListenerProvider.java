package br.com.dealership.keycloak.registration;

import org.jboss.logging.Logger;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import java.util.Objects;

public final class SelfRegistrationClientGroupListenerProvider implements EventListenerProvider {

    static final String GROUP_NAME = "self-registered-client";

    private static final Logger LOGGER =
            Logger.getLogger(SelfRegistrationClientGroupListenerProvider.class);

    private final KeycloakSession session;

    public SelfRegistrationClientGroupListenerProvider(final KeycloakSession session) {
        this.session = Objects.requireNonNull(session);
    }

    @Override
    public void onEvent(final Event event) {
        if (event == null || event.getType() != EventType.REGISTER) {
            return;
        }
        if (event.getRealmId() == null || event.getUserId() == null) {
            LOGGER.warn("Skipping REGISTER event without realmId or userId");
            return;
        }

        final RealmModel realm = session.realms().getRealm(event.getRealmId());
        if (realm == null) {
            LOGGER.warnf("Skipping REGISTER event because realm %s was not found", event.getRealmId());
            return;
        }

        final UserModel user = session.users().getUserById(realm, event.getUserId());
        if (user == null) {
            LOGGER.warnf("Skipping REGISTER event because user %s was not found", event.getUserId());
            return;
        }

        final GroupModel group = realm.getGroupsStream()
                .filter(candidate -> GROUP_NAME.equals(candidate.getName()))
                .findFirst()
                .orElse(null);
        if (group == null) {
            LOGGER.warnf("Skipping REGISTER event because group %s was not found", GROUP_NAME);
            return;
        }

        if (!user.isMemberOf(group)) {
            user.joinGroup(group);
        }
    }

    @Override
    public void onEvent(final AdminEvent adminEvent, final boolean includeRepresentation) {
        // no-op
    }

    @Override
    public void close() {
        // no-op
    }
}
