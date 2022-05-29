/*
 * Copyright 2021-2022 Rudy De Busscher (https://www.atbash.be)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package be.atbash.runtime.security.jwt.jaxrs;

import be.atbash.runtime.core.data.util.SystemPropertyUtil;
import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.container.DynamicFeature;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.FeatureContext;
import jakarta.ws.rs.ext.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Arrays.asList;

/**
 * Based on SmallRye JWT.
 */
@Provider
public class JWTAuthorizationFilterRegistrar implements DynamicFeature {

    private static final Logger LOGGER = LoggerFactory.getLogger(JWTAuthorizationFilterRegistrar.class);

    private static final DenyAllFilter denyAllFilter = new DenyAllFilter();
    private final Set<Class<? extends Annotation>> permissionAnnotations = new HashSet<>(
            asList(DenyAll.class, PermitAll.class, RolesAllowed.class));

    private final Set<String> missingPermissionAnnotationWarning = new HashSet<>();

    @Override
    public void configure(ResourceInfo resourceInfo, FeatureContext context) {
        Annotation annotation = getPermissionAnnotation(resourceInfo);
        if (annotation != null) {
            if (annotation instanceof DenyAll) {
                configureDenyAll(context);
            } else if (annotation instanceof RolesAllowed) {
                configureRolesAllowed((RolesAllowed) annotation, context);
            }
        } else {
            if (SystemPropertyUtil.getInstance().isTck("jwt")) {
                if (hasSecurityAnnotations(resourceInfo)) {
                    // DenyAll when this method has no annotation but other methods in the Resource have annotations.
                    // (and we have the TCK flag -> allow the method which is against the spirit of the spec and the security best practives
                    configureDenyAll(context);
                }
            } else {
                // Default Atbash implementation -> @LoginConfig means requires JWT Auth access control (as the spec says)
                // And thus no annotations means denyAll.
                configureDenyAll(context);
                String resourceReference = resourceInfo.getResourceMethod().toString();
                if (!missingPermissionAnnotationWarning.contains(resourceReference)) {
                    missingPermissionAnnotationWarning.add(resourceReference);
                    LOGGER.atWarn().log("Missing Permission annotation (@RolesAllowed or @PermitAll) on " + resourceReference);
                }

            }
        }
    }

    private void configureRolesAllowed(RolesAllowed rolesAllowed, FeatureContext context) {
        context.register(new RolesAllowedFilter(rolesAllowed.value()));
    }

    private void configureDenyAll(FeatureContext context) {
        context.register(denyAllFilter);
    }

    private Annotation getPermissionAnnotation(ResourceInfo resourceInfo) {
        Annotation annotation = getAnnotation(
                resourceInfo.getResourceMethod().getDeclaredAnnotations(),
                () -> resourceInfo.getResourceMethod().toString());
        if (annotation == null) {
            annotation = getAnnotation(resourceInfo.getResourceClass().getDeclaredAnnotations(),
                    () -> resourceInfo.getResourceClass().getCanonicalName());
        }

        return annotation;
    }

    private Annotation getAnnotation(Annotation[] declaredAnnotations,
                                     Supplier<String> annotationPlacementDescriptor) {
        List<Annotation> annotations = Stream.of(declaredAnnotations)
                .filter(annotation -> permissionAnnotations.contains(annotation.annotationType()))
                .collect(Collectors.toList());
        switch (annotations.size()) {
            case 0:
                return null;
            case 1:
                return annotations.iterator().next();
            default:
                // FIXME
                throw new RuntimeException("Multiple Permission annotations found at " + annotationPlacementDescriptor.get());
        }
    }

    private boolean hasSecurityAnnotations(ResourceInfo resource) {
        // resource methods are inherited (see JAX-RS spec, chapter 3.6)
        // resource methods must be `public` (see JAX-RS spec, chapter 3.3.1)
        // hence `resourceClass.getMethods` -- returns public methods, including inherited ones
        return Stream.of(resource.getResourceClass().getMethods())
                .filter(this::isResourceMethod)
                .anyMatch(this::hasSecurityAnnotations);
    }

    private boolean hasSecurityAnnotations(Method method) {
        return Stream.of(method.getAnnotations())
                .anyMatch(annotation -> permissionAnnotations.contains(annotation.annotationType()));
    }

    private boolean isResourceMethod(Method method) {
        // resource methods are methods annotated with an annotation that is itself annotated with @HttpMethod
        // (see JAX-RS spec, chapter 3.3)
        return Stream.of(method.getAnnotations())
                .anyMatch(annotation -> annotation.annotationType().getAnnotation(HttpMethod.class) != null);
    }
}
