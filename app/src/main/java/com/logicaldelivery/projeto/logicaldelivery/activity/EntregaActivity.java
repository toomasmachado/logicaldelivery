package com.logicaldelivery.projeto.logicaldelivery.activity;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.logicaldelivery.projeto.logicaldelivery.Config.ConfiguracaoFirebase;
import com.logicaldelivery.projeto.logicaldelivery.R;
import com.logicaldelivery.projeto.logicaldelivery.helper.UsuarioFirebase;
import com.logicaldelivery.projeto.logicaldelivery.model.Requisicao;
import com.logicaldelivery.projeto.logicaldelivery.model.Usuario;

public class EntregaActivity extends AppCompatActivity
        implements OnMapReadyCallback {

    private GoogleMap mMap;

    private LocationManager locationManager;
    private LocationListener locationListener;
    private LatLng localEntregador;
    private LatLng localCliente;
    private Usuario entregador;
    private Usuario cliente;
    private String idRequisicao;
    private Requisicao requisicao;
    private DatabaseReference firebaseRef;
    private Button buttonAceitarEntrega;
    private Marker marcadorMotorista;
    private Marker marcadorCliente;
    private String statusRequisicao;
    private boolean requisicaoAtiva;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_entrega);

        inicializarComponentes();

        //Recupera dados do usuario
        if(getIntent().getExtras().containsKey("idRequisicao")
            && getIntent().getExtras().containsKey("entregador")) {
            Bundle extras = getIntent().getExtras();
            entregador = (Usuario) extras.getSerializable("entregador");
            localEntregador = new LatLng(
                    Double.parseDouble(entregador.getLatitude()),
                    Double.parseDouble(entregador.getLongitude())
            );
            idRequisicao = extras.getString("idRequisicao");
            requisicaoAtiva = extras.getBoolean("requisicaoAtiva");

            verificaStatusRequisicao();

        }
    }

    private void verificaStatusRequisicao(){
        DatabaseReference requisicoes = firebaseRef.child("requisicoes")
                .child(idRequisicao);
        requisicoes.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                //Recupera requisicão
                requisicao = dataSnapshot.getValue(Requisicao.class);
                if(requisicao!=null){
                    cliente = requisicao.getEntrega();
                    localCliente = new LatLng(
                            Double.parseDouble(cliente.getLatitude()),
                            Double.parseDouble(cliente.getLongitude())
                    );
                    statusRequisicao = requisicao.getStatus();
                    alteraInterfaceStatusRequisicao(statusRequisicao);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void alteraInterfaceStatusRequisicao(String status){
        switch (status){
            case Requisicao.STATUS_AGUARDANDO:
                requisicaoAguardando();
                break;
            case Requisicao.STATUS_ACAMINHO:
                requisicaoACaminho();
                break;
        }
    }

    private void requisicaoAguardando(){

        buttonAceitarEntrega.setText("Aceitar Entrega");
        //Exibe Marcador do motorista/entregador
        adicionaMarcadorMotorista(localEntregador, entregador.getNome());

        mMap.moveCamera(
                CameraUpdateFactory.newLatLngZoom(localEntregador, 20)
        );
    }

    private void requisicaoACaminho(){
        buttonAceitarEntrega.setText("A caminho do cliente");

        //Exibe Marcador do motorista/entregador
        adicionaMarcadorMotorista(localEntregador, entregador.getNome());

        //Exibe local do cliente
        adicionaMarcadorCliente(localCliente, cliente.getNome());

        //Centralizar dois marcadores
        centralizarDoisMarcadores(marcadorMotorista, marcadorCliente);

        //Inicia monitoramento do motorista
        iniciarMonitoramentoCorrida(cliente, entregador);
    }

    private void iniciarMonitoramentoCorrida(final Usuario cliente, final Usuario entregador){
        //Inicializando GeoFire
        DatabaseReference localUsuario = ConfiguracaoFirebase.getFirebaseDatabase()
                .child("local_usuario");
        GeoFire geoFire = new GeoFire(localUsuario);

        //Adiciona Circulo no Passageiro
        final Circle circle = mMap.addCircle(
                new CircleOptions()
                .center(localCliente)
                .radius(50)
                .fillColor(Color.argb(90,255, 153, 0))
                .strokeColor(Color.argb(190,255,153,0))
        );
        final GeoQuery geoQuery = geoFire.queryAtLocation(
                new GeoLocation(localCliente.latitude, localCliente.longitude),
                0.05
        );

        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
             if(key.equals(entregador.getId())){
                 requisicao.setStatus(Requisicao.STATUS_VIAGEM);
                 requisicao.atualizarStatus();

                 geoQuery.removeAllListeners();
                 circle.remove();
                }
            }

            @Override
            public void onKeyExited(String key) {

            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {

            }

            @Override
            public void onGeoQueryReady() {

            }

            @Override
            public void onGeoQueryError(DatabaseError error) {

            }
        });
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

    private void recuperarLocalizaçaoUsuario() {
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                //Recuperar Latitude e Longitude

                double latitude = location.getLatitude();
                double longitude = location.getLongitude();
                localEntregador = new LatLng(latitude, longitude);

                //atualizar Geofire
                UsuarioFirebase.atualizarDadosLocalizacao(latitude, longitude);

                alteraInterfaceStatusRequisicao(statusRequisicao);
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
                    0,
                    0,
                    locationListener
            );
        }

    }

    public void aceitarEntrega(View view){

        requisicao = new Requisicao();
        requisicao.setId(idRequisicao);
        requisicao.setMotorista(entregador);
        requisicao.setStatus(Requisicao.STATUS_ACAMINHO);

        requisicao.atualizar();

    }

    private void inicializarComponentes(){
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Iniciar Entrega");

        buttonAceitarEntrega = findViewById(R.id.buttonAceitarEntrega);

        //Configurações iniciais
        firebaseRef = ConfiguracaoFirebase.getFirebaseDatabase();

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    public boolean onSupportNavigateUp() {
        if(requisicaoAtiva){
            Toast.makeText(EntregaActivity.this, "Necessário encerrar requisição atual!",Toast.LENGTH_SHORT).show();
        }else{
            Intent i = new Intent(EntregaActivity.this, RequisicoesActivity.class);
            startActivity(i);
        }
        return false;
    }
}
