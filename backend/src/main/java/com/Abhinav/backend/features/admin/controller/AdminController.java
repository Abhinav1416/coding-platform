package com.Abhinav.backend.features.admin.controller;


import com.Abhinav.backend.features.admin.dto.GrantRequest;
import com.Abhinav.backend.features.admin.dto.GrantScopedRequest;
import com.Abhinav.backend.features.admin.service.AdminService;
import com.Abhinav.backend.features.authentication.dto.Response;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;



@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;



    @PostMapping("/permissions/grant-create")
    public ResponseEntity<Response> grantCreate(@RequestBody GrantRequest request) {
        adminService.grantCreatePermission(request.email());
        return ResponseEntity.ok(new Response("CREATE_PROBLEM permission granted for 30 minutes."));
    }


    @PostMapping("/permissions/grant-update")
    public ResponseEntity<Response> grantUpdate(@RequestBody GrantScopedRequest request) {
        adminService.grantUpdatePermission(request.email(), request.problemId());
        return ResponseEntity.ok(new Response("UPDATE_PROBLEM permission for problem " + request.problemId() + " granted for 30 minutes."));
    }


    @PostMapping("/permissions/grant-delete")
    public ResponseEntity<Response> grantDelete(@RequestBody GrantScopedRequest request) {
        adminService.grantDeletePermission(request.email(), request.problemId());
        return ResponseEntity.ok(new Response("DELETE_PROBLEM permission for problem " + request.problemId() + " granted for 30 minutes."));
    }
}