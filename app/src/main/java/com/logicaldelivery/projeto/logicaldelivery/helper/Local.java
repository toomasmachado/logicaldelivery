package com.logicaldelivery.projeto.logicaldelivery.helper;

import android.location.Location;

import com.google.android.gms.maps.model.LatLng;

import java.text.DecimalFormat;

public class Local {

    public static float calcularDistancia(LatLng latLngInicial, LatLng latLngFinal){

        Location localInicial = new Location("Local inicial");
        localInicial.setLatitude(latLngInicial.latitude);
        localInicial.setLongitude(latLngInicial.longitude);

        Location localFinal = new Location("Local final");
        localFinal.setLatitude(latLngFinal.latitude);
        localFinal.setLongitude(latLngFinal.longitude);

        float distancia = localInicial.distanceTo(localFinal)/1000;

        return distancia;
    }

    public static String formatarDistancia(float distancia){
        String distanciaForm;
        if (distancia < 1 ){
            distancia = distancia * 1000; // em metros
            distanciaForm = Math.round(distancia) + " M ";
        }else{
            DecimalFormat decimal = new DecimalFormat("0.0");
            distanciaForm = decimal.format(distancia) + " KM ";
        }
        return distanciaForm;
    }
}
