package com.bank.kb.web;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/template")
public class TemplateController {

    @GetMapping("/download")
    public ResponseEntity<Resource> download() {
        // 放到 src/main/resources/templates/kb_stystem_template.xlsx
        Resource resource = new ClassPathResource("static/kb_stystem_template.xlsx");

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"kb_stystem_template.xlsx\"")
                .body(resource);
    }
}
