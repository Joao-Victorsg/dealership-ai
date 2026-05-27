package br.com.dealership.keycloak.registration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.events.Event;
import org.keycloak.events.EventType;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RealmProvider;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserProvider;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.stream.Stream;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SelfRegistrationClientGroupListenerProviderTest {

    @Mock
    private KeycloakSession session;
    @Mock
    private RealmProvider realmProvider;
    @Mock
    private UserProvider userProvider;
    @Mock
    private RealmModel realm;
    @Mock
    private UserModel user;
    @Mock
    private GroupModel group;

    private SelfRegistrationClientGroupListenerProvider provider;

    @BeforeEach
    void setUp() {
        provider = new SelfRegistrationClientGroupListenerProvider(session);
    }

    @Test
    void shouldJoinDedicatedClientGroupOnRegisterEvent() {
        final Event event = registerEvent();
        when(session.realms()).thenReturn(realmProvider);
        when(session.users()).thenReturn(userProvider);
        when(realmProvider.getRealm("realm-id")).thenReturn(realm);
        when(userProvider.getUserById(realm, "user-id")).thenReturn(user);
        when(realm.getGroupsStream()).thenReturn(Stream.of(group));
        when(group.getName()).thenReturn(SelfRegistrationClientGroupListenerProvider.GROUP_NAME);
        when(user.isMemberOf(group)).thenReturn(false);

        provider.onEvent(event);

        verify(user).joinGroup(group);
    }

    @Test
    void shouldSkipWhenUserIsAlreadyMemberOfDedicatedGroup() {
        final Event event = registerEvent();
        when(session.realms()).thenReturn(realmProvider);
        when(session.users()).thenReturn(userProvider);
        when(realmProvider.getRealm("realm-id")).thenReturn(realm);
        when(userProvider.getUserById(realm, "user-id")).thenReturn(user);
        when(realm.getGroupsStream()).thenReturn(Stream.of(group));
        when(group.getName()).thenReturn(SelfRegistrationClientGroupListenerProvider.GROUP_NAME);
        when(user.isMemberOf(group)).thenReturn(true);

        provider.onEvent(event);

        verify(user, never()).joinGroup(group);
    }

    @Test
    void shouldSkipWhenDedicatedGroupDoesNotExist() {
        final Event event = registerEvent();
        when(session.realms()).thenReturn(realmProvider);
        when(session.users()).thenReturn(userProvider);
        when(realmProvider.getRealm("realm-id")).thenReturn(realm);
        when(userProvider.getUserById(realm, "user-id")).thenReturn(user);
        when(realm.getGroupsStream()).thenReturn(Stream.empty());

        provider.onEvent(event);

        verify(user, never()).joinGroup(group);
    }

    @Test
    void shouldIgnoreNonRegisterEvents() {
        final Event event = new Event();
        event.setType(EventType.LOGIN);

        provider.onEvent(event);

        verifyNoInteractions(realmProvider, userProvider);
    }

    @Test
    void shouldSkipWhenRealmCannotBeResolved() {
        when(session.realms()).thenReturn(realmProvider);
        provider.onEvent(registerEvent());

        verifyNoInteractions(userProvider);
    }

    private static Event registerEvent() {
        final Event event = new Event();
        event.setType(EventType.REGISTER);
        event.setRealmId("realm-id");
        event.setUserId("user-id");
        return event;
    }
}
