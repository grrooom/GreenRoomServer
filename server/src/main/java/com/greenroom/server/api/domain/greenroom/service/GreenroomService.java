package com.greenroom.server.api.domain.greenroom.service;

import com.greenroom.server.api.domain.greenroom.dto.in.*;
import com.greenroom.server.api.domain.greenroom.dto.out.*;
import com.greenroom.server.api.domain.greenroom.entity.*;
import com.greenroom.server.api.domain.greenroom.enums.GreenRoomStatus;
import com.greenroom.server.api.domain.greenroom.repository.GreenRoomRepository;
import com.greenroom.server.api.domain.user.entity.User;
import com.greenroom.server.api.domain.user.service.GradeService;
import com.greenroom.server.api.global.response.enums.ResponseCodeEnum;
import com.greenroom.server.api.global.exception.CustomException;
import com.greenroom.server.api.security.service.CustomUserDetailService;
import com.greenroom.server.api.utils.S3ImageUploader;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GreenroomService {

    //repository
    private final GreenRoomRepository greenRoomRepository;

    //service
    private final CustomUserDetailService customUserDetailService;
    private final TodoService todoService;
    private final TodoLogService todoLogService;
    private final AdornmentService adornmentService;
    private final S3ImageUploader s3ImageUploader;
    private final PlantService plantService;
    private final GradeService gradeService;
    private final DiaryService diaryService;
    private final ActivityService activityService;

    //util
    private final GreenroomResponseAssembler greenroomResponseAssembler;

    public GreenRoom findEnabledGreenroomById(Long greenRoomId){
        return greenRoomRepository.findByGreenroomIdAndGreenroomStatus(greenRoomId,GreenRoomStatus.ENABLED).orElseThrow(()->new CustomException(ResponseCodeEnum.GREENROOM_NOT_FOUND));
    }


    public GreenroomInfoResponseDto getGreenroomInfo(String email){

        User user = customUserDetailService.findUserByEmail(email); //없으면 NOT_FOUND 예외 발생

        List<GreenRoom> greenRoomList =  greenRoomRepository.findGreenRoomByUserAndGreenroomStatus(user, GreenRoomStatus.ENABLED);

        if(greenRoomList.isEmpty()){return null;}

        return greenroomResponseAssembler.toGreenroomInfo(greenRoomList.get(0));

    }

    public Boolean checkDuplication(String email, String nickName){
        User user = customUserDetailService.findUserByEmail(email);

        List<GreenRoom> greenRoomList =  greenRoomRepository.findGreenRoomByUserAndGreenroomStatus(user,GreenRoomStatus.ENABLED);

        for(GreenRoom greenroom : greenRoomList){
            if(greenroom.getName().equals(nickName)) return true;
        }
        return false;
    }

    @Transactional
    public PointAndLevelUpResponseDto createGreenroom(String email, GreenroomRegistrationRequestDto greenroomRegistrationRequestDto, MultipartFile imageFile){
        LocalDate wateringBaseTime;
        try{
            wateringBaseTime =LocalDate.parse(greenroomRegistrationRequestDto.getWateringBaseDate());
        }
        catch (DateTimeParseException e){throw new CustomException(ResponseCodeEnum.INVALID_REQUEST_ARGUMENT);}

        // 그린룸 등록
        User user = customUserDetailService.findUserByEmail(email);  // 없으면 not found
        Plant plant = greenroomRegistrationRequestDto.getPlantId()==null?null:plantService.findPlantById(greenroomRegistrationRequestDto.getPlantId());  //없으면 not found
        String imageFileName = imageFile==null ||imageFile.isEmpty()?null:s3ImageUploader.uploadGreenroomImage(imageFile);  //FAIL_TO_UPLOAD_IMAGE , //INVALID_IMAGE_FORMAT
        GreenRoom greenRoom =  GreenRoom.of(greenroomRegistrationRequestDto.getNickname(),imageFileName,user,plant);
        greenRoomRepository.save(greenRoom);

        // 아이템 등록
        adornmentService.createAdornment(greenRoom, greenroomRegistrationRequestDto.getItemId());//없으면 not found

        // 할 일 등록
        todoService.createWateringTodo(greenroomRegistrationRequestDto.getWateringInterval(), greenRoom,wateringBaseTime);

        //첫 등록일 경우
        if(!user.getIsFirstGreenroomRegistered()){
            user.updateIsFirstGreenroomRegistered(true);
            user.addTotalSeed(2);
            return PointAndLevelUpResponseDto.ofFirstGreenroomRegistration(user,2,gradeService.updateUserGrade(user));
        }
        //첫 식물이 아닐 경우
        else{
            return PointAndLevelUpResponseDto.of(user,0, PointAndLevelUpResponseDto.LevelUpStatus.of(user));
        }
    }


    public PointAndLevelUpResponseDto completeTodo(Long greenroomId, CompleteTodoRequestDto completeTodoRequestDto){

        GreenRoom greenRoom = findEnabledGreenroomById(greenroomId); //없으면 not found

        return todoService.completeTodo(greenRoom,completeTodoRequestDto.getCompletedTodo());

    }

    public Map<String,ItemSimpleDto> updateGreenroomAdornment(Long greenroomId, GreenroomDecorationRequestDto greenroomDecorationRequestDto){

        GreenRoom greenRoom = findEnabledGreenroomById(greenroomId); // 없으면 not found

        return adornmentService.updateAdornment(greenRoom, greenroomDecorationRequestDto);

    }

    public GreenroomDetailResponseDto getGreenroomDetails(Long greenroomId){

        GreenRoom greenRoom = findEnabledGreenroomById(greenroomId); // 없으면 not found exception 발생

        return greenroomResponseAssembler.toDetailResponse(greenRoom);
    }

    @Transactional
    public List<String> deleteGreenroom(List<Long> greenroomIdList){

        List<String> imageDeleteList = new ArrayList<>();

        greenRoomRepository.findAllById(greenroomIdList).forEach(greenRoom -> {
            if(greenRoom.getPictureUrl()!=null){imageDeleteList.add(greenRoom.getPictureUrl());}}
        );

        //greenroom 연관 adornment 객체 삭제
        adornmentService.deleteAllByGreenRoom(greenroomIdList);

        //greenroom 연관 diary 객체 삭제  + diary 객체 image 파일 삭제 대상에 포함.
        imageDeleteList.addAll(diaryService.deleteAllByGreenRoom(greenroomIdList));

        // greenroom 연관된 todo_log, todo 삭제
        todoLogService.deleteAllByGreenroom(greenroomIdList);
        todoService.deleteAllByGreenRoom(greenroomIdList);

        // greenroom 삭제
        greenRoomRepository.deleteAllByIdInBatch(greenroomIdList);

        return imageDeleteList;
    }

    @Transactional
    public void deleteAllGreenroomAndDeleteAllImages(List<Long> greenroomIdList){

        s3ImageUploader.deleteImageInBatch(deleteGreenroom(greenroomIdList));
    }

    @Transactional
    public void postMemo(Long greenroomId,MemoRequestDto memoRequestDto){

        if(isOver30Characters(memoRequestDto.memo())){throw new CustomException(ResponseCodeEnum.TOO_LONG_STRING);}

        GreenRoom greenRoom = findEnabledGreenroomById(greenroomId);
        greenRoom.updateMemo(memoRequestDto.memo());
    }

    public boolean isOver30Characters(String str) {
        return str.length() > 30;
    }

    @Transactional
    public GreenroomDetailResponseDto registerGreenroomPlant(Long greenroomId, GreenroomPlantRequestDto greenroomPlantRequestDto){

        GreenRoom greenRoom =  findEnabledGreenroomById(greenroomId); //없으면 not found

        Plant plant ;
        if(greenroomPlantRequestDto.plantId()==null){plant=null;}
        else{plant= plantService.findPlantById(greenroomPlantRequestDto.plantId());}

        greenRoom.updatePlant(plant);

        return getGreenroomDetails(greenroomId);
    }

    @Transactional
    public GreenroomDetailResponseDto updateGreenroomInfo(Long greenroomId, GreenroomInfoUpdateRequestDto greenroomInfoUpdateRequestDto, MultipartFile imageFile){

        GreenRoom  greenRoom = findEnabledGreenroomById(greenroomId); //없으면 not found
        GreenRoomStatus greenRoomStatus = greenroomInfoUpdateRequestDto.isAlive()?GreenRoomStatus.ENABLED:GreenRoomStatus.DISABLED;

        greenRoom.updateName(greenroomInfoUpdateRequestDto.nickname()); //이름 변경
        greenRoom.updateStatus(greenRoomStatus); // 상태 변경

        if(greenroomInfoUpdateRequestDto.imageAction()== GreenroomInfoUpdateRequestDto.ImageAction.DELETE){
            deleteGreenroomImage(greenRoom);
        }
        else if(greenroomInfoUpdateRequestDto.imageAction()== GreenroomInfoUpdateRequestDto.ImageAction.UPLOAD){
            updateGreenroomImage(greenRoom,imageFile);
        }
        return getGreenroomDetails(greenroomId);
    }

    private void deleteGreenroomImage(GreenRoom greenRoom){

        String oldImageUrl = greenRoom.getPictureUrl();
        greenRoom.updatePictureUrl(null);

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    if(oldImageUrl!=null){s3ImageUploader.deleteImage(oldImageUrl);}
                }
            });
        }
    }

    private void updateGreenroomImage(GreenRoom greenRoom, MultipartFile imageFile){

        deleteGreenroomImage(greenRoom);

        if(imageFile==null || imageFile.isEmpty()){return;}

        String imageUrl = s3ImageUploader.uploadGreenroomImage(imageFile);
        greenRoom.updatePictureUrl(imageUrl);

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    if (status == TransactionSynchronization.STATUS_ROLLED_BACK && imageUrl!=null) {
                        s3ImageUploader.deleteImage(imageUrl);
                    }
                }
            });
        }
    }

    public GreenroomTodoCycleResponseDto getGreenroomTodoInfo(Long greenroomId){

        GreenRoom greenRoom = findEnabledGreenroomById(greenroomId); // 없는 경우 not found exception 반환.

        Map<Long,String> activityMap = new HashMap<>();
        activityService.findAllActivity().forEach(activity ->  activityMap.put(activity.getActivityId(),activity.getActivityName()));

        List<Todo> todoList = todoService.findAllEnabledTodoByGreenroom(greenRoom);
        todoList.forEach(todo-> activityMap.remove(todo.getActivity().getActivityId()));

        List<GreenroomTodoCycleResponseDto.TodoSimpleInfo> notUsedActivity =
                activityMap.keySet().stream().map(a-> GreenroomTodoCycleResponseDto.TodoSimpleInfo.of(a,  activityMap.get(a))).toList();

        List<GreenroomTodoCycleResponseDto.TodoCycleInfo> usedActivity =
                todoList.stream().map(GreenroomTodoCycleResponseDto.TodoCycleInfo::from).toList();

        return GreenroomTodoCycleResponseDto.of(usedActivity,notUsedActivity);
    }

    @Transactional
    public GreenroomTodoCycleResponseDto updateActivityStatus(ActivityStatusUpdateRequestDto activityStatusUpdateRequestDto,Long greenroomId){

        GreenRoom greenRoom = findEnabledGreenroomById(greenroomId);

        Set<Long> active = new HashSet<>(activityStatusUpdateRequestDto.activeList());
        Set<Long> inactive = new HashSet<>(activityStatusUpdateRequestDto.inactiveList());

        if(active.stream().anyMatch(inactive::contains)){throw new CustomException(ResponseCodeEnum.INVALID_REQUEST_ARGUMENT);}

        List<Todo> todoList = todoService.findAllByGreenroom(greenRoom);

        // 기존에 존재하던 todo는 update
        updateExistingTodos(todoList,active,inactive);

        // 한번도 활성화된 적 없는 주기는 새로 생성
        todoService.createTodo(greenRoom,new ArrayList<>(active));

        return getGreenroomTodoInfo(greenroomId);

    }

    private void updateExistingTodos(List<Todo> todoList, Set<Long> activeIds, Set<Long> inactiveIds) {

        for (Todo todo : todoList) {
            Long activityId = todo.getActivity().getActivityId();

            if (inactiveIds.contains(activityId)) {
                todo.updateUseYn(false);
            }

            if (activeIds.contains(activityId)) {
                activeIds.remove(activityId);
                todo.updateUseYn(true);
            }
        }
    }

    @Transactional
    public GreenroomTodoCycleResponseDto updateActivity(ActivityInfoUpdateRequestDto activityInfoUpdateRequestDto, Long greenroomId){

        GreenRoom greenRoom = findEnabledGreenroomById(greenroomId);

        Map<Long, ActivityInfoUpdateRequestDto.ActivityInfoUpdateDto> updateMap = new HashMap<>();
        activityInfoUpdateRequestDto.updateList().forEach(a-> updateMap.put(a.activityId(),a));
        Set<Long> updateActivityList= updateMap.keySet();

        todoService.findAllEnabledTodoByGreenroom(greenRoom).forEach(todo->{
            if(updateActivityList.contains(todo.getActivity().getActivityId())){
                updateTodo(todo,updateMap.get(todo.getActivity().getActivityId()));
            }
        });

        return getGreenroomTodoInfo(greenroomId);
    }


    public void updateTodo(Todo todo, ActivityInfoUpdateRequestDto.ActivityInfoUpdateDto updateDto){
        LocalDate baseDate ;

        try{
            baseDate = LocalDate.parse(updateDto.date());
            if(baseDate.isAfter(LocalDate.now())){throw new CustomException(ResponseCodeEnum.INVALID_REQUEST_ARGUMENT); }
        }
        catch (DateTimeParseException e){throw new CustomException(ResponseCodeEnum.INVALID_REQUEST_ARGUMENT);}

        Integer term = updateDto.term();

        todo.updateBaseDate(baseDate);
        todo.updateTerm(term);
        todo.updateNextTodoDate(baseDate.plusDays(term));

    }

    public GreenroomCalendarResponseDto getGreenroomInfoFromCalendar(String email, LocalDate date, Long activityId){

        User user = customUserDetailService.findUserByEmail(email);

        if(date.isBefore(LocalDate.now())){
            return GreenroomCalendarResponseDto.from(date,getGreenroomInfoPast(user,date,activityId));
        }
        else if(date.isEqual(LocalDate.now())){
            return GreenroomCalendarResponseDto.from(date,getGreenroomInfoPresent(user,date,activityId));
        }
        else{
            return GreenroomCalendarResponseDto.from(date,getGreenroomInfoFuture(user,date,activityId));
        }
    }

    private List<GreenRoom> getEnabledGreenRooms(User user) {
        return greenRoomRepository.findGreenRoomByUserAndGreenroomStatus(user, GreenRoomStatus.ENABLED);
    }

    private List<GreenRoom> getAllGreenRooms(User user) {
        return greenRoomRepository.findGreenRoomByUser(user);
    }

    private Map<GreenRoom, List<TodoLog>> getTodoLogs(List<GreenRoom> rooms, LocalDate date, Long activityId) {
        return todoLogService.getAllTodoLogByGreenroomAndDate(rooms, date).stream()
                .filter(log -> activityId == null || Objects.equals(log.getActivity().getActivityId(), activityId))
                .collect(Collectors.groupingBy(TodoLog::getGreenRoom));
    }

    private Map<GreenRoom, List<Todo>> getTodos(List<GreenRoom> rooms, LocalDate date, Long activityId) {
        return todoService.getAllTodoByGreenroomAndDate(rooms, date).stream()
                .filter(todo -> activityId == null || Objects.equals(todo.getActivity().getActivityId(), activityId))
                .collect(Collectors.groupingBy(Todo::getGreenRoom));
    }

    private Map<GreenRoom, List<Diary>> getDiaries(List<GreenRoom> rooms, LocalDate date) {
        return diaryService.getAllDiariesByGreenroomAndDate(rooms, date).stream()
                .collect(Collectors.groupingBy(Diary::getGreenRoom));
    }

    private GreenroomCalendarResponseDto.CalendarInfo buildCalendarInfo(GreenRoom greenRoom, List<TodoLog> todoLogs, List<Todo> todos, List<Diary> diaries) {

        List<GreenroomCalendarResponseDto.TodoInfo> todoInfos = new ArrayList<>();
        if (todoLogs != null) todoInfos.addAll(todoLogs.stream().map(GreenroomCalendarResponseDto.TodoInfo::from).toList());
        if (todos != null) todoInfos.addAll(todos.stream().map(GreenroomCalendarResponseDto.TodoInfo::from).toList());

        List<GreenroomCalendarResponseDto.DiaryInfo> diaryInfos = diaries != null ? diaries.stream().map(GreenroomCalendarResponseDto.DiaryInfo::from).toList() : new ArrayList<>();

        GreenroomCalendarResponseDto.GreenroomInfo greenroomInfo = GreenroomCalendarResponseDto.GreenroomInfo.from(greenRoom);
        return GreenroomCalendarResponseDto.CalendarInfo.of(greenroomInfo, todoInfos, diaryInfos);
    }

    public List<GreenroomCalendarResponseDto.CalendarInfo> getGreenroomInfoPast(User user, LocalDate date, Long activityId){
        List<GreenRoom> greenRoomListForTodoLogAndDiary = getAllGreenRooms(user);
        if(greenRoomListForTodoLogAndDiary.isEmpty()){return List.of();}

        Map<GreenRoom,List<TodoLog>>  todoLogList = getTodoLogs(greenRoomListForTodoLogAndDiary,date,activityId);
        Map<GreenRoom,List<Diary>> diaryList = getDiaries(greenRoomListForTodoLogAndDiary,date);

        Set<GreenRoom> resultGreenroomList = new HashSet<>();
        resultGreenroomList.addAll(todoLogList.keySet());resultGreenroomList.addAll(diaryList.keySet());


        return resultGreenroomList.isEmpty()?List.of():resultGreenroomList.stream()
                .map(greenroom -> buildCalendarInfo(greenroom, todoLogList.getOrDefault(greenroom, null), null, diaryList.getOrDefault(greenroom, null)))
                .toList();

    }
    public List<GreenroomCalendarResponseDto.CalendarInfo> getGreenroomInfoPresent(User user, LocalDate date, Long activityId){
        List<GreenRoom> greenRoomListForTodoLogAndDiary = getAllGreenRooms(user);
        List<GreenRoom> greenRoomListForTodo = getEnabledGreenRooms(user);

        if(greenRoomListForTodoLogAndDiary.isEmpty()||greenRoomListForTodo.isEmpty()){return List.of();}

        Map<GreenRoom,List<TodoLog>>  todoLogList = getTodoLogs(greenRoomListForTodoLogAndDiary,date,activityId);
        Map<GreenRoom,List<Todo>> todoList = getTodos(greenRoomListForTodo,date,activityId);
        Map<GreenRoom,List<Diary>> diaryList = getDiaries(greenRoomListForTodoLogAndDiary,date);

        Set<GreenRoom> resultGreenroomList = new HashSet<>();
        resultGreenroomList.addAll(todoList.keySet()); resultGreenroomList.addAll(todoLogList.keySet());resultGreenroomList.addAll(diaryList.keySet());


        return resultGreenroomList.isEmpty()?List.of():resultGreenroomList.stream()
                .map(greenroom -> buildCalendarInfo(greenroom, todoLogList.getOrDefault(greenroom, null), todoList.getOrDefault(greenroom, null) ,diaryList.getOrDefault(greenroom, null)))
                .toList();
    }

    public List<GreenroomCalendarResponseDto.CalendarInfo> getGreenroomInfoFuture(User user, LocalDate date, Long activityId){
        List<GreenRoom> greenRoomListForTodo = getEnabledGreenRooms(user);
        if(greenRoomListForTodo.isEmpty()){return List.of();}

        Map<GreenRoom,List<Todo>> todoList = getTodos(greenRoomListForTodo,date,activityId);


        return todoList.isEmpty()?List.of():todoList.keySet().stream()
                .map(greenroom -> buildCalendarInfo(greenroom, null, todoList.getOrDefault(greenroom, null), null))
                .toList();
    }

    @Transactional
    public DiaryResponseDto createDiary(String email, DiaryCreationRequestDto request, MultipartFile imageFile){
        User user = customUserDetailService.findUserByEmail(email);
        List<GreenRoom> greenRoomList =  greenRoomRepository.findGreenRoomByUserAndGreenroomStatus(user,GreenRoomStatus.ENABLED);
        if(greenRoomList.isEmpty()){throw new CustomException(ResponseCodeEnum.GREENROOM_NOT_FOUND);} //일기 작성이 가능한 그린룸이 없는 경우

        String imageUrl = null;
        if(imageFile!=null) imageUrl = s3ImageUploader.uploadGreenroomImage(imageFile);

        Diary createdDiary =  diaryService.createDiary(greenRoomList.get(0),request,imageUrl);

        return DiaryResponseDto.from(createdDiary);

    }
}

