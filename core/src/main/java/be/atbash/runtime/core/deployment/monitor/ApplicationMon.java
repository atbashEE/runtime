/*
 * Copyright 2021-2023 Rudy De Busscher (https://www.atbash.be)
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
package be.atbash.runtime.core.deployment.monitor;

import be.atbash.runtime.core.data.deployment.AbstractDeployment;
import be.atbash.runtime.core.data.deployment.ArchiveDeployment;
import be.atbash.runtime.core.data.exception.UnexpectedException;

import javax.management.MBeanException;
import javax.management.openmbean.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ApplicationMon implements ApplicationMonMBean {

    private static final String[] JMX_ATTRIBUTE_NAMES = {"Name", "ContextRoot", "Specifications"};

    private static final CompositeType JMX_COMPOSITE_TYPE;

    static {
        OpenType<?>[] types = new OpenType[3];

        types[0] = SimpleType.STRING;  //name
        types[1] = SimpleType.STRING;  //context root
        types[2] = SimpleType.STRING;  //specifications

        try {
            JMX_COMPOSITE_TYPE = new CompositeType("Application data type",
                    "data type for Application information",
                    JMX_ATTRIBUTE_NAMES,
                    JMX_ATTRIBUTE_NAMES,
                    types);
        } catch (OpenDataException e) {
            throw new UnexpectedException(UnexpectedException.UnexpectedExceptionCode.UE001, e);
        }
    }

    private final List<ApplicationInfo> applications = new ArrayList<>();

    public List<ApplicationInfo> getApplications() {
        return applications;
    }

    @Override
    public CompositeData[] getRunningApplications() {
        List<CompositeData> compositeDataList = new ArrayList<>();

        try {
            for (ApplicationInfo info : applications) {
                Object[] itemValues = new Object[]{
                        info.getName()
                        , info.getContextRoot()
                        , info.getSpecifications().stream().map(Enum::name).collect(Collectors.joining(","))
                };
                CompositeDataSupport support = new CompositeDataSupport(JMX_COMPOSITE_TYPE, JMX_ATTRIBUTE_NAMES,
                        itemValues);
                compositeDataList.add(support);
            }
        } catch (OpenDataException e) {
            throw new UnexpectedException(UnexpectedException.UnexpectedExceptionCode.valueOf("XX"), new MBeanException(e, "Error occurred when getting customer information via JMX"));
        }
        return compositeDataList.toArray(new CompositeData[0]);
    }

    public void registerApplication(AbstractDeployment deployment) {
        applications.add(ApplicationInfo.createFor(deployment));
    }

    public void unregisterApplication(ArchiveDeployment deployment) {
        applications.remove(ApplicationInfo.createFor(deployment));
    }
}
