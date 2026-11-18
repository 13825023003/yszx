package com.yunshu.zhixue.modules.system.service;

import com.yunshu.zhixue.common.exception.BizException;
import com.yunshu.zhixue.common.exception.ErrorCode;
import com.yunshu.zhixue.support.storage.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Set;

/**
 * 管理端图片上传（A-F01）：JPG/PNG，校验真实图片与尺寸。
 */
@Service
@RequiredArgsConstructor
public class ImageUploadService {

    private static final long MAX_IMAGE_SIZE = 2L * 1024 * 1024;
    private static final int MAX_DIMENSION = 4096;
    private static final Set<String> ALLOWED_TYPES = Set.of("image/jpeg", "image/png");

    private final StorageService storageService;

    public String upload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException(ErrorCode.FILE_INVALID, "请选择要上传的图片");
        }
        if (file.getSize() > MAX_IMAGE_SIZE) {
            throw new BizException(ErrorCode.FILE_INVALID, "图片大小不能超过 2MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType.toLowerCase())) {
            throw new BizException(ErrorCode.FILE_INVALID, "仅支持 JPG / PNG 格式");
        }
        try {
            byte[] bytes = file.getBytes();
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
            if (image == null) {
                throw new BizException(ErrorCode.FILE_INVALID, "图片文件无效");
            }
            if (image.getWidth() > MAX_DIMENSION || image.getHeight() > MAX_DIMENSION) {
                throw new BizException(ErrorCode.FILE_INVALID,
                        "图片尺寸不能超过 " + MAX_DIMENSION + "×" + MAX_DIMENSION);
            }
            return storageService.upload(new ByteArrayInputStream(bytes), bytes.length,
                    file.getOriginalFilename(), contentType, "settings");
        } catch (BizException ex) {
            throw ex;
        } catch (IOException ex) {
            throw new BizException(ErrorCode.FILE_INVALID, "图片读取失败", ex);
        }
    }
}
