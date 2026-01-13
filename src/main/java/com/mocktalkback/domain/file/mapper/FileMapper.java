package com.mocktalkback.domain.file.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.mocktalkback.domain.file.dto.FileClassCreateRequest;
import com.mocktalkback.domain.file.dto.FileClassResponse;
import com.mocktalkback.domain.file.dto.FileCreateRequest;
import com.mocktalkback.domain.file.dto.FileResponse;
import com.mocktalkback.domain.file.entity.FileClassEntity;
import com.mocktalkback.domain.file.entity.FileEntity;
import com.mocktalkback.global.config.MapstructConfig;

@Mapper(config = MapstructConfig.class)
public interface FileMapper {

    @Mapping(target = "fileClassId", source = "fileClass.id")
    FileResponse toResponse(FileEntity entity);

    FileClassResponse toResponse(FileClassEntity entity);

    default FileEntity toEntity(FileCreateRequest request, FileClassEntity fileClass) {
        return FileEntity.builder()
            .fileClass(fileClass)
            .fileName(request.fileName())
            .storageKey(request.storageKey())
            .fileSize(request.fileSize())
            .mimeType(request.mimeType())
            .build();
    }

    default FileClassEntity toEntity(FileClassCreateRequest request) {
        return FileClassEntity.builder()
            .code(request.code())
            .name(request.name())
            .description(request.description())
            .mediaKind(request.mediaKind())
            .build();
    }
}
