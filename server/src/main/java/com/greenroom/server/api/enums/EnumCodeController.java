package com.greenroom.server.api.enums;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.servlet.ModelAndView;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class EnumCodeController {

    @GetMapping("/document")
    public String getErrorCodeDocument(Model model){
        List<ResponseCodeResponseDto> responses = Arrays.stream(ResponseCodeEnum.values()).map(ResponseCodeResponseDto::from).toList();

        model.addAttribute("errorGroups", responses);

        return "responseCode";

    }
}
