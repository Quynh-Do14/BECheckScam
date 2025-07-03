package com.example.checkscamv2.service.Impl;

import com.example.checkscamv2.entity.Category;
import com.example.checkscamv2.repository.CategoryRepository;
import com.example.checkscamv2.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class CategoryServiceImpl implements CategoryService {
    private final CategoryRepository categoryRepository;

    @Override
    public Category createCategory(Category category) {
        return categoryRepository.save(category);
    }
}
