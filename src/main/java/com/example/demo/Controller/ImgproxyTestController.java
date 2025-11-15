package com.example.demo.Controller;

import com.example.demo.service.ImgproxyService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ImgproxyTestController {

    private final ImgproxyService imgproxyService;

    public ImgproxyTestController(ImgproxyService imgproxyService) {
        this.imgproxyService = imgproxyService;
    }

    @GetMapping("/test/imgproxy-local")
    public String getSignedImgproxyUrlForLocalTest() {

        String localImageUrl = "http://host.docker.internal:8080/test-image.png";;

        String options = "/rs:fill:200:150/q:70";

        String signedUrl = imgproxyService.generateSignedUrl(localImageUrl, options);

        return "원본 로컬 URL: " + localImageUrl + "\n\n"
                + "처리 옵션: " + options + "\n\n"
                + "✅ 생성된 imgproxy URL:\n" + signedUrl;
    }
}