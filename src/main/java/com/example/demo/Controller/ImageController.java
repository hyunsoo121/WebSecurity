package com.example.demo.Controller;

import com.example.demo.service.ImgproxyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Controller
@Slf4j
@RequiredArgsConstructor
public class ImageController {

    private final S3Client s3Client;
    private final ImgproxyService imgproxyService;

    @Value("${app.s3.bucket}")
    private String bucket;

    @Value("${app.s3.base-url}")
    private String s3BaseUrl; // http://host.docker.internal:4566

    // 메인 페이지: S3에 있는 객체 리스트를 이미지로 보여줌
    @GetMapping("/")
    public String index(Model model) {
        List<String> imageUrls = new ArrayList<>();

        ListObjectsV2Request listReq = ListObjectsV2Request.builder()
                .bucket(bucket)
                .build();
        ListObjectsV2Response listRes = s3Client.listObjectsV2(listReq);

        for (S3Object obj : listRes.contents()) {

            // LocalStack S3의 원본 URL 생성
            String originUrl = String.format("http://localstack:4566/%s/%s", bucket, obj.key());
            log.info("ORIGIN S3 URL = {}", originUrl);

            // 이미지 처리 옵션 (300x200으로 맞춤)
            String options = "/rs:fit:300:200/q:85";

            // 🔒 ImgproxyService를 사용하여 보안 서명된 URL 생성
            String proxyUrl = imgproxyService.generateSignedUrl(originUrl, options);

            imageUrls.add(proxyUrl);
        }

        model.addAttribute("images", imageUrls);

        return "index";
    }

    // 이미지 업로드 처리 → S3에 업로드 (수정 없음)
    @PostMapping("/upload")
    public String upload(@RequestParam("file") MultipartFile file,
                         RedirectAttributes redirectAttributes) throws IOException {

        if (file.isEmpty()) {
            redirectAttributes.addFlashAttribute("message", "파일을 선택하세요.");
            return "redirect:/";
        }

        String originalName = file.getOriginalFilename();
        String key = UUID.randomUUID() + "_" + originalName;

        // S3 업로드 요청
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(file.getContentType())
                .build();

        s3Client.putObject(
                putObjectRequest,
                RequestBody.fromBytes(file.getBytes())
        );
        log.info("File uploaded to S3: {}", key);

        redirectAttributes.addFlashAttribute("message", "업로드 완료!");

        return "redirect:/";
    }
}