package com.greenroom.server.api.domain.greenroom.controller;

import com.greenroom.server.api.domain.greenroom.service.ItemService;
import com.greenroom.server.api.global.response.ApiResponse;
import com.greenroom.server.api.global.response.enums.ResponseCodeEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("api/items")
@RestController
@RequiredArgsConstructor
public class ItemController {

    private final ItemService itemService;

    @GetMapping()
    public ResponseEntity<ApiResponse> getItems(@AuthenticationPrincipal User user, @RequestParam(value = "category",required = false,defaultValue = "-1") Integer category, @RequestParam(value = "subcategory",required = false,defaultValue = "-1") Integer subCategory) {
        return ResponseEntity.ok(ApiResponse.success(ResponseCodeEnum.SUCCESS,itemService.getItems(user.getUsername(),category,subCategory)));
    }

}
