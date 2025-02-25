package com.greenroom.server.api.domain.greenroom.service;

import com.greenroom.server.api.domain.greenroom.entity.Item;
import com.greenroom.server.api.domain.greenroom.repository.ItemRepository;
import com.greenroom.server.api.global.response.enums.ResponseCodeEnum;
import com.greenroom.server.api.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class ItemService {

    private final ItemRepository itemRepository;

    public Item findItemById(Long itemId){
        return itemRepository.findById(itemId).orElseThrow(()->new CustomException(ResponseCodeEnum.ITEM_NOT_FOUND));
    }

}
