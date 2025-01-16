package com.greenroom.server.api.enums;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Arrays;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class ResponseCodeController {

    @GetMapping("docs/error")
    public String getErrorCodeDocument(Model model){
        List<ResponseCodeEnumDto> responses = Arrays.stream(ResponseCodeEnum.values()).map(ResponseCodeEnumDto::from).toList();

        model.addAttribute("errorGroups", responses);

        return "docs/errorCodeDocs";
    }

}