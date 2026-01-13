package com.mocktalkback.domain.file.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mocktalkback.domain.file.dto.FileCreateRequest;
import com.mocktalkback.domain.file.dto.FileResponse;
import com.mocktalkback.domain.file.dto.FileUpdateRequest;
import com.mocktalkback.domain.file.entity.FileClassEntity;
import com.mocktalkback.domain.file.entity.FileEntity;
import com.mocktalkback.domain.file.mapper.FileMapper;
import com.mocktalkback.domain.file.repository.FileClassRepository;
import com.mocktalkback.domain.file.repository.FileRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FileService {

    private final FileRepository fileRepository;
    private final FileClassRepository fileClassRepository;
    private final FileMapper fileMapper;

    @Transactional
    public FileResponse create(FileCreateRequest request) {
        FileClassEntity fileClass = getFileClass(request.fileClassId());
        FileEntity entity = fileMapper.toEntity(request, fileClass);
        FileEntity saved = fileRepository.save(entity);
        return fileMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public FileResponse findById(Long id) {
        FileEntity entity = fileRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("file not found: " + id));
        return fileMapper.toResponse(entity);
    }

    @Transactional(readOnly = true)
    public List<FileResponse> findAll() {
        return fileRepository.findAll().stream()
            .map(fileMapper::toResponse)
            .toList();
    }

    @Transactional
    public FileResponse update(Long id, FileUpdateRequest request) {
        FileEntity entity = fileRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("file not found: " + id));
        FileClassEntity fileClass = getFileClass(request.fileClassId());
        entity.update(fileClass, request.fileName(), request.storageKey(), request.fileSize(), request.mimeType());
        return fileMapper.toResponse(entity);
    }

    @Transactional
    public void delete(Long id) {
        fileRepository.deleteById(id);
    }

    private FileClassEntity getFileClass(Long fileClassId) {
        return fileClassRepository.findById(fileClassId)
            .orElseThrow(() -> new IllegalArgumentException("file class not found: " + fileClassId));
    }
}
