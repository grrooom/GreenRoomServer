package com.greenroom.server.api.domain.greenroom.service;

import com.greenroom.server.api.domain.greenroom.dto.out.ItemResponseDto;
import com.greenroom.server.api.domain.greenroom.entity.Item;
import com.greenroom.server.api.domain.greenroom.repository.ItemRepository;

import com.greenroom.server.api.global.response.enums.ResponseCodeEnum;
import com.greenroom.server.api.global.exception.CustomException;

import com.greenroom.server.api.domain.user.entity.User;
import com.greenroom.server.api.security.service.CustomUserDetailService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ItemService {

    //repository
    private final ItemRepository itemRepository;

    //service
    private final CustomUserDetailService customUserDetailService;
    private final ItemCachedService itemCachedService;

    public Item findItemById(Long itemId){
        return itemRepository.findById(itemId).orElseThrow(()->new CustomException(ResponseCodeEnum.ITEM_NOT_FOUND));
    }

    public List<ItemResponseDto> getItems(String userEmail , Integer category, Integer subCategory){

        User user = customUserDetailService.findUserByEmail(userEmail);

        int userLevel = user.getGrade().getLevel();
        return itemCachedService.getItems(category,subCategory)
                .stream()
                .map(item-> ItemResponseDto.of(item, item.getGrade().getLevel()<=userLevel)).toList();

    }
}
