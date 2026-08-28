package com.example.shipping.api;

import com.example.shipping.domain.OrderRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"})
public class DemoController {
    private final DemoService service;

    public DemoController(DemoService service) {
        this.service = service;
    }

    @GetMapping("/demo")
    DemoSnapshot snapshot() {
        return service.snapshot();
    }

    @PostMapping("/orders")
    @ResponseStatus(HttpStatus.ACCEPTED)
    OrderSubmission submit(@RequestBody OrderRequest request) {
        return service.submitOrder(request);
    }

    @PostMapping("/items/{workflowId}/approve")
    @ResponseStatus(HttpStatus.ACCEPTED)
    void approve(@PathVariable String workflowId) {
        service.approve(workflowId);
    }
}
