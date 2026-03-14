package com.mocktalkback.global.common.sanitize;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class HtmlSanitizerTest {

    // 에디터 이미지의 크기 스타일과 데이터 속성은 sanitize 이후에도 유지되어야 한다.
    @Test
    void sanitize_keeps_editor_image_dimensions_and_data_attributes() {
        // Given: 허용된 파일 출처와 에디터 이미지 HTML
        HtmlSanitizerProperties properties = new HtmlSanitizerProperties();
        properties.setAllowedFileOrigins(List.of("https://cdn.mocktalk.site"));
        properties.setAllowedIframePrefixes(List.of("https://www.youtube.com/embed"));

        HtmlSanitizer sanitizer = new HtmlSanitizer(properties);
        sanitizer.initialize();

        String html = """
            <figure data-type="editor-image" data-align="center" data-original-width="1280" data-original-height="720">
              <img
                src="https://cdn.mocktalk.site/uploads/editor/image.png"
                alt="샘플"
                data-align="center"
                data-width="640px"
                data-height="360px"
                data-caption="샘플 캡션"
                data-original-width="1280"
                data-original-height="720"
                width="640"
                height="360"
                style="width: 640px; height: 360px;"
              />
              <figcaption>샘플 캡션</figcaption>
            </figure>
            """;

        // When: HTML sanitize 실행
        String sanitized = sanitizer.sanitize(html);

        // Then: 이미지 크기와 데이터 속성이 유지되어야 한다
        assertThat(sanitized).contains("data-type=\"editor-image\"");
        assertThat(sanitized).contains("data-align=\"center\"");
        assertThat(sanitized).contains("data-width=\"640px\"");
        assertThat(sanitized).contains("data-height=\"360px\"");
        assertThat(sanitized).contains("width=\"640\"");
        assertThat(sanitized).contains("height=\"360\"");
        assertThat(sanitized).contains("style=\"width:640px;height:360px\"");
    }

    // 허용되지 않은 CSS 속성은 제거하고 에디터에 필요한 스타일만 유지되어야 한다.
    @Test
    void sanitize_strips_disallowed_inline_styles() {
        // Given: 허용되지 않은 CSS 속성이 섞인 HTML
        HtmlSanitizerProperties properties = new HtmlSanitizerProperties();
        properties.setAllowedFileOrigins(List.of("https://cdn.mocktalk.site"));
        properties.setAllowedIframePrefixes(List.of("https://www.youtube.com/embed"));

        HtmlSanitizer sanitizer = new HtmlSanitizer(properties);
        sanitizer.initialize();

        String html = """
            <p style="text-align: center; position: fixed; top: 0;">정렬 문단</p>
            <span style="color: #1f2937; font-size: 16px; background-image: url(https://evil.example/x.png);">텍스트</span>
            <img src="https://cdn.mocktalk.site/uploads/editor/image.png" style="width: 640px; height: 360px; position: absolute;" />
            """;

        // When: HTML sanitize 실행
        String sanitized = sanitizer.sanitize(html);

        // Then: 위험한 CSS는 제거되고 필요한 속성만 남아야 한다
        assertThat(sanitized).contains("style=\"text-align:center\"");
        assertThat(sanitized).doesNotContain("position:fixed");
        assertThat(sanitized).doesNotContain("top:0");
        assertThat(sanitized).contains("style=\"color:#1f2937;font-size:16px\"");
        assertThat(sanitized).doesNotContain("background-image");
        assertThat(sanitized).contains("style=\"width:640px;height:360px\"");
        assertThat(sanitized).doesNotContain("position:absolute");
    }
}
