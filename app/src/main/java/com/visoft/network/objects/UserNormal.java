package com.visoft.network.objects;

/**
 * Clase UserNormal, usada para almacenar la informacion de un usuario
 */
public class UserNormal extends User {

    public UserNormal() {
        isPro = false;
    }

    @Override
    public String getUid() {
        return uid;
    }
}
