package com.logicaldelivery.projeto.logicaldelivery.model;

import com.google.firebase.database.DatabaseReference;
import com.logicaldelivery.projeto.logicaldelivery.Config.ConfiguracaoFirebase;

import java.util.HashMap;
import java.util.Map;

public class Requisicao {
    private String id;
    private String status;
    private Usuario entrega;
    private Usuario motorista;
    private Destino destino;

    public static final String STATUS_AGUARDANDO = "Aguardando";
    public static final String STATUS_ACAMINHO = "Motorista a caminho";
    public static final String STATUS_VIAGEM = "Entrega em viagem";
    public static final String STATUS_FINALIZADA = "Entrega finalizada";
    public static final String STATUS_ENCERRADA = "Entrega encerrada";
    public static final String STATUS_CANCELADA = "Entrega cancelada";

    public Requisicao() {
    }

    public void salvar(){
        DatabaseReference firebaseRef = ConfiguracaoFirebase.getFirebaseDatabase();
        DatabaseReference requisicoes = firebaseRef.child("requisicoes");

        String idRequisicao = requisicoes.push().getKey();
        setId(idRequisicao);

        requisicoes.child(getId()).setValue(this);
    }

    public void atualizar(){
        DatabaseReference firebaseRef = ConfiguracaoFirebase.getFirebaseDatabase();
        DatabaseReference requisicoes = firebaseRef.child("requisicoes");

        DatabaseReference requisicao = requisicoes.child(getId());
        Map objeto = new HashMap();
        objeto.put("motorista", getMotorista());
        objeto.put("status", getStatus());

        requisicao.updateChildren(objeto);
    }

    public void atualizarStatus(){
        DatabaseReference firebaseRef = ConfiguracaoFirebase.getFirebaseDatabase();
        DatabaseReference requisicoes = firebaseRef.child("requisicoes");

        DatabaseReference requisicao = requisicoes.child(getId());

        Map objeto = new HashMap();
        objeto.put("status", getStatus());

        requisicao.updateChildren(objeto);
    }

    public void atualizarLocalizacaoEntregador(){
        DatabaseReference firebaseRef = ConfiguracaoFirebase.getFirebaseDatabase();
        DatabaseReference requisicoes = firebaseRef.child("requisicoes");

        DatabaseReference requisicao = requisicoes.child(getId()).child("motorista");
        Map objeto = new HashMap();
        objeto.put("latitude", getMotorista().getLatitude());
        objeto.put("longitude", getMotorista().getLongitude());

        requisicao.updateChildren(objeto);
    }


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Usuario getEntrega() {
        return entrega;
    }

    public void setEntrega(Usuario entrega) {
        this.entrega = entrega;
    }

    public Usuario getMotorista() {
        return motorista;
    }

    public void setMotorista(Usuario motorista) {
        this.motorista = motorista;
    }

    public Destino getDestino() {
        return destino;
    }

    public void setDestino(Destino destino) {
        this.destino = destino;
    }
}
