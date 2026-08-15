package io.github.filipchyla.shopapi.product.category;

import io.github.filipchyla.shopapi.product.category.dto.CreateCategoryRequest;
import io.github.filipchyla.shopapi.product.category.dto.CategoryResponse;
import io.github.filipchyla.shopapi.product.category.dto.UpdateCategoryRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class CategoryService {
    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    @Transactional
    public List<CategoryResponse> getCategoryTree() {
        List<Category> allCategories = categoryRepository.findAllOrdered();

        Map<UUID, CategoryResponse> dtoById = new LinkedHashMap<>();
        for (Category category : allCategories) {
            dtoById.put(category.getId(), CategoryResponse.leaf(category));
        }

        List<CategoryResponse> roots = new ArrayList<>();
        for (Category category : allCategories) {
            CategoryResponse dto = dtoById.get(category.getId());
            Category parent = category.getParent();

            if (parent == null) {
                roots.add(dto);
            } else {
                CategoryResponse parentDto = dtoById.get(parent.getId());
                parentDto.children().add(dto);
            }
        }

        return roots;
    }

    public Category getCategoryById(UUID id) {
        return categoryRepository.findById(id).orElseThrow(() -> new CategoryNotFoundException("Category not found with id: " + id));
    }
    public Category addCategory(CreateCategoryRequest categoryData) {
        Category category = new Category();
        category.setName(categoryData.categoryName());

        if (categoryData.parentId() != null) {
            category.setParent(getCategoryById(categoryData.parentId()));
        }

        return categoryRepository.save(category);
    }

    public void deleteCategory(UUID id) {
        categoryRepository.deleteById(id);
    }

    @Transactional
    public Category updateCategory(UUID id, UpdateCategoryRequest request) {
        Category category = getCategoryById(id);
        categoryMapper.updateCategory(request, category);
        return category;
    }
}
