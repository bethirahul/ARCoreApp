package com.rahulbethi.arcoreapp

import android.Manifest
import com.google.ar.sceneform.ux.ArFragment

class CustomArFragment : ArFragment() {
    override fun getAdditionalPermissions(): Array<String> {
        val additionalPermissions = super.getAdditionalPermissions()
        val permissionLength = additionalPermissions.size
        val permissions = Array(permissionLength + 1) { Manifest.permission.WRITE_EXTERNAL_STORAGE }
        if(permissionLength > 0) {
            System.arraycopy(additionalPermissions, 0, permissions, 1, permissionLength)
        }
        return permissions
    }
}