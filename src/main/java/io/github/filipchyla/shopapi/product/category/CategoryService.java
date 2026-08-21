package io.github.filipchyla.shopapi.product.category;

import io.github.filipchyla.shopapi.product.category.dto.CreateCategoryRequest;
import io.github.filipchyla.shopapi.product.category.dto.CategoryTreeResponse;
import io.github.filipchyla.shopapi.product.category.dto.SingleCategoryResponse;
import io.github.filipchyla.shopapi.product.category.dto.UpdateCategoryRequest;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class CategoryService {
    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    private static final String TREE_CACHE_KEY = "'tree'";

    @Transactional
    @Cacheable(value = "categories", key = TREE_CACHE_KEY)
    public List<CategoryTreeResponse> getCategoryTree() {
        List<Category> allCategories = categoryRepository.findAllOrdered();

        Map<UUID, CategoryTreeResponse> dtoById = new LinkedHashMap<>();
        for (Category category : allCategories) {
            dtoById.put(category.getId(), CategoryTreeResponse.leaf(category));
        }

        List<CategoryTreeResponse> roots = new ArrayList<>();
        for (Category category : allCategories) {
            CategoryTreeResponse dto = dtoById.get(category.getId());
            Category parent = category.getParent();

            if (parent == null) {
                roots.add(dto);
            } else {
                CategoryTreeResponse parentDto = dtoById.get(parent.getId());
                parentDto.children().add(dto);
            }
        }

        return roots;
    }

    @Transactional
    @CacheEvict(value = "categories", key = TREE_CACHE_KEY)
    public SingleCategoryResponse addCategory(CreateCategoryRequest categoryData) {
        Category category = new Category();
        category.setName(categoryData.name());

        if (categoryData.parentId() != null) {
            category.setParent(getCategoryById(categoryData.parentId()));
        }

        return categoryMapper.toSingleCategoryResponse(categoryRepository.save(category));
    }

    @Transactional
    @CacheEvict(value = "categories", key = TREE_CACHE_KEY)
    public void deleteCategory(UUID id) {
        if (!categoryRepository.existsById(id)) {
            throw new CategoryNotFoundException("Category not found with id: " + id);
        }
        categoryRepository.deleteById(id);
    }

    @Transactional
    @CacheEvict(value = "categories", key = TREE_CACHE_KEY)
    public SingleCategoryResponse updateCategory(UUID id, UpdateCategoryRequest request) {
        Category category = getCategoryById(id);
        if (request.parentId() != null) {
            category.setParent(getCategoryById(request.parentId()));
        }
        categoryMapper.updateCategory(request, category);
        return categoryMapper.toSingleCategoryResponse(category);
    }

    public Category getCategoryById(UUID id) {
        return categoryRepository.findById(id).orElseThrow(() -> new CategoryNotFoundException("Category not found with id: " + id));
    }
}