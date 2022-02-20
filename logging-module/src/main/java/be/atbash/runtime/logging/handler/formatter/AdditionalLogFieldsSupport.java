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
// Portions Copyright [2021] [Payara Foundation and/or its affiliates]
package be.atbash.runtime.logging.handler.formatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.BitSet;

/**
 * Inspired by code of Glassfish.
 */
public class AdditionalLogFieldsSupport {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdditionalLogFieldsSupport.class);

    public enum SupplementalAttribute {TID, TIME_MILLIS, LEVEL_VALUE}

    private final BitSet excludeSuppAttrsBits = new BitSet();

    public AdditionalLogFieldsSupport(String excludeFields) {
        setExcludeFields(excludeFields);
    }

    private void setExcludeFields(String excludeFields) {
        excludeSuppAttrsBits.clear();
        if (excludeFields != null) {
            String[] fields = excludeFields.split(",");
            for (String field : fields) {
                if (field.isBlank()) {
                    continue;
                }
                switch (field) {
                    case "tid":
                        excludeSuppAttrsBits.set(SupplementalAttribute.TID.ordinal());
                        break;
                    case "timeMillis":
                        excludeSuppAttrsBits.set(SupplementalAttribute.TIME_MILLIS.ordinal());
                        break;
                    case "levelValue":
                        excludeSuppAttrsBits.set(SupplementalAttribute.LEVEL_VALUE.ordinal());
                        break;
                    default:
                        LOGGER.warn(String.format("LOG-011: Unknown Exclude Field provided : '%s'", field));
                }
            }
        }
    }

    public boolean isSet(SupplementalAttribute attr) {
        return excludeSuppAttrsBits.get(attr.ordinal());
    }

}
