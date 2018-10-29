package com.logicaldelivery.projeto.logicaldelivery.activity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthActionCodeException;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.logicaldelivery.projeto.logicaldelivery.Config.ConfiguracaoFirebase;
import com.logicaldelivery.projeto.logicaldelivery.R;
import com.logicaldelivery.projeto.logicaldelivery.helper.UsuarioFirebase;
import com.logicaldelivery.projeto.logicaldelivery.model.Usuario;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputEditText;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Switch;
import android.widget.Toast;

public class CadastroActivity extends AppCompatActivity {

    private TextInputEditText campoNome, campoEmail, campoSenha;
    private Switch switchTipoUsuario;

    private FirebaseAuth autenticacao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cadastro);

        //inicializar componentes
        campoNome = findViewById(R.id.editCadastroNome);
        campoEmail = findViewById(R.id.editCadastroEmail);
        campoSenha = findViewById(R.id.editCadastroSenha);
        switchTipoUsuario = findViewById(R.id.switchTipoUsuario);

    }

    public void validarCadastroUsuario(View view){
        //Recuperar campos de texto

        String textoNome = campoNome.getText().toString();
        String textoEmail = campoEmail.getText().toString();
        String textoSenha = campoSenha.getText().toString();

            if(!textoNome.isEmpty()){
                if(!textoEmail.isEmpty()){
                    if(!textoSenha.isEmpty()){

                        Usuario usuario = new Usuario();
                        usuario.setNome( textoNome );
                        usuario.setEmail( textoEmail );
                        usuario.setSenha( textoSenha );
                        usuario.setTipo( validaTipoUsuario() );

                        cadastrarUsuario(usuario);

                    }else{
                        Toast.makeText(CadastroActivity.this, "Preencha a senha!", Toast.LENGTH_SHORT).show();
                    }

                }else{
                    Toast.makeText(CadastroActivity.this, "Preencha o Email!", Toast.LENGTH_SHORT).show();
                }
            }else{
                Toast.makeText(CadastroActivity.this, "Preencha o nome!", Toast.LENGTH_SHORT).show();
            }
    }

    public void cadastrarUsuario(final Usuario usuario){
        autenticacao = ConfiguracaoFirebase.getFirebaseAutenticacao();
        autenticacao.createUserWithEmailAndPassword(
                usuario.getEmail(), usuario.getSenha()
        ).addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()){
                    try{
                        String idUsuario = task.getResult().getUser().getUid();
                        usuario.setId(idUsuario);
                        usuario.salvar();

                        //Atualizar nome no userProfile
                        UsuarioFirebase.atualizarNomeUsuario(usuario.getNome());

                        //Redireciona usuario com base no seu tipo
                        if(validaTipoUsuario()== "C"){
                            startActivity(new Intent(CadastroActivity.this, MapsActivity.class));
                            finish();

                            Toast.makeText(CadastroActivity.this, "Sucesso ao cadastrar Cliente!", Toast.LENGTH_SHORT).show();

                        }else{
                            startActivity(new Intent(CadastroActivity.this, RequisicoesActivity.class));
                            finish();

                            Toast.makeText(CadastroActivity.this, "Sucesso ao cadastrar Entregador!", Toast.LENGTH_SHORT).show();
                        }

                    }catch (Exception e ){
                        e.printStackTrace();
                    }
                }else{

                    String excessao = "";
                    try{
                        throw task.getException();
                    }catch (FirebaseAuthWeakPasswordException e){
                        excessao = "Digite uma senha mais forte!";
                    }catch (FirebaseAuthInvalidCredentialsException e){
                        excessao = "Por favor, digite um email correto!";
                    }catch (FirebaseAuthUserCollisionException e){
                        excessao = "Esta conta ja foi cadastrada";
                    }catch (Exception e){
                        excessao = "Erro ao cadastrar usuário: " + e.getMessage();
                        e.printStackTrace();
                    }
                    Toast.makeText(CadastroActivity.this, excessao, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    public String validaTipoUsuario(){
        return switchTipoUsuario.isChecked() ? "E": "C";
    }
}
