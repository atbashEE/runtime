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

/**
 *
 * Inspired by code of Payara
 */
public enum AnsiColor {
    
    BLACK("\u001B[0;30m"),
    RED("\u001B[0;31m"),
    YELLOW("\u001B[0;33m"),
    BLUE("\u001B[0;34m"),
    PURPLE("\u001B[0;35m"),
    CYAN("\u001B[0;36m"),
    WHITE("\u001B[0;37m"),
    GREEN("\u001B[0;32m"),
    INTENSE_BLACK("\u001B[0;90m"),
    INTENSE_RED("\u001B[0;91m"),
    INTENSE_YELLOW("\u001B[0;93m"),
    INTENSE_BLUE("\u001B[0;94m"),
    INTENSE_PURPLE("\u001B[0;95m"),
    INTENSE_CYAN("\u001B[0;96m"),
    INTENSE_WHITE("\u001B[0;97m"),
    INTENSE_GREEN("\u001B[0;92m"),
    BOLD_INTENSE_BLACK("\u001B[1;90m"),
    BOLD_INTENSE_RED("\u001B[1;91m"),
    BOLD_INTENSE_YELLOW("\u001B[1;93m"),
    BOLD_INTENSE_BLUE("\u001B[1;94m"),
    BOLD_INTENSE_PURPLE("\u001B[1;95m"),
    BOLD_INTENSE_CYAN("\u001B[1;96m"),
    BOLD_INTENSE_WHITE("\u001B[1;97m"),
    BOLD_INTENSE_GREEN("\u001B[1;92m"), 
    RESET("\u001b[0m"), 
    NOTHING("");
    
    AnsiColor(String color) {
        colorString = color;
    }
    
    public String toString() {
        return colorString;
    }
    
    private final String colorString;
    
}
