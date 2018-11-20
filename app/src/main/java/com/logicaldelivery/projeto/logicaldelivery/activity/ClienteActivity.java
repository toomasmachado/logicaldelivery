package com.logicaldelivery.projeto.logicaldelivery.activity;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.logicaldelivery.projeto.logicaldelivery.Config.ConfiguracaoFirebase;
import com.logicaldelivery.projeto.logicaldelivery.R;
import com.logicaldelivery.projeto.logicaldelivery.helper.Local;
import com.logicaldelivery.projeto.logicaldelivery.helper.UsuarioFirebase;
import com.logicaldelivery.projeto.logicaldelivery.model.Destino;
import com.logicaldelivery.projeto.logicaldelivery.model.Requisicao;
import com.logicaldelivery.projeto.logicaldelivery.model.Usuario;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class ClienteActivity extends AppCompatActivity
        implements OnMapReadyCallback {

    private EditText editDestino;
    private LinearLayout linearLayoutDestino;
    private Button buttonChamarEntregador;
    private Requisicao requisicao;
    private Usuario cliente;
    private Usuario motorista;
    private String statusRequisicao;
    private Destino destino;
    private Marker marcadorCliente;
    private Marker marcadorMotorista;
    private Marker marcadorDestino;
    private LatLng localMotorista;

    private GoogleMap mMap;
    private FirebaseAuth autenticacao;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private LatLng localCliente;
    private boolean cancelarEntrega = false;
    private DatabaseReference firebaseRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cliente);

        inicializarComponentes();

        //Adiciona listener para status da requisição
        verificaStatusRequisicao();

    }

    private void verificaStatusRequisicao(){

        Usuario usuarioLogado = UsuarioFirebase.getDadosUsuarioLogado();
        DatabaseReference requisicoes = firebaseRef.child("requisicoes");
        Query requisicaoPesquisa = requisicoes.orderByChild("passageiro/id")
                .equalTo( usuarioLogado.getId() );

        requisicaoPesquisa.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                List<Requisicao> lista = new ArrayList<>();
                for (DataSnapshot ds: dataSnapshot.getChildren()){
                    lista.add(ds.getValue(Requisicao.class));
                }
                Collections.reverse(lista);

                if(lista != null && lista.size() > 0) {
                    requisicao = lista.get(0);

                    if(requisicao!=null) {
                        if (!requisicao.getStatus().equals(Requisicao.STATUS_ENCERRADA)){

                        cliente = requisicao.getEntrega();
                        localCliente = new LatLng(
                                Double.parseDouble(cliente.getLatitude()),
                                Double.parseDouble(cliente.getLongitude())
                        );
                        statusRequisicao = requisicao.getStatus();
                        destino = requisicao.getDestino();
                        if (requisicao.getMotorista() != null) {
                            motorista = requisicao.getMotorista();
                            localMotorista = new LatLng(
                                    Double.parseDouble(motorista.getLatitude()),
                                    Double.parseDouble(motorista.getLongitude())

                            );
                        }
                        alteraInterfaceStatusRequisicao(statusRequisicao);
                    }
                    }

                    }
                }



            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void alteraInterfaceStatusRequisicao(String status){

        if (status != null && !status.isEmpty()) {
            switch (status) {
                case Requisicao.STATUS_AGUARDANDO:
                    cancelarEntrega = true;
                    requisicaoAguardando();
                    break;
                case Requisicao.STATUS_ACAMINHO:
                    cancelarEntrega = false;
                    requisicaoACaminho();
                    break;
                case Requisicao.STATUS_VIAGEM:
                    cancelarEntrega = false;
                    requisicaoViagem();
                    break;
                case Requisicao.STATUS_FINALIZADA:
                    cancelarEntrega = false;
                    requisicaoFinalizada();
                    break;
                case Requisicao.STATUS_CANCELADA:
                    cancelarEntrega = false;
                    requisicaoCancelada();
                    break;
            }
        }else{
            adicionaMarcadorCliente(localCliente, "Seu local");
            centralizarMarcadores(localCliente);

        }

    }

    private void requisicaoAguardando(){
        linearLayoutDestino.setVisibility(View.GONE);
        buttonChamarEntregador.setText("Cancelar Entrega");

        //Adiciona marcador cliente
        adicionaMarcadorCliente(localCliente, cliente.getNome());
        centralizarMarcadores(localCliente);
    }

    private void requisicaoACaminho(){
        linearLayoutDestino.setVisibility(View.GONE);
        buttonChamarEntregador.setText("Motorista a caminho");
        buttonChamarEntregador.setEnabled(false);

        //Adiciona Marcador cliente
        adicionaMarcadorCliente(localCliente, cliente.getNome());

        //Adiciona Marcador Entregador
        adicionaMarcadorMotorista(localMotorista, motorista.getNome());

        //centralizar os usuários
        centralizarDoisMarcadores(marcadorMotorista, marcadorCliente);
    }

    private void requisicaoViagem(){
        linearLayoutDestino.setVisibility(View.GONE);
        buttonChamarEntregador.setText("Entrega a caminho");
        buttonChamarEntregador.setEnabled(false);

        //Adiciona Marcador Entregador
        adicionaMarcadorMotorista(localMotorista, motorista.getNome());

        //Marcador de destino
        LatLng localDestino = new LatLng(
                Double.parseDouble(destino.getLatitude()),
                Double.parseDouble(destino.getLongitude())
        );
        adicionaMarcadorDestino(localDestino,"Destino");

        centralizarDoisMarcadores(marcadorMotorista, marcadorDestino);
    }

    private void requisicaoFinalizada(){
        linearLayoutDestino.setVisibility(View.GONE);
        buttonChamarEntregador.setEnabled(false);

        //Marcador de destino
        LatLng localDestino = new LatLng(
                Double.parseDouble(destino.getLatitude()),
                Double.parseDouble(destino.getLongitude())
        );
        adicionaMarcadorDestino(localDestino,"Destino");
        centralizarMarcadores(localDestino);

        //Calcular distancia
        float distancia = Local.calcularDistancia(localCliente, localDestino);

        float valor = distancia *4;

        DecimalFormat decimalFormat = new DecimalFormat("0.00");
        final String resultado = decimalFormat.format(valor);

        buttonChamarEntregador.setText("Corrida Finalizada - R$ " + resultado);

        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle("Total da viagem")
                .setMessage("Sua viagem ficou " + resultado)
                .setCancelable(false)
                .setNegativeButton("Encerrar Viagem", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        requisicao.setStatus(Requisicao.STATUS_ENCERRADA);
                        requisicao.atualizarStatus();

                        finish();
                        startActivity(new Intent(getIntent()));
                    }
                });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void requisicaoCancelada(){
        linearLayoutDestino.setVisibility(View.VISIBLE);
        buttonChamarEntregador.setText("Chamar Entregador");
    }

    private void adicionaMarcadorCliente(LatLng localizacao, String titulo){
        if( marcadorCliente != null)
            marcadorCliente.remove();

        marcadorCliente = mMap.addMarker(
                new MarkerOptions()
                        .position(localizacao)
                        .title(titulo)
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.usuario))
        );

    }

    private void adicionaMarcadorMotorista(LatLng localizacao, String titulo){
        if( marcadorMotorista != null)
            marcadorMotorista.remove();

        marcadorMotorista = mMap.addMarker(
                new MarkerOptions()
                        .position(localizacao)
                        .title(titulo)
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.carro))
        );
    }

    private void adicionaMarcadorDestino(LatLng localizacao, String titulo){
        if( marcadorCliente != null)
            marcadorCliente.remove();

        if( marcadorDestino != null)
            marcadorDestino.remove();

        marcadorDestino = mMap.addMarker(
                new MarkerOptions()
                        .position(localizacao)
                        .title(titulo)
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.destino))
        );
    }

    private void centralizarDoisMarcadores(Marker marcador1, Marker marcador2){
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        builder.include(marcador1.getPosition());
        builder.include(marcador2.getPosition());

        LatLngBounds bounds = builder.build();

        int largura = getResources().getDisplayMetrics().widthPixels;
        int altura = getResources().getDisplayMetrics().heightPixels;
        int espacoInterno = (int) (largura*0.2);

        mMap.moveCamera(
                CameraUpdateFactory.newLatLngBounds(bounds, largura, altura, espacoInterno)
        );
    }

    private void centralizarMarcadores(LatLng local){
        mMap.moveCamera(
                CameraUpdateFactory.newLatLngZoom(local, 20)
        );
    }
    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        recuperarLocalizaçaoUsuario();

    }

    private void salvarRequisicao(Destino destino){
        Requisicao requisicao = new Requisicao();
        requisicao.setDestino(destino);

        Usuario usuarioCliLogado = UsuarioFirebase.getDadosUsuarioLogado();
        usuarioCliLogado.setLatitude(String.valueOf(localCliente.latitude));
        usuarioCliLogado.setLongitude(String.valueOf(localCliente.longitude));

        requisicao.setEntrega(usuarioCliLogado);

        requisicao.setStatus(Requisicao.STATUS_AGUARDANDO);
        requisicao.salvar();

        linearLayoutDestino.setVisibility(View.GONE);
        buttonChamarEntregador.setText("Cancelar Entrega");
        cancelarEntrega = true;
    }

    public void chamarEntregador(View view){

        Log.d("p->", "Valor Cancelar entrega:" + cancelarEntrega);

        if ( cancelarEntrega ){
            requisicao.setStatus(Requisicao.STATUS_CANCELADA);
            requisicao.atualizarStatus();
            cancelarEntrega = false;

            Log.d("p->", "Valor Cancelar entrega3:" + cancelarEntrega);

        }else{
            String endereçoDestino = editDestino.getText().toString();

            if (!endereçoDestino.equals("") || endereçoDestino != null ){
                Address AddressDestino = recuperarEndereco (endereçoDestino);
                if(AddressDestino!=null){
                    final Destino destino = new Destino();
                    destino.setCidade(AddressDestino.getAdminArea());
                    destino.setCep(AddressDestino.getPostalCode());
                    destino.setBairro(AddressDestino.getSubLocality());
                    destino.setRua(AddressDestino.getThoroughfare());
                    destino.setNumero(AddressDestino.getFeatureName());
                    destino.setLatitude(String.valueOf(AddressDestino.getLatitude()));
                    destino.setLongitude(String.valueOf(AddressDestino.getLongitude()));

                    StringBuilder msg = new StringBuilder();
                    msg.append("Cidade:" + destino.getCidade());
                    msg.append("\nRua:" + destino.getRua());
                    msg.append("\nBairro:" + destino.getBairro());
                    msg.append("\nNúmero:" + destino.getNumero());
                    msg.append("\nCep:" + destino.getCep());

                    AlertDialog.Builder builder = new AlertDialog.Builder(this)
                            .setTitle("Confirme seu endereço!")
                            .setMessage(msg)
                            .setPositiveButton("Confirmar", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    salvarRequisicao(destino);
                                    Log.d("p->", "Valor Cancelar entrega2:" + cancelarEntrega);
                                }
                            }).setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {

                                }
                            });
                    AlertDialog dialog = builder.create();
                    dialog.show();

                }

            }else{
                Toast.makeText(this, "Informe o endereço de destino!", Toast.LENGTH_SHORT).show();
            }
        }
    }


    private Address recuperarEndereco(String endereco){
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> listaEnderecos = geocoder.getFromLocationName(endereco, 3);
            if (listaEnderecos != null && listaEnderecos.size()>0){
                Address address = listaEnderecos.get(0);

                double lat = address.getLatitude();
                double lon = address.getLongitude();

                return address;

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void recuperarLocalizaçaoUsuario() {
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                //Recuperar Latitude e Longitude

                double latitude = location.getLatitude();
                double longitude = location.getLongitude();
                localCliente = new LatLng(latitude, longitude);

                //atualizar Geofire
                UsuarioFirebase.atualizarDadosLocalizacao(latitude, longitude);

                //Altera interface de acordo com o status
                alteraInterfaceStatusRequisicao(statusRequisicao);

                if (statusRequisicao != null && !statusRequisicao.isEmpty()) {
                    if (statusRequisicao.equals(Requisicao.STATUS_VIAGEM)
                            || statusRequisicao.equals(Requisicao.STATUS_FINALIZADA)) {
                        locationManager.removeUpdates(locationListener);
                    }else{
                        if (ActivityCompat.checkSelfPermission(ClienteActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                            locationManager.requestLocationUpdates(
                                    LocationManager.GPS_PROVIDER,
                                    10000,
                                    10,
                                    locationListener
                            );
                        }

                    }
                }
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onProviderDisabled(String provider) {

            }
        };

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    10000,
                    10,
                    locationListener
            );
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()){
            case R.id.menuSair:
                autenticacao.signOut();
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }
    private void inicializarComponentes(){
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle("Iniciar Uma Entrega");
        setSupportActionBar(toolbar);

        //Inicializar componentes
        editDestino = findViewById(R.id.editDestino);
        linearLayoutDestino = findViewById(R.id.linearLayoutDestino);
        buttonChamarEntregador = findViewById(R.id.buttonChamarEntregador);

        //Configurações iniciais
        autenticacao = ConfiguracaoFirebase.getFirebaseAutenticacao();
        firebaseRef = ConfiguracaoFirebase.getFirebaseDatabase();


        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }
}
