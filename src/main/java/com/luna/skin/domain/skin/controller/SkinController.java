package com.luna.skin.domain.skin.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Skin", description = "skin 관련 API")
@RestController
@RequestMapping("/api/skins")
@RequiredArgsConstructor
public class SkinController {
}
