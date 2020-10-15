package in.projecteka.gateway.clients;

import in.projecteka.gateway.common.Constants;
import in.projecteka.gateway.common.IdentityService;
import in.projecteka.gateway.common.cache.ServiceOptions;
import in.projecteka.gateway.registry.BridgeRegistry;
import in.projecteka.gateway.registry.CMRegistry;
import in.projecteka.gateway.registry.ServiceType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import static in.projecteka.gateway.registry.ServiceType.HIP;

public class HipInitLinkServiceClient extends ServiceClient{

    private final CMRegistry cmRegistry;
    private final BridgeRegistry bridgeRegistry;

    public HipInitLinkServiceClient(ServiceOptions serviceOptions,
                                   WebClient.Builder webClientBuilder,
                                   IdentityService identityService,
                                   CMRegistry cmRegistry,
                                   BridgeRegistry bridgeRegistry) {
        super(serviceOptions, webClientBuilder, identityService);
        this.cmRegistry = cmRegistry;
        this.bridgeRegistry = bridgeRegistry;
    }

    @Override
    protected Mono<String> getResponseUrl(String clientId, ServiceType serviceType) {
        return bridgeRegistry.getHostFor(clientId, HIP).map(host -> host + Constants.PATH_ON_ADD_CARE_CONTEXTS);
    }

    @Override
    protected Mono<String> getRequestUrl(String clientId, ServiceType serviceType) {
        return cmRegistry.getHostFor(clientId).map(host -> host + Constants.PATH_ADD_CARE_CONTEXTS);
    }
}
