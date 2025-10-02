package vn.edu.usth.camer;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {
    private EditText etEmail, etPassword;
    private Button btnLogin;
    private TextView tvRegister;
    private DatabaseHelper db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        db = new DatabaseHelper(this);

        // Check if already logged in
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        int userId = prefs.getInt("user_id", -1);
        if (userId != -1) {
            goToMain();
            return;
        }

        initViews();
        setupListeners();
    }

    private void initViews() {
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvRegister = findViewById(R.id.tvRegister);
    }

    private void setupListeners() {
        btnLogin.setOnClickListener(v -> loginUser());
        tvRegister.setOnClickListener(v -> showRegisterDialog());
    }

    private void loginUser() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        // Validation
        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Email is required");
            etEmail.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Please enter a valid email");
            etEmail.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Password is required");
            etPassword.requestFocus();
            return;
        }

        // Check credentials
        int userId = db.loginUser(email, password);

        if (userId != -1) {
            // Save session
            SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
            prefs.edit()
                    .putInt("user_id", userId)
                    .putString("email", email)
                    .putBoolean("is_logged_in", true)
                    .apply();

            Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show();
            goToMain();
        } else {
            Toast.makeText(this, "Invalid email or password", Toast.LENGTH_SHORT).show();
        }
    }

    private void showRegisterDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_register, null);
        EditText etUsername = dialogView.findViewById(R.id.etUsername);
        EditText etRegEmail = dialogView.findViewById(R.id.etRegEmail);
        EditText etRegPassword = dialogView.findViewById(R.id.etRegPassword);
        EditText etConfirmPassword = dialogView.findViewById(R.id.etConfirmPassword);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Create Account")
                .setView(dialogView)
                .setPositiveButton("Register", null)
                .setNegativeButton("Cancel", null)
                .create();

        dialog.setOnShowListener(dialogInterface -> {
            Button button = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            button.setOnClickListener(view -> {
                String username = etUsername.getText().toString().trim();
                String email = etRegEmail.getText().toString().trim();
                String password = etRegPassword.getText().toString().trim();
                String confirmPass = etConfirmPassword.getText().toString().trim();

                // Validation
                if (TextUtils.isEmpty(username)) {
                    etUsername.setError("Username is required");
                    return;
                }

                if (TextUtils.isEmpty(email)) {
                    etRegEmail.setError("Email is required");
                    return;
                }

                if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    etRegEmail.setError("Please enter a valid email");
                    return;
                }

                if (TextUtils.isEmpty(password)) {
                    etRegPassword.setError("Password is required");
                    return;
                }

                if (password.length() < 6) {
                    etRegPassword.setError("Password must be at least 6 characters");
                    return;
                }

                if (!password.equals(confirmPass)) {
                    etConfirmPassword.setError("Passwords do not match");
                    return;
                }

                // Check if email exists
                if (db.checkEmailExists(email)) {
                    etRegEmail.setError("Email already registered");
                    return;
                }

                // Register user
                if (db.registerUser(username, email, password)) {
                    Toast.makeText(this, "Registration successful! Please login", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                    etEmail.setText(email);
                } else {
                    Toast.makeText(this, "Registration failed. Please try again", Toast.LENGTH_SHORT).show();
                }
            });
        });

        dialog.show();
    }

    private void goToMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}