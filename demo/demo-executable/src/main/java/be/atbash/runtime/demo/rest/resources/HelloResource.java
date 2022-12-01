package be.atbash.runtime.demo.rest.resources;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@Path(("/hello"))
@ApplicationScoped
public class HelloResource {

    @GET
    public String helloWorld() {
        return "Hello World";
    }
}
