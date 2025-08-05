package dev.diegoflassa.comiqueta.core.domain.usecase

import java.util.UUID

/**
 * Interface para o caso de uso de enfileirar uma requisição de trabalho para o SafFolderScanWorker.
 * Define o contrato público do caso de uso.
 */
interface IEnqueueSafFolderScanWorkerUseCase {
    /**
     * Enfileira uma requisição de trabalho única para o SafFolderScanWorker.
     */
    operator fun invoke(): UUID
}