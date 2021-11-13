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
package be.atbash.runtime.demo.rest.json;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

@Path("/person")
public class PersonResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Person getPerson() {
        return new Person("Rudy", 42);
    }

    @GET
    @Path("jsonp")
    @Produces(MediaType.APPLICATION_JSON)
    public JsonObject getPersonJSONP() {
        JsonObjectBuilder builder = Json.createObjectBuilder();
        builder.add("type", "jsonp");
        builder.add("name", "Rudy");
        builder.add("age", 42);
        return builder.build();
    }

    @POST
    @Produces(MediaType.TEXT_PLAIN)
    @Consumes(MediaType.APPLICATION_JSON)
    public String handlePersonRequest(Person person) {
        return person.toString();
    }
}
