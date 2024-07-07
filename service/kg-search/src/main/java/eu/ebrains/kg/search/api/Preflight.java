/*
 *  Copyright 2018 - 2021 Swiss Federal Institute of Technology Lausanne (EPFL)
 *  Copyright 2021 - 2023 EBRAINS AISBL
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0.
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  This open source software code was developed in part or in whole in the
 *  Human Brain Project, funded from the European Union's Horizon 2020
 *  Framework Programme for Research and Innovation under
 *  Specific Grant Agreements No. 720270, No. 785907, and No. 945539
 *  (Human Brain Project SGA1, SGA2 and SGA3).
 */

package eu.ebrains.kg.search.api;


import eu.ebrains.kg.common.services.KGServiceClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;


@RequestMapping(value = "/preflight", produces = MediaType.APPLICATION_JSON_VALUE)
@RestController
public class Preflight {

    @Autowired
    private RestTemplate restTemplate;

    private final KGServiceClient kgServiceClient;

    public Preflight(KGServiceClient kgServiceClient) {
        this.kgServiceClient = kgServiceClient;
    }

    @CrossOrigin(origins = "*")
    @GetMapping
    @SuppressWarnings("java:S1452") // we keep the generics intentionally
    public ResponseEntity<?> checkAuthEndpoint() {
        String authUrl = this.kgServiceClient.getAuthEndpoint();

        try {
            // Send a GET request to the authentication endpoint
            ResponseEntity<String> response = restTemplate.getForEntity("https://iam-int.ebrains.eu/auth", String.class);

            // If the response status is 2xx, the endpoint exists
            if (response.getStatusCode().is2xxSuccessful()) {
                return new ResponseEntity<>("Authentication endpoint is reachable.", HttpStatus.OK);
            } else {
                return new ResponseEntity<>("Authentication endpoint is not reachable.", HttpStatus.SERVICE_UNAVAILABLE);
            }
        } catch (HttpClientErrorException e) {
            // Handle 4xx and 5xx HTTP errors
            return new ResponseEntity<>("Authentication endpoint is not reachable.", HttpStatus.SERVICE_UNAVAILABLE);
        } catch (Exception e) {
            // Handle other errors (e.g., network issues)
            return new ResponseEntity<>("Error checking authentication endpoint.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }



}
