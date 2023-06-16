package com.javatechie.orderservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.javatechie.orderservice.common.Payment;
import com.javatechie.orderservice.common.TransactionRequest;
import com.javatechie.orderservice.common.TransactionResponse;
import com.javatechie.orderservice.entity.Order;
import com.javatechie.orderservice.repository.OrderRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
@RefreshScope
public class OrderService {

    @Autowired
    private OrderRepository repository;

    @Autowired
    @Lazy
    private RestTemplate template;

    @Value("${microservice.payment-service.endpoints.endpoint.uri}")
    private String ENDPOINT_URL;

    public TransactionResponse saveOrder(TransactionRequest request) {
        log.info("request is: {}", request);
        String response = "";
        Order order = request.getOrder();
        Payment payment = request.getPayment();
        payment.setOrderId(order.getId());
        payment.setAmount(order.getPrice());

        try {
            log.info("OrderService request: {}", new ObjectMapper().writeValueAsString(request));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        // rest call
        log.info("ENDPOINT_URL: {}", ENDPOINT_URL);
        Payment paymentResponse = template.postForObject(ENDPOINT_URL, payment, Payment.class);
        try {
            log.info("Payment-service response from OrderService Rest call: {}", new ObjectMapper().writeValueAsString(paymentResponse));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        response = paymentResponse.getPaymentStatus().equals("success")?
                "payment processing successful and order placed":
                "there is a failure in payment api, order added to cart";
        repository.save(order);
        return new TransactionResponse(order, paymentResponse.getAmount(), paymentResponse.getTransactionId(), response);
    }


}
