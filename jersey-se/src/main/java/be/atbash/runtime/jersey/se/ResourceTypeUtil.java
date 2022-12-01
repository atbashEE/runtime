package be.atbash.runtime.jersey.se;

import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.ext.Provider;

public final class ResourceTypeUtil {

    private ResourceTypeUtil() {
    }

    public static ResourceType determineType(Class<?> someClass) {
        ResourceType result = ResourceType.CLASS;
        if (Application.class.isAssignableFrom(someClass)) {
            result = ResourceType.APPLICATION;
        } else {
            if (someClass.getAnnotation(Path.class) != null ||
                    someClass.getAnnotation(Provider.class) != null) {
                result = ResourceType.RESOURCE;
            }
        }
        return result;
    }
}
