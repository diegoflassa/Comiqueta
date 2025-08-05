package dev.diegoflassa.comiqueta.core.domain.usecase.folder

import android.net.Uri
import kotlinx.coroutines.flow.Flow


/**
 * Interface para o caso de uso de obter a lista de URIs de pastas de quadrinhos monitoradas.
 * Define o contrato público do caso de uso.
 */
interface IGetMonitoredFoldersUseCase {
    /**
     * Retorna um Flow que emite a lista de URIs de pastas monitoradas.
     *
     * @return Um Flow de List de Uri.
     */
    operator fun invoke(): Flow<List<Uri>>
}