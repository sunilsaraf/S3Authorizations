package com.example.s3auth.controller;

import com.example.s3auth.dto.CreateAccessKeyRequest;
import com.example.s3auth.dto.CreateAccessKeyResponse;
import com.example.s3auth.model.AccessKey;
import com.example.s3auth.service.AccessKeyService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/keys")
public class AccessKeyController {

    private final AccessKeyService service;

    @Autowired
    public AccessKeyController(AccessKeyService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<CreateAccessKeyResponse> create(@Valid @RequestBody CreateAccessKeyRequest req) throws Exception {
        AccessKeyService.CreatedKey created = service.create(req.getUserId(), req.getDescription());
        // Return secret only now
        return ResponseEntity.ok(new CreateAccessKeyResponse(created.accessKeyId, created.secret));
    }

    @GetMapping("/{accessKeyId}")
    public ResponseEntity<?> get(@PathVariable String accessKeyId) {
        return service.get(accessKeyId)
                .map(ak -> {
                    return ResponseEntity.ok(new AccessKeyMetadata(ak.getAccessKeyId(), ak.getUserId(), ak.getDescription(), ak.getCreatedAt().toString(), ak.getStatus()));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<AccessKeyMetadata>> listForUser(@RequestParam("userId") String userId) {
        List<AccessKeyMetadata> list = service.listByUser(userId).stream()
                .map(ak -> new AccessKeyMetadata(ak.getAccessKeyId(), ak.getUserId(), ak.getDescription(), ak.getCreatedAt().toString(), ak.getStatus()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }

    @DeleteMapping("/{accessKeyId}")
    public ResponseEntity<Void> revoke(@PathVariable String accessKeyId) {
        service.revoke(accessKeyId);
        return ResponseEntity.noContent().build();
    }

    public static class AccessKeyMetadata {
        public String accessKeyId;
        public String userId;
        public String description;
        public String createdAt;
        public String status;

        public AccessKeyMetadata(String accessKeyId, String userId, String description, String createdAt, String status) {
            this.accessKeyId = accessKeyId;
            this.userId = userId;
            this.description = description;
            this.createdAt = createdAt;
            this.status = status;
        }
    }
}