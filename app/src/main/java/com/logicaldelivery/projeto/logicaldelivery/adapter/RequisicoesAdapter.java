package com.logicaldelivery.projeto.logicaldelivery.adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.gms.maps.model.LatLng;
import com.logicaldelivery.projeto.logicaldelivery.R;

import com.logicaldelivery.projeto.logicaldelivery.helper.Local;
import com.logicaldelivery.projeto.logicaldelivery.model.Requisicao;
import com.logicaldelivery.projeto.logicaldelivery.model.Usuario;

import java.util.List;

public class RequisicoesAdapter extends RecyclerView.Adapter<RequisicoesAdapter.MyViewHolder> {

    private List<Requisicao> requisicoes;
    private Context context;
    private Usuario Entregador;

    public RequisicoesAdapter(List<Requisicao> requisicoes, Context context, Usuario entregador) {
        this.requisicoes = requisicoes;
        this.context = context;
        Entregador = entregador;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int i) {
        View item = LayoutInflater.from(parent.getContext()).inflate(R.layout.adapter_requisicoes, parent, false);
        return new MyViewHolder(item);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        Requisicao requisicao = requisicoes.get(position);
        Usuario cliente = requisicao.getEntrega();

        Entregador.getLatitude();
        Entregador.getLongitude();

        holder.nome.setText(cliente.getNome());
        if(Entregador != null ){

            LatLng localCliente = new LatLng(
                    Double.parseDouble(cliente.getLatitude()),
                    Double.parseDouble(cliente.getLongitude())
            );
            LatLng localEntregador = new LatLng(
                    Double.parseDouble(Entregador.getLatitude()),
                    Double.parseDouble(Entregador.getLongitude())
            );
            float distancia = Local.calcularDistancia(localCliente, localEntregador);
            String distanciaForm = Local.formatarDistancia(distancia);
            holder.distancia.setText(distanciaForm + "- aproximadamente");
        }
    }

    @Override
    public int getItemCount() {
        return requisicoes.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder{
        TextView nome, distancia;

        public MyViewHolder(View itemView){
            super(itemView);

            nome = itemView.findViewById(R.id.textRequisicaoNome);
            distancia = itemView.findViewById(R.id.textRequisicaoDistancia);
        }
    }

}
