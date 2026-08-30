package io.github.filipchyla.shopapi.product.category;

import io.github.filipchyla.shopapi.product.category.dto.CategoryTreeResponse;
import io.github.filipchyla.shopapi.product.category.dto.CreateCategoryRequest;
import io.github.filipchyla.shopapi.product.category.dto.SingleCategoryResponse;
import io.github.filipchyla.shopapi.product.category.dto.UpdateCategoryRequest;
import io.github.filipchyla.shopapi.product.category.exception.CircularCategoryReferenceException;
import io.github.filipchyla.shopapi.product.category.exception.CategoryNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private CategoryMapper categoryMapper;

    @InjectMocks
    private CategoryService categoryService;

    private Category buildCategory(String name, Category parent) {
        Category category = new Category();
        category.setId(UUID.randomUUID());
        category.setName(name);
        category.setParent(parent);
        category.setCreatedAt(Instant.now());
        return category;
    }

    @Nested
    class GetCategoryTree {
        @Test
        void getCategoryTree_ShouldReturnEmptyList_WhenNoCategoriesExist() {
            // Given
            when(categoryRepository.findAllOrdered()).thenReturn(List.of());

            // When
            List<CategoryTreeResponse> result = categoryService.getCategoryTree();

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        void getCategoryTree_ShouldReturnAllCategoriesAsRoots_WhenNoneHaveParent() {
            // Given
            Category first = buildCategory("Electronics", null);
            Category second = buildCategory("Books", null);

            when(categoryRepository.findAllOrdered()).thenReturn(List.of(first, second));

            // When
            List<CategoryTreeResponse> result = categoryService.getCategoryTree();

            // Then
            assertThat(result).hasSize(2);
            assertThat(result).extracting(CategoryTreeResponse::name)
                    .containsExactly("Electronics", "Books");
            assertThat(result).allSatisfy(dto -> assertThat(dto.children()).isEmpty());
        }

        @Test
        void getCategoryTree_ShouldNestChildrenUnderTheirParent() {
            //Given
            Category root = buildCategory("Electronics", null);
            Category child = buildCategory("Laptops", root);
            Category grandchild = buildCategory("Gaming Laptops", child);

            when(categoryRepository.findAllOrdered()).thenReturn(List.of(root, child, grandchild));

            //When
            List<CategoryTreeResponse> result = categoryService.getCategoryTree();

            //Then
            assertThat(result).hasSize(1);

            CategoryTreeResponse rootDto = result.getFirst();
            assertThat(rootDto.name()).isEqualTo("Electronics");
            assertThat(rootDto.children()).hasSize(1);

            CategoryTreeResponse childDto = rootDto.children().getFirst();
            assertThat(childDto.name()).isEqualTo("Laptops");
            assertThat(childDto.children()).hasSize(1);

            assertThat(childDto.children().getFirst().name()).isEqualTo("Gaming Laptops");
        }

        @Test
        void getCategoryTree_ShouldHandlesMultipleRootsWithSeparateSubtrees() {
            //Given
            Category root1 = buildCategory("Electronics", null);
            Category root2 = buildCategory("Books", null);
            Category child = buildCategory("Laptops", root1);

            when(categoryRepository.findAllOrdered()).thenReturn(List.of(root1, root2, child));

            //When
            List<CategoryTreeResponse> result = categoryService.getCategoryTree();

            //Then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).children()).hasSize(1);
            assertThat(result.get(1).children()).isEmpty();
        }
    }

    @Nested
    class GetCategoryById {
        UUID id;

        @BeforeEach
        void setUp() {
            id = UUID.randomUUID();
        }

        @Test
        void getCategoryById_ShouldReturnCategory_WhenItExists() {
            //Given
            Category category = buildCategory("Electronics", null);

            when(categoryRepository.findById(id)).thenReturn(Optional.of(category));

            //When
            Category result = categoryService.getCategoryById(id);

            //Then
            assertThat(result).isEqualTo(category);
        }

        @Test
        void getCategoryById_ShouldThrowNotFound_WhenCategoryDoesNotExists() {
            //Given
            when(categoryRepository.findById(id)).thenReturn(Optional.empty());

            //When & Then
            assertThatThrownBy(() -> categoryService.getCategoryById(id))
                    .isInstanceOf(CategoryNotFoundException.class)
                    .hasMessageContaining(id.toString());
        }
    }

    @Nested
    class AddCategory {
        @Test
        void addCategory_ShouldSaveCategoryWithoutParent_WhenParentIdIsNull() {
            // Given
            CreateCategoryRequest request = new CreateCategoryRequest("Electronics", null);
            SingleCategoryResponse response = new SingleCategoryResponse(UUID.randomUUID(), request.name(), Instant.now(), request.parentId());

            when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> inv.getArgument(0));
            when(categoryMapper.toSingleCategoryResponse(any(Category.class))).thenReturn(response);

            // When
            SingleCategoryResponse result = categoryService.addCategory(request);

            //Then
            assertThat(result.name()).isEqualTo(request.name());
            assertThat(result.parentId()).isEqualTo(request.parentId());

            verify(categoryRepository).save(any());
            verify(categoryRepository, never()).findById(any());
        }

        @Test
        void addCategory_ShouldSetParentAndSave_WhenParentIdIsProvided() {
            // Given
            UUID parentId = UUID.randomUUID();
            Category parent = buildCategory("Electronics", null);
            parent.setId(parentId);

            CreateCategoryRequest request = new CreateCategoryRequest("Laptops", parentId);
            SingleCategoryResponse response = new SingleCategoryResponse(UUID.randomUUID(), request.name(), Instant.now(), request.parentId());

            when(categoryRepository.findById(parentId)).thenReturn(Optional.of(parent));
            when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> inv.getArgument(0));
            when(categoryMapper.toSingleCategoryResponse(any(Category.class))).thenReturn(response);

            // When
            SingleCategoryResponse result = categoryService.addCategory(request);

            //Then
            assertThat(result.name()).isEqualTo(request.name());
            assertThat(result.parentId()).isEqualTo(request.parentId());

            verify(categoryRepository).findById(parentId);
            verify(categoryRepository).save(any());
        }

        @Test
        void addCategory_ShouldThrowNotFound_WhenParentDoesNotExist() {
            // Given
            UUID parentId = UUID.randomUUID();
            CreateCategoryRequest request = new CreateCategoryRequest("Laptops", parentId);
            when(categoryRepository.findById(parentId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> categoryService.addCategory(request))
                    .isInstanceOf(CategoryNotFoundException.class);

            verify(categoryRepository, never()).save(any());
        }
    }

    @Nested
    class DeleteCategory {
        @Test
        void deleteCategory_ShouldDelegateToRepository() {
            // Given
            UUID id = UUID.randomUUID();
            when(categoryRepository.existsById(id)).thenReturn(true);

            // When
            categoryService.deleteCategory(id);

            // Then
            verify(categoryRepository).deleteById(id);
        }
    }

    @Nested
    class UpdateCategory {
        private UUID id;

        @BeforeEach
        void setUp() {
            id = UUID.randomUUID();
        }

        @Test
        void updateCategory_ShouldNotOverrideParent_WhenParentIdIsNull() {
            // Given
            Category parent = buildCategory("Books", null);
            Category category = buildCategory("Electronics", parent);

            UpdateCategoryRequest request = new UpdateCategoryRequest("Consumer Electronics", null);
            SingleCategoryResponse response = new SingleCategoryResponse(category.getId(), "Electronics", Instant.now(), parent.getId());

            when(categoryRepository.findById(id)).thenReturn(Optional.of(category));
            when(categoryMapper.toSingleCategoryResponse(category)).thenReturn(response);

            // When
            SingleCategoryResponse result = categoryService.updateCategory(id, request);

            // Then
            assertThat(result).isEqualTo(response);
            assertThat(result.parentId()).isEqualTo(parent.getId());
        }

        @Test
        void updateCategory_ShouldOverrideParent_WhenParentIdIsNotNull() {
            // Given
            Category oldCategory = buildCategory("Electronics", null);
            Category newParent = buildCategory("Books", null);

            UpdateCategoryRequest request = new UpdateCategoryRequest("Consumer Electronics", newParent.getId());
            SingleCategoryResponse response = new SingleCategoryResponse(oldCategory.getId(), "Consumer Electronics", Instant.now(), newParent.getId());

            when(categoryRepository.findById(id)).thenReturn(Optional.of(oldCategory));
            when(categoryRepository.findById(newParent.getId())).thenReturn(Optional.of(newParent));
            when(categoryMapper.toSingleCategoryResponse(oldCategory)).thenReturn(response);

            // When
            SingleCategoryResponse result = categoryService.updateCategory(id, request);

            // Then
            assertThat(result).isEqualTo(response);
            assertThat(result.parentId()).isEqualTo(newParent.getId());
        }

        @Test
        void updateCategory_ShouldThrowNotFound_WhenCategoryDoesNotExist() {
            // Given
            UpdateCategoryRequest request = new UpdateCategoryRequest("Consumer Electronics", null);

            when(categoryRepository.findById(id)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> categoryService.updateCategory(id, request))
                    .isInstanceOf(CategoryNotFoundException.class);

            verifyNoInteractions(categoryMapper);
        }

        @Test
        void updateCategory_ShouldThrowCategoryCycleException_WhenChildCategoryIsSetAsParent() {
            // Given
            Category parentCategory = buildCategory("Electronics", null);
            Category childCategory = buildCategory("Laptops", parentCategory);

            UpdateCategoryRequest request =
                    new UpdateCategoryRequest("Electronics", childCategory.getId());

            when(categoryRepository.findById(id)).thenReturn(Optional.of(parentCategory));
            when(categoryRepository.findById(request.parentId()))
                    .thenReturn(Optional.of(childCategory));

            // When / Then
            assertThatThrownBy(() -> categoryService.updateCategory(id, request))
                    .isInstanceOf(CircularCategoryReferenceException.class);
        }

        @Test
        void updateCategory_ShouldThrowNotFound_WhenParentDoesNotExist() {
            // Given
            UUID parentId = UUID.randomUUID();
            UpdateCategoryRequest request = new UpdateCategoryRequest("Consumer Electronics", parentId);

            Category category = buildCategory("Electronics", null);

            when(categoryRepository.findById(id)).thenReturn(Optional.of(category));
            when(categoryRepository.findById(parentId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> categoryService.updateCategory(id, request))
                    .isInstanceOf(CategoryNotFoundException.class);

            verifyNoInteractions(categoryMapper);
        }
    }
}
