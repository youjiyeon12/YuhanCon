package com.yuhancon.service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

// 포스터 업로드. 응답 형식이 확장자로 정해지므로 이미지 확장자만 받고, 파일 앞부분(매직 바이트)도 확장자와 맞는지 본다.
@Component
public class ImageStorage {

    public static final String WEB_PREFIX = "/uploaded-images/";

    private static final Map<String, String> MAGIC = Map.of(
        ".jpg", "FFD8FF", ".jpeg", "FFD8FF", ".png", "89504E47", ".gif", "47494638", ".webp", "52494646");

    private File dir() {
        return new File(System.getProperty("user.dir"), "uploaded-images");
    }

    // 파일이 없으면 null
    public String save(MultipartFile file) {
        if (file == null || file.isEmpty()) return null;
        String name = file.getOriginalFilename();
        int dot = name == null ? -1 : name.lastIndexOf('.');
        String ext = dot < 0 ? "" : name.substring(dot).toLowerCase(Locale.ROOT);
        String magic = MAGIC.get(ext);
        if (magic == null || !startsWith(file, magic)) {
            throw new UserFacingException("포스터는 이미지 파일(jpg, jpeg, png, gif, webp)만 올릴 수 있습니다.");
        }
        String fileName = System.currentTimeMillis() + "_" + UUID.randomUUID() + ext;
        try {
            dir().mkdirs();
            file.transferTo(new File(dir(), fileName).getAbsoluteFile());
        } catch (IOException e) {
            throw new UserFacingException("이미지를 저장하지 못했습니다. 잠시 후 다시 시도해 주세요.");
        }
        return WEB_PREFIX + fileName;
    }

    public void delete(String webPath) {
        if (webPath == null || !webPath.startsWith(WEB_PREFIX)) return; // 기본 제공 이미지(/images/...)는 지우지 않는다
        String fileName = webPath.substring(WEB_PREFIX.length());
        if (fileName.contains("/") || fileName.contains("\\") || fileName.contains("..")) return;
        new File(dir(), fileName).delete();
    }

    private static boolean startsWith(MultipartFile file, String hex) {
        byte[] head = new byte[hex.length() / 2];
        try (InputStream in = file.getInputStream()) {
            if (in.readNBytes(head, 0, head.length) < head.length) return false;
        } catch (IOException e) {
            return false;
        }
        StringBuilder sb = new StringBuilder();
        for (byte b : head) sb.append(String.format("%02X", b));
        return sb.toString().equals(hex);
    }
}
