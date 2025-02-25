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

    public Map<String,GreenroomInfoResponseDto.ItemSimpleDto> getGreenroomAdornmentInfo(GreenRoom greenRoom){
        List<Adornment> adornmentList =  adornmentRepository.findAllByGreenRoom(greenRoom);


        Map<String, GreenroomInfoResponseDto.ItemSimpleDto> itemMap = new HashMap<>();
        Arrays.stream(ItemType.values()).forEach(it->itemMap.put(it.name().toLowerCase(),null));

        adornmentList.forEach(a-> itemMap.put(a.getItem().getItemType().name().toLowerCase(), GreenroomInfoResponseDto.ItemSimpleDto.from(a.getItem())));

        return itemMap;
    }

    public void createAdornment(GreenRoom greenRoom, Long itemId){
        Item item = itemService.findItemById(itemId);
        Adornment adornment =  Adornment.builder().greenRoom(greenRoom).item(item).build();
        adornmentRepository.save(adornment);
    }


}
