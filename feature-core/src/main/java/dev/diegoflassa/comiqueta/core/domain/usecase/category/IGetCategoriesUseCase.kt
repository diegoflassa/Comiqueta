package dev.diegoflassa.comiqueta.core.domain.usecase.category

import dev.diegoflassa.comiqueta.core.data.database.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

/**
 * Interface para o caso de uso de recuperar a lista de entidades de categoria.
 * Define o contrato público do caso de uso.
 */
interface IGetCategoriesUseCase {
    /**
     * Retorna um Flow que emite a lista de entidades de categoria.
     *
     * @return Um Flow de List de CategoryEntity.
     */
    operator fun invoke(): Flow<List<CategoryEntity>>
}