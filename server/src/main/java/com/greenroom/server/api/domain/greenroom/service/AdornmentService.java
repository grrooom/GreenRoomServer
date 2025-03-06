package com.greenroom.server.api.domain.greenroom.service;

import com.greenroom.server.api.domain.greenroom.dto.out.GreenroomInfoResponseDto;
import com.greenroom.server.api.domain.greenroom.entity.Adornment;
import com.greenroom.server.api.domain.greenroom.entity.GreenRoom;
import com.greenroom.server.api.domain.greenroom.entity.Item;
import com.greenroom.server.api.domain.greenroom.enums.ItemType;
import com.greenroom.server.api.domain.greenroom.repository.AdornmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class AdornmentService {

    //repository
    private final AdornmentRepository adornmentRepository;

    //service
    private final ItemService itemService;
    private final List<String> itemCatergoryList = List.of("shape","hair_accessory","eyewear","window_stuff","shelf_stuff");

    public Map<String,GreenroomInfoResponseDto.ItemSimpleDto> getGreenroomAdornmentInfo(GreenRoom greenRoom){
        List<Adornment> adornmentList =  adornmentRepository.findAllByGreenRoom(greenRoom);

        Map<String, GreenroomInfoResponseDto.ItemSimpleDto> itemMap = new HashMap<>();

        itemCatergoryList.forEach(it->itemMap.put(it,null));

        adornmentList.forEach(a-> itemMap.put(a.getItem().getItemType().equals(ItemType.SHAPE)?"shape":a.getItem().getItemDetailType().name().toLowerCase(), GreenroomInfoResponseDto.ItemSimpleDto.from(a.getItem())));

        return itemMap;
    }

    public void createAdornment(GreenRoom greenRoom, Long itemId){
        Item item = itemService.findItemById(itemId);
        Adornment adornment =  Adornment.builder().greenRoom(greenRoom).item(item).build();
        adornmentRepository.save(adornment);
    }


}
