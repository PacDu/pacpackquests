package fr.pacdu.pacpackquests.platform;

import fr.pacdu.pacpackquests.Constants;
import fr.pacdu.pacpackquests.platform.services.INetworkHelper;
import fr.pacdu.pacpackquests.platform.services.IPlatformHelper;

import java.util.ServiceLoader;

/**
 * Service locator using the Java {@link ServiceLoader} API to dynamically bind loader-specific implementations
 * (Fabric, NeoForge) at runtime without creating compile-time coupling in the common module.
 */
public final class Services {

    private Services() {}

    /** The platform service instance providing environment and mod-loading utilities. */
    public static final IPlatformHelper PLATFORM = load(IPlatformHelper.class);

    /** The network service instance providing client and server packet dispatchers. */
    public static final INetworkHelper NETWORK = load(INetworkHelper.class);

    /**
     * Loads the first available service implementation for the given service interface.
     *
     * @param <T> The service interface type.
     * @param clazz The class object representing the service interface.
     * @return The loaded service implementation.
     * @throws NullPointerException if no provider is found in META-INF/services.
     */
    public static <T> T load(Class<T> clazz) {
        final T loadedService = ServiceLoader.load(clazz)
                .findFirst()
                .orElseThrow(() -> new NullPointerException("Failed to load service for " + clazz.getName()));
        Constants.LOG.debug("Loaded {} for service {}", loadedService, clazz);
        return loadedService;
    }
}
