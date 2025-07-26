package dev.diegoflassa.comiqueta.core.domain.usecase.category

import dev.diegoflassa.comiqueta.core.data.database.entity.CategoryEntity
import dev.diegoflassa.comiqueta.core.domain.repository.ICategoryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetCategoriesUseCase @Inject constructor(
    private val ICategoryRepository: ICategoryRepository
) {
    operator fun invoke(): Flow<List<CategoryEntity>> {
        return ICategoryRepository.getEditableCategories()
    }
}
