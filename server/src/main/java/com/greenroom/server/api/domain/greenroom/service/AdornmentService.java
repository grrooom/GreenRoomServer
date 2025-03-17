package com.greenroom.server.api.domain.greenroom.service;

import com.greenroom.server.api.domain.greenroom.dto.in.GreenroomDecorationRequestDto;
import com.greenroom.server.api.domain.greenroom.dto.out.GreenroomDetailResponseDto;
import com.greenroom.server.api.domain.greenroom.dto.out.GreenroomInfoResponseDto;
import com.greenroom.server.api.domain.greenroom.dto.out.ItemSimpleDto;
import com.greenroom.server.api.domain.greenroom.entity.Adornment;
import com.greenroom.server.api.domain.greenroom.entity.GreenRoom;
import com.greenroom.server.api.domain.greenroom.entity.Item;
import com.greenroom.server.api.domain.greenroom.enums.ItemType;
import com.greenroom.server.api.domain.greenroom.repository.AdornmentRepository;
import com.greenroom.server.api.global.exception.CustomException;
import com.greenroom.server.api.global.response.enums.ResponseCodeEnum;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class AdornmentService {

    //repository
    private final AdornmentRepository adornmentRepository;

    //service
    private final ItemService itemService;

    public static String snakeToCamel(String snakeUpper) {
        return Arrays.stream(snakeUpper.toLowerCase().split("_"))
                .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1))
                .collect(Collectors.joining(""))
                .replaceFirst("^.", Character.toLowerCase(snakeUpper.charAt(0)) + "");
    }

    public Map<String, ItemSimpleDto> getGreenroomFullAdornmentInfo(GreenRoom greenRoom){

        return getGreenroomAdornmentInfo(greenRoom,List.of("shape","hairAccessory","eyewear","window","shelf"));
    }

    public Map<String, ItemSimpleDto> getGreenroomSimpleAdornmentInfo(GreenRoom greenRoom){

        return getGreenroomAdornmentInfo(greenRoom,List.of("shape","hairAccessory","eyewear"));
    }

    public Map<String,ItemSimpleDto> getGreenroomAdornmentInfo(GreenRoom greenRoom, List<String> itemCategoryList){

        List<Adornment> adornmentList =  adornmentRepository.findAllByGreenRoom(greenRoom);

        Map<String, ItemSimpleDto> itemMap = new HashMap<>();

        itemCategoryList.forEach(it->itemMap.put(it,null));

        adornmentList.forEach(a-> itemMap.put(a.getItem().getItemType().equals(ItemType.SHAPE)?"shape": snakeToCamel(a.getItem().getItemDetailType().name().toLowerCase()), ItemSimpleDto.from(a.getItem())));

        return itemMap;

    }

    @Transactional
    public void createAdornment(GreenRoom greenRoom, Long itemId){
        Item item = itemService.findItemById(itemId);
        Adornment adornment =  Adornment.createAdornment(item,greenRoom);
        adornmentRepository.save(adornment);
    }


    @Transactional
    public Map<String, ItemSimpleDto> updateAdornment(
            GreenRoom greenRoom, GreenroomDecorationRequestDto greenroomDecorationRequestDto) {

        // 기존 장식 삭제
        adornmentRepository.deleteAllByGreenRoom(greenRoom);

        // 아이템 ID 리스트 추출
        List<Long> itemIdList = extractItemIds(greenroomDecorationRequestDto);

        // 아이템 리스트 조회
        List<Item> itemList = getItemList(itemIdList);

        // 아이템 타입 검증
        validateItemTypes(itemList, itemIdList.size());

        // Adornment 리스트 생성 및 저장
        List<Adornment> adornmentList = createAdornmentList(itemList, greenRoom);
        adornmentRepository.saveAll(adornmentList);

        // 업데이트된 정보 반환
        return getGreenroomFullAdornmentInfo(greenRoom);
    }

    // 아이템 조회 메서드
    private List<Item> getItemList(List<Long> itemIdList) {
        return itemIdList.stream()
                .map(id -> {
                    try {
                        return itemService.findItemById(id);
                    } catch (Exception e) {
                        throw new CustomException(ResponseCodeEnum.ITEM_NOT_FOUND);
                    }
                })
                .toList();
    }

    // 아이템 타입 검증 메서드
    private void validateItemTypes(List<Item> itemList, int expectedSize) {
        long distinctCount = itemList.stream()
                .map(this::extractItemType)
                .distinct()
                .count();
        if (distinctCount != expectedSize) {
            throw new CustomException(ResponseCodeEnum.INVALID_REQUEST_ARGUMENT);
        }
    }

    // 아이템 타입 추출 메서드
    private String extractItemType(Item item) {
        return item.getItemType().equals(ItemType.SHAPE)
                ? ItemType.SHAPE.name()
                : item.getItemDetailType().name();
    }

    // 장식 리스트 생성 메서드
    private List<Adornment> createAdornmentList(List<Item> itemList, GreenRoom greenRoom) {
        return itemList.stream()
                .map(item -> Adornment.createAdornment(item, greenRoom))
                .toList();
    }

    // 아이템 ID 리스트를 추출하는 메소드
    private List<Long> extractItemIds(GreenroomDecorationRequestDto dto) {
        return Stream.of(
                        dto.getShape(),
                        dto.getEyewear(),
                        dto.getHairAccessory(),
                        dto.getWindow(),
                        dto.getShelf())
                .filter(Objects::nonNull)  // null 값 제거
                .toList();
    }
}
