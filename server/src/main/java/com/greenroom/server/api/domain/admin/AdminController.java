package com.greenroom.server.api.domain.admin;

import com.greenroom.server.api.utils.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin")
public class AdminController {

    private final AdminService adminService;

    @DeleteMapping("/data")
    public ResponseEntity<ApiResponse> deleteAllData(){
        adminService.deleteAllData();
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body(null);
    }

}
