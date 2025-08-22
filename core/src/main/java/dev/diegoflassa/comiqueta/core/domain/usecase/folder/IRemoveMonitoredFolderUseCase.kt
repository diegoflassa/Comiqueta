package dev.diegoflassa.comiqueta.core.domain.usecase.folder

import android.net.Uri

/**
 * Interface para o caso de uso de remover uma URI de pasta de quadrinhos monitorada.
 * Define o contrato público do caso de uso.
 */
interface IRemoveMonitoredFolderUseCase {
    /**
     * Remove uma URI de pasta monitorada do repositório.
     *
     * @param uri A URI da pasta a ser removida.
     * @return true se a remoção foi bem-sucedida, false caso contrário.
     */
    operator fun invoke(uri: Uri): Boolean
}
