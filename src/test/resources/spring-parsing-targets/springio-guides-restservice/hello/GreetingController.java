package hello;

import java.util.concurrent.atomic.AtomicLong;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Taken from https://spring.io/guides/gs/rest-service/
 */
@RestController
public class GreetingController {

    private static final String template = "Hello, %s!";
    private final AtomicLong counter = new AtomicLong();

    @RequestMapping("/greeting")
    public Greeting greeting(
            @RequestParam(value="name", defaultValue="World") String name
            //3 ,@RequestParam(value="family") String family
    ) {
        return new Greeting(counter.incrementAndGet(),
                String.format(template, name));
    }

    public notAnnotated() {

    }

    // These lines get uncommented in some tests for contract evolution
//2    @RequestMapping("/greeting2")
//2    public Greeting greeting2(@RequestParam(value="name", defaultValue="World") String name) {
//2        return new Greeting(counter.incrementAndGet(),
//2                String.format(template, name));
//2    }
}