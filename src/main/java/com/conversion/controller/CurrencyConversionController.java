package com.conversion.controller;

import com.conversion.model.ConversionModel;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixProperty;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@RestController
@Slf4j
@RequiredArgsConstructor
public class CurrencyConversionController {

    private final RestTemplate restTemplate;

    @GetMapping(
            "/currency-converter/fromCurrency/{fromCurrency}/toCurrency/{toCurrency}/quantity/{quantity}")
    @HystrixCommand(fallbackMethod = "fallback", commandProperties = {
            @HystrixProperty(name = "execution.isolation.thread.timeoutInMilliseconds", value = "10000"),
            @HystrixProperty(name = "circuitBreaker.errorThresholdPercentage", value = "50"),
            @HystrixProperty(name = "circuitBreaker.requestVolumeThreshold", value = "10") })
    public ConversionModel convertCurrency(@PathVariable String fromCurrency,
                    @PathVariable String toCurrency,
                    @PathVariable BigDecimal quantity) {

        Map<String, String> uriVariables = new HashMap<>();
        uriVariables.put("fromCurrency", fromCurrency);
        uriVariables.put("toCurrency", toCurrency);
        log.info("uriVariables: {}", uriVariables);

        ResponseEntity<ConversionModel> responseEntity = restTemplate.getForEntity(
                "http://localhost:8081/currency-exchange/fromCurrency/{fromCurrency}/toCurrency/{toCurrency}",
                ConversionModel.class,
                uriVariables);

        ConversionModel response = responseEntity.getBody();
        log.info("response: {}", response);
        if(response != null)
            return new ConversionModel(
                    response.getId(),
                    fromCurrency,
                    toCurrency,
                    response.getConversionMultiple(),
                    quantity,
                    quantity.multiply(response.getConversionMultiple()),
                    response.getPort());

        return new ConversionModel();
    }

    public ConversionModel fallback(String fromCurrency, String toCurrency, BigDecimal quantity) {

        ConversionModel conversionModel = new ConversionModel();
        conversionModel.setId(101L);
        conversionModel.setFrom("fallback-" + fromCurrency);
        conversionModel.setTo("fallback-" + toCurrency);
        conversionModel.setConversionMultiple(BigDecimal.valueOf(82));
        conversionModel.setQuantity(quantity);
        conversionModel.setTotalCalculatedAmount(BigDecimal.valueOf(82000));
        conversionModel.setPort(8081);
        /*return new ConversionModel(10001L, fromCurrency, toCurrency,
                BigDecimal.valueOf(82),
                quantity,
                BigDecimal.valueOf(82000), 8081);*/
        return conversionModel;
    }
}
