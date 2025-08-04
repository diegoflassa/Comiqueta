@file:Suppress("UnusedFlow")

package dev.diegoflassa.comiqueta.domain.usecase

import dev.diegoflassa.comiqueta.core.data.database.entity.CategoryEntity
import dev.diegoflassa.comiqueta.core.domain.repository.ICategoryRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import com.google.common.truth.Truth.assertThat // Using Google Truth

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class LoadCategoriesUseCaseTest {

    @Mock
    private lateinit var mockCategoryRepository: ICategoryRepository

    private lateinit var loadCategoriesUseCase: ILoadCategoriesUseCase

    @Before
    fun setUp() {
        loadCategoriesUseCase = LoadCategoriesUseCase(mockCategoryRepository)
    }

    @Test
    fun `invoke should call repository and return its flow of categories`() = runTest {
        // Arrange
        val expectedCategories = listOf(
            CategoryEntity(id = 1, name = "DC Comics"),
            CategoryEntity(id = 2, name = "Marvel")
        )
        val expectedFlow = flowOf(expectedCategories)

        whenever(mockCategoryRepository.getAllCategories()).thenReturn(expectedFlow)

        // Act
        val resultFlow = loadCategoriesUseCase()

        // Assert
        assertThat(resultFlow.first()).isEqualTo(expectedCategories)
        verify(mockCategoryRepository).getAllCategories()
    }

    @Test
    fun `invoke when repository returns empty list should return empty list flow`() = runTest {
        // Arrange
        val expectedCategories = emptyList<CategoryEntity>()
        val expectedFlow = flowOf(expectedCategories)

        whenever(mockCategoryRepository.getAllCategories()).thenReturn(expectedFlow)

        // Act
        val resultFlow = loadCategoriesUseCase()

        // Assert
        assertThat(resultFlow.first()).isEqualTo(expectedCategories)
        verify(mockCategoryRepository).getAllCategories()
    }
}
