package com.greenroom.server.api.domain.greenroom.service;

import com.greenroom.server.api.domain.greenroom.dto.GreenroomInfoResponseDto;
import com.greenroom.server.api.domain.greenroom.entity.Adornment;
import com.greenroom.server.api.domain.greenroom.entity.GreenRoom;
import com.greenroom.server.api.domain.greenroom.enums.ItemType;
import com.greenroom.server.api.domain.greenroom.repository.AdornmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class AdornmentService {

    private final AdornmentRepository adornmentRepository;

    public Map<String,GreenroomInfoResponseDto.ItemSimpleDto> getGreenroomAdornmentInfo(GreenRoom greenRoom){
        List<Adornment> adornmentList =  adornmentRepository.findAllByGreenRoom(greenRoom);


        Map<String, GreenroomInfoResponseDto.ItemSimpleDto> itemMap = new HashMap<>();
        Arrays.stream(ItemType.values()).forEach(it->itemMap.put(it.name().toLowerCase(),null));

        adornmentList.forEach(a-> itemMap.put(a.getItem().getItemType().name().toLowerCase(), GreenroomInfoResponseDto.ItemSimpleDto.from(a.getItem())));

        return itemMap;
    }


}
