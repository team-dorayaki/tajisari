package com.tajisali.property.service;

import com.tajisali.property.client.AiAnalysisClient;
import com.tajisali.property.dto.PropertyAnalysisResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PropertyAnalysisService {

    private final AiAnalysisClient aiAnalysisClient;

    public PropertyAnalysisResponse analyzeImages(List<MultipartFile> files) {
        return aiAnalysisClient.analyzeImages(files);
    }

    public PropertyAnalysisResponse analyzeUrl(String url) {
        return aiAnalysisClient.analyzeUrl(url);
    }
}
