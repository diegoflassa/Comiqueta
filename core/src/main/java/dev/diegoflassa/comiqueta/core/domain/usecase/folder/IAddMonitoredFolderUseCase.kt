package dev.diegoflassa.comiqueta.core.domain.usecase.folder

import android.net.Uri

/**
 * Interface para o caso de uso de adicionar uma nova URI de pasta de quadrinhos monitorada.
 * Define o contrato público do caso de uso.
 */
interface IAddMonitoredFolderUseCase {
    /**
     * Adiciona uma URI de pasta monitorada ao repositório.
     * Assume que a permissão de URI persistível já foi obtida pela camada de UI.
     *
     * @param uri A URI da pasta a ser adicionada.
     * @return true se a adição foi bem-sucedida, false caso contrário.
     */
    operator fun invoke(uri: Uri): Boolean
}