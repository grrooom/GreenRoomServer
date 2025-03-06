package com.greenroom.server.api.domain.greenroom.service;

import com.greenroom.server.api.domain.greenroom.dto.out.ItemResponseDTO;
import com.greenroom.server.api.domain.greenroom.entity.Item;
import com.greenroom.server.api.domain.greenroom.enums.ItemType;
import com.greenroom.server.api.domain.greenroom.repository.ItemRepository;

import com.greenroom.server.api.domain.user.entity.User;
import com.greenroom.server.api.global.response.enums.ResponseCodeEnum;
import com.greenroom.server.api.global.exception.CustomException;
import com.greenroom.server.api.security.service.CustomUserDetailService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
@Slf4j
@RequiredArgsConstructor
public class ItemService {

    //repository
    private final ItemRepository itemRepository;

    //service
    private final CustomUserDetailService customUserDetailService;

    public Item findItemById(Long itemId){
        return itemRepository.findById(itemId).orElseThrow(()->new CustomException(ResponseCodeEnum.ITEM_NOT_FOUND));
    }

    public List<ItemResponseDTO> getItems(String userEmail ,Integer category, Integer subCategory){

        User user = customUserDetailService.findUserByEmail(userEmail);

        int userLevel = user.getGrade().getLevel();

        return  itemRepository.findAll()
                .stream()
                .filter(item -> category==null  || Objects.equals(item.getItemType().getId(), category))
                .filter(item -> subCategory==null|| Objects.equals(item.getItemDetailType().getId(), subCategory))
                .map(item-> ItemResponseDTO.of(item, item.getGrade().getLevel()<=userLevel)).toList();
    }

}
