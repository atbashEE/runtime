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
package be.atbash.runtime.logging.handler.formatter;

import be.atbash.runtime.logging.util.LogUtil;

import java.util.HashMap;
import java.util.Optional;
import java.util.logging.Level;

/**
 * Inspired by code of Payara.
 */
public abstract class AnsiColorFormatter extends CommonFormatter {

    private final boolean ansiColor;
    private final HashMap<Level, AnsiColor> colors = new HashMap<>();
    private AnsiColor loggerColor = AnsiColor.NOTHING;

    public AnsiColorFormatter(String excludeFields) {
        super(excludeFields);

        String formatterClassName = this.getClass().getCanonicalName();
        Optional<String> colorActive = LogUtil.getStringProperty(formatterClassName + ".ansiColor");

        ansiColor = colorActive.map(Boolean::parseBoolean).orElse(Boolean.FALSE);
        if (!ansiColor) {
            // No ansiColoring so no need to continue.
            return;
        }

        colors.put(Level.INFO, AnsiColor.BOLD_INTENSE_GREEN);
        colors.put(Level.WARNING, AnsiColor.BOLD_INTENSE_YELLOW);
        colors.put(Level.SEVERE, AnsiColor.BOLD_INTENSE_RED);
        loggerColor = AnsiColor.BOLD_INTENSE_BLUE;

        Optional<String> infoColor = LogUtil.getStringProperty(formatterClassName + ".infoColor");
        infoColor.ifPresent(s -> colors.put(Level.INFO, AnsiColor.parse(s).orElse(AnsiColor.BOLD_INTENSE_GREEN)));

        Optional<String> warnColor = LogUtil.getStringProperty(formatterClassName + ".warnColor");
        warnColor.ifPresent(s -> colors.put(Level.WARNING, AnsiColor.parse(s).orElse(AnsiColor.BOLD_INTENSE_YELLOW)));

        Optional<String> severeColor = LogUtil.getStringProperty(formatterClassName + ".severeColor");
        severeColor.ifPresent(s -> colors.put(Level.SEVERE, AnsiColor.parse(s).orElse(AnsiColor.BOLD_INTENSE_RED)));

        Optional<String> loggerColorValue = LogUtil.getStringProperty(formatterClassName + ".loggerColor");
        loggerColorValue.ifPresent(s -> loggerColor = AnsiColor.parse(s).orElse(AnsiColor.BOLD_INTENSE_BLUE));

    }

    public AnsiColor getLoggerColor() {
        return loggerColor;
    }

    protected boolean color() {
        return ansiColor;
    }

    protected AnsiColor getColor(Level level) {
        AnsiColor result = colors.get(level);
        if (result == null) {
            result = AnsiColor.NOTHING;
        }
        return result;
    }

    protected AnsiColor getReset() {
        return AnsiColor.RESET;
    }

}
