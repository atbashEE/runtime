/*
 * Copyright 2021 Rudy De Busscher (https://www.atbash.be)
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
package be.atbash.runtime.logging.handler.formatter;

import be.atbash.runtime.core.data.version.VersionInfo;

import java.util.logging.Formatter;

/**
 * A Parent Formatter supporting the Exclude Fields Support
 * and providing the logic for the Product version value.
 *  Inspired by code of Payara
 */
public abstract class CommonFormatter extends Formatter {

    private final ExcludeFieldsSupport excludeFieldsSupport;

    private String productId;

    protected CommonFormatter(String excludeFields) {
        super();
        this.excludeFieldsSupport = new ExcludeFieldsSupport(excludeFields);
        productId = VersionInfo.getInstance().getReleaseVersion();
    }

    protected boolean isFieldExcluded(ExcludeFieldsSupport.SupplementalAttribute excludeField) {
        return excludeFieldsSupport.isSet(excludeField);
    }

    /**
     * Payara can override this to specify product version.
     *
     * @return The string value of the product id.
     */
    protected String getProductId() {
        return productId;
    }

}
