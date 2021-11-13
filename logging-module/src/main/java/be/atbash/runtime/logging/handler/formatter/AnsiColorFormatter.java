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

import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.LogManager;

/**
 *
 *  Inspired by code of Payara.
 */
public abstract class AnsiColorFormatter extends CommonFormatter {
    
    private boolean ansiColor;
    private HashMap<Level,AnsiColor> colors;
    private AnsiColor loggerColor;
    
    public AnsiColorFormatter(String excludeFields) {
        super(excludeFields);
        LogManager manager = LogManager.getLogManager();
        String color = manager.getProperty(this.getClass().getCanonicalName() + ".ansiColor");
        if ("true".equals(color)) {
            ansiColor = true;
        }
        colors = new HashMap<>();
        colors.put(Level.INFO, AnsiColor.BOLD_INTENSE_GREEN);
        colors.put(Level.WARNING, AnsiColor.BOLD_INTENSE_YELLOW);
        colors.put(Level.SEVERE, AnsiColor.BOLD_INTENSE_RED);
        loggerColor = AnsiColor.BOLD_INTENSE_BLUE;
        String infoColor = manager.getProperty(this.getClass().getCanonicalName()+".infoColor");
        if (infoColor != null) {
            try {
                colors.put(Level.INFO, AnsiColor.valueOf(infoColor));
            }catch (IllegalArgumentException iae) {
                colors.put(Level.INFO, AnsiColor.BOLD_INTENSE_GREEN);
            }
        }
        String colorProp = manager.getProperty(this.getClass().getCanonicalName()+".warnColor");
        if (colorProp != null) {
            try {
                colors.put(Level.WARNING, AnsiColor.valueOf(colorProp));
            }catch (IllegalArgumentException iae) {
                colors.put(Level.WARNING, AnsiColor.BOLD_INTENSE_YELLOW);
            }
        }
        colorProp = manager.getProperty(this.getClass().getCanonicalName()+".severeColor");
        if (colorProp != null) {
            try {
                colors.put(Level.SEVERE, AnsiColor.valueOf(colorProp));
            }catch (IllegalArgumentException iae) {
                colors.put(Level.SEVERE, AnsiColor.BOLD_INTENSE_RED);
            }
        }
        
        colorProp = manager.getProperty(this.getClass().getCanonicalName()+".loggerColor");
        if (colorProp != null) {
            try {
                loggerColor = AnsiColor.valueOf(colorProp);
            }catch (IllegalArgumentException iae) {
                loggerColor = AnsiColor.BOLD_INTENSE_BLUE;
            }
        }
        
    }

    public AnsiColor getLoggerColor() {
        return loggerColor;
    }
    
    
    
    protected boolean color() {
        return ansiColor;
    }
    
    public void noAnsi(){
        ansiColor = false;
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
