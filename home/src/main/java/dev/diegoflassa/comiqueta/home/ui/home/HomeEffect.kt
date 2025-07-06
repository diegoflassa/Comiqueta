package dev.diegoflassa.comiqueta.home.ui.home

sealed interface HomeEffect {
    data class ShowToast(val message: String) : HomeEffect
    data object LaunchFolderPicker : HomeEffect
    data class RequestStoragePermission(val permission: String) : HomeEffect
}