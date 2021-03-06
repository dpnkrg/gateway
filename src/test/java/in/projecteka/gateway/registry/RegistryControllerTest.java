package in.projecteka.gateway.registry;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nimbusds.jose.jwk.JWKSet;
import in.projecteka.gateway.clients.model.ClientResponse;
import in.projecteka.gateway.common.AdminAuthenticator;
import in.projecteka.gateway.common.Authenticator;
import in.projecteka.gateway.registry.model.BridgeServiceRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;
import reactor.core.publisher.Mono;

import java.util.List;

import static in.projecteka.gateway.common.Constants.GW_PATH_HI_SERVICE_BY_ID;
import static in.projecteka.gateway.common.Constants.HFR_BRIDGES_BRIDGE_ID;
import static in.projecteka.gateway.common.Constants.HFR_BRIDGES_BRIDGE_ID_SERVICES;
import static in.projecteka.gateway.common.Constants.INTERNAL_BRIDGES;
import static in.projecteka.gateway.common.Constants.INTERNAL_BRIDGES_BRIDGE_ID_SERVICES;
import static in.projecteka.gateway.common.Constants.INTERNAL_GET_FACILITY_BY_ID;
import static in.projecteka.gateway.common.Constants.INTERNAL_SEARCH_FACILITY_BY_NAME;
import static in.projecteka.gateway.common.Role.ADMIN;
import static in.projecteka.gateway.common.Role.CM;
import static in.projecteka.gateway.common.Role.HFR;
import static in.projecteka.gateway.common.TestBuilders.OBJECT_MAPPER;
import static in.projecteka.gateway.registry.TestBuilders.bridgeRegistryRequest;
import static in.projecteka.gateway.registry.TestBuilders.bridgeServiceRequest;
import static in.projecteka.gateway.registry.TestBuilders.facilityRepresentationBuilder;
import static in.projecteka.gateway.registry.TestBuilders.hfrBridgeResponse;
import static in.projecteka.gateway.registry.TestBuilders.serviceProfileResponse;
import static in.projecteka.gateway.testcommon.TestBuilders.caller;
import static in.projecteka.gateway.testcommon.TestBuilders.string;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static reactor.core.publisher.Mono.just;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class RegistryControllerTest {

    @Autowired
    WebTestClient webTestClient;

    @MockBean(name = "centralRegistryJWKSet")
    JWKSet jwkSet;

    @MockBean
    AdminAuthenticator adminAuthenticator;

    @MockBean
    RegistryService registryService;

    @MockBean
    Authenticator authenticator;

    @Test
    void shouldPopulateBridgeEntryAndCreateClient() {
        var token = string();
        var clientId = string();
        var bridgeRegistryRequest = bridgeRegistryRequest().build();
        var caller = caller().clientId(clientId).roles(List.of(ADMIN)).build();
        var clientResponse = ClientResponse.builder().id(clientId).build();
        when(adminAuthenticator.verify(token)).thenReturn(just(caller));
        when(registryService.populateBridgeEntry(bridgeRegistryRequest)).thenReturn(just(clientResponse));

        webTestClient
                .put()
                .uri(INTERNAL_BRIDGES)
                .header(AUTHORIZATION, token)
                .body(BodyInserters.fromValue(bridgeRegistryRequest))
                .exchange()
                .expectStatus()
                .isOk();
    }

    @Test
    void shouldPopulateBridgeServicesEntriesAndAddRoles() {
        var token = string();
        var clientId = string();
        var bridgeId = string();
        var bridgeServiceRequest = bridgeServiceRequest().build();
        var caller = caller().clientId(clientId).roles(List.of(ADMIN)).build();
        when(adminAuthenticator.verify(token)).thenReturn(just(caller));
        when(registryService.populateBridgeServicesEntries(bridgeId, List.of(bridgeServiceRequest))).thenReturn(Mono.empty());

        webTestClient
                .put()
                .uri(INTERNAL_BRIDGES_BRIDGE_ID_SERVICES, bridgeId)
                .contentType(MediaType.APPLICATION_JSON)
                .header(AUTHORIZATION, token)
                .body(Mono.just(List.of(bridgeServiceRequest)), BridgeServiceRequest.class)
                .exchange()
                .expectStatus()
                .isOk();
    }

    @Test
    void shouldGetServiceProfileForGivenServiceId() throws JsonProcessingException {
        var token = string();
        var clientId = string();
        var serviceId = string();
        var caller = caller().clientId(clientId).build();
        var serviceProfileResponse = serviceProfileResponse().build();
        var serviceProfileJson = OBJECT_MAPPER.writeValueAsString(serviceProfileResponse);
        when(authenticator.verify(token)).thenReturn(just(caller));
        when(registryService.serviceProfile(serviceId)).thenReturn(Mono.just(serviceProfileResponse));

        webTestClient
                .get()
                .uri(GW_PATH_HI_SERVICE_BY_ID, serviceId)
                .header(AUTHORIZATION, token)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .json(serviceProfileJson);
    }

    @Test
    void shouldGetBridgeProfileForGivenBridgeId() throws JsonProcessingException {
        var token = string();
        var clientId = string();
        var bridgeId = string();
        var caller = caller().clientId(clientId).roles(List.of(HFR)).build();
        var bridgeProfileResponse = hfrBridgeResponse().build();
        var bridgeProfileJson = OBJECT_MAPPER.writeValueAsString(bridgeProfileResponse);
        when(authenticator.verify(token)).thenReturn(just(caller));
        when(registryService.bridgeProfile(bridgeId)).thenReturn(Mono.just(bridgeProfileResponse));

        webTestClient
                .get()
                .uri(HFR_BRIDGES_BRIDGE_ID, bridgeId)
                .header(AUTHORIZATION, token)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .json(bridgeProfileJson);
    }

    @Test
    void shouldPopulateBridgeServicesEntriesAndAddRolesforHFR() {
        var token = string();
        var clientId = string();
        var bridgeId = string();
        var bridgeServiceRequest = bridgeServiceRequest().build();
        var caller = caller().clientId(clientId).roles(List.of(HFR)).build();
        when(authenticator.verify(token)).thenReturn(just(caller));
        when(registryService.populateBridgeServicesEntries(bridgeId, List.of(bridgeServiceRequest))).thenReturn(Mono.empty());

        webTestClient
                .put()
                .uri(HFR_BRIDGES_BRIDGE_ID_SERVICES, bridgeId)
                .contentType(MediaType.APPLICATION_JSON)
                .header(AUTHORIZATION, token)
                .body(Mono.just(List.of(bridgeServiceRequest)), BridgeServiceRequest.class)
                .exchange()
                .expectStatus()
                .isOk();
    }

    @Test
    void shouldReturnListOfProvidersByName() throws JsonProcessingException {
        var token = string();
        var clientId = string();
        var name = string();
        var state = string();
        var district = string();
        var facilities = List.of(facilityRepresentationBuilder().build());
        var caller = caller().clientId(clientId).roles(List.of(CM)).build();

        var response = OBJECT_MAPPER.writeValueAsString(facilities);
        when(authenticator.verify(token)).thenReturn(just(caller));
        when(registryService.searchFacilityByName(eq(name), eq(state), eq(district))).thenReturn(Mono.just(facilities));

        webTestClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path(INTERNAL_SEARCH_FACILITY_BY_NAME)
                        .queryParam("name", name)
                        .queryParam("district", district)
                        .queryParam("state", state)
                        .build())
                .header(AUTHORIZATION, token)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .json(response);
    }

    @Test
    void shouldReturnFacilityById() throws JsonProcessingException {
        var token = string();
        var clientId = string();
        var facilityId = string();
        var facility = facilityRepresentationBuilder().build();
        var caller = caller().clientId(clientId).roles(List.of(CM)).build();

        var response = OBJECT_MAPPER.writeValueAsString(facility);
        when(authenticator.verify(token)).thenReturn(just(caller));
        when(registryService.getFacilityById(eq(facilityId))).thenReturn(Mono.just(facility));

        webTestClient
                .get()
                .uri(INTERNAL_GET_FACILITY_BY_ID, facilityId)
                .header(AUTHORIZATION, token)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .json(response);
    }

}
