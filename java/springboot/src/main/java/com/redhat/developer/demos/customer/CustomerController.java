package com.redhat.developer.demos.customer;

import io.opentracing.Tracer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletRequest;
import java.util.Enumeration;
import java.util.Random;

@RestController
public class CustomerController {

    private static final String RESPONSE_STRING_FORMAT = "customer => %s\n";

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private final RestTemplate restTemplate;

    private static final String[] names = {"Harry Potter", "Hermione Granger", "Lord Voldemort", "Draco Malfoy", "Ron Weasley",
            "Severus Snape", "Sirius Black", "Albus Dumbledore", "Rubeus Hagrid", "Ginny Weasley"};
    private static final String[] addresses = {"1800 Sunset Bvd, Los Angeles", "200 5h Ave, New York City", "1600 Pennsylvania Ave NW, Washington DC"};

    @Value("${preferences.api.url:http://preference:8080}")
    private String remoteURL;

    @Autowired
    private Tracer tracer;

    public CustomerController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @RequestMapping(value = "/customer",  method = RequestMethod.GET)
    public ResponseEntity<Customer> getCustomer(HttpServletRequest httpServletRequest, @RequestHeader("User-Agent") String userAgent, @RequestHeader(value = "user-preference", required = false) String userPreference,
                                              HttpServletRequest request) throws Exception {
        try {

            /**
             * Set baggage
             */
//            String header = request.getHeader("x-api-key");
//            if(header == null){
//                throw new Exception("There is no x-api-key");
//            }

//            tracer.activeSpan().setBaggageItem("user-agent", userAgent);
//            if (userPreference != null && !userPreference.isEmpty()) {
//                tracer.activeSpan().setBaggageItem("user-preference", userPreference);
//            }
            MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
            String header = httpServletRequest.getHeader("x-api-key");
            headers.add("x-api-key", header);
            ResponseEntity<String> entity = restTemplate.exchange(
                    remoteURL, HttpMethod.GET, new HttpEntity<>(headers),
                    String.class);

            String response = entity.getBody();

            ResponseEntity<Preference> responseEntity = restTemplate.getForEntity(remoteURL, Preference.class);
            Preference preferenceResponse = responseEntity.getBody();
            Customer customer = new Customer();

            Random rand = new Random();
            Integer id = rand.nextInt(1000000);
            customer.setId(id);
            String name = names[id % 10];
            customer.setName(name);
            String address = addresses[id % 3];
            customer.setAddress(address);
            customer.setPreference(preferenceResponse);

            return ResponseEntity.ok(customer);
        } catch (HttpStatusCodeException ex) {
            logger.warn("Exception trying to get the response from preference service.", ex);
            return new ResponseEntity(HttpStatus.BAD_REQUEST);
        } catch (RestClientException ex) {
            logger.warn("Exception trying to get the response from preference service.", ex);
            return new ResponseEntity(HttpStatus.BAD_REQUEST);
        }
    }

    private String createHttpErrorResponseString(HttpStatusCodeException ex) {
        String responseBody = ex.getResponseBodyAsString().trim();
        if (responseBody.startsWith("null")) {
            return ex.getStatusCode().getReasonPhrase();
        }
        return responseBody;
    }
//    @PostConstruct
//    public void addInterceptors() {
//        List<ClientHttpRequestInterceptor> interceptors = restTemplate.getInterceptors();
//        interceptors.add(new RestTemplateInterceptor());
//        restTemplate.setInterceptors(interceptors);
//    }
}
