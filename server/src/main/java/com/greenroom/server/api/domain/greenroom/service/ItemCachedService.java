package com.greenroom.server.api.domain.greenroom.service;

import com.greenroom.server.api.domain.greenroom.entity.Item;
import com.greenroom.server.api.domain.greenroom.repository.ItemRepository;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ItemCachedService {

    //repository
    private final ItemRepository itemRepository;

    @Cacheable(cacheNames = {"items"},key = "#category+'_'+#subCategory")
    public List<Item> getItems(Integer category, Integer subCategory){
        return(itemRepository.findAll()
                .stream()
                .filter(item -> category==-1  || Objects.equals(item.getItemType().getId(), category))
                .filter(item -> subCategory==-1|| Objects.equals(item.getItemDetailType().getId(), subCategory))
                .toList());
    }

}
