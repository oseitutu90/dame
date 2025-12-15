package com.dame.ui.auth;

import com.dame.dto.RegisterRequest;
import com.dame.service.PlayerService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

@Route("register")
@PageTitle("Register | Checkers")
@AnonymousAllowed
public class RegisterView extends VerticalLayout {

        private final PlayerService playerService;

        private final TextField usernameField = new TextField("Username");
        private final PasswordField passwordField = new PasswordField("Password");
        private final PasswordField confirmPasswordField = new PasswordField("Confirm Password");
        private final Button registerButton = new Button("Register");

        private final Binder<RegisterRequest> binder = new BeanValidationBinder<>(RegisterRequest.class);

        public RegisterView(PlayerService playerService) {
                this.playerService = playerService;

                addClassName("register-view");
                setSizeFull();
                setAlignItems(Alignment.CENTER);
                setJustifyContentMode(JustifyContentMode.CENTER);

                configureFields();
                configureBinder();

                H1 title = new H1("Checkers");
                title.addClassName("register-title");

                Paragraph subtitle = new Paragraph("Create your account");
                subtitle.addClassName("register-subtitle");

                VerticalLayout formLayout = new VerticalLayout();
                formLayout.addClassName("register-form");
                formLayout.setWidth("300px");
                formLayout.setAlignItems(Alignment.STRETCH);

                registerButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
                registerButton.setWidthFull();
                registerButton.addClickListener(e -> register());

                formLayout.add(usernameField, passwordField, confirmPasswordField, registerButton);

                Anchor loginLink = new Anchor("login", "Already have an account? Login here");
                loginLink.addClassName("login-link");

                add(title, subtitle, formLayout, loginLink);
        }

        private void configureFields() {
                usernameField.setPlaceholder("3-20 characters");
                usernameField.setPattern("^[a-zA-Z0-9_]+$");
                usernameField.setMinLength(3);
                usernameField.setMaxLength(20);
                usernameField.setRequired(true);
                usernameField.setHelperText("Letters, numbers, and underscores only");

                passwordField.setMinLength(6);
                passwordField.setRequired(true);
                passwordField.setHelperText("At least 6 characters");

                confirmPasswordField.setRequired(true);
        }

        private void configureBinder() {
                binder.forField(usernameField)
                                .asRequired("Username is required")
                                .withValidator(
                                                username -> username.length() >= 3 && username.length() <= 20,
                                                "Username must be between 3 and 20 characters")
                                .withValidator(
                                                username -> username.matches("^[a-zA-Z0-9_]+$"),
                                                "Username can only contain letters, numbers, and underscores")
                                .withValidator(
                                                username -> !playerService.usernameExists(username),
                                                "Username already taken")
                                .bind(RegisterRequest::getUsername, RegisterRequest::setUsername);

                binder.forField(passwordField)
                                .asRequired("Password is required")
                                .withValidator(
                                                password -> password.length() >= 6,
                                                "Password must be at least 6 characters")
                                .bind(RegisterRequest::getPassword, RegisterRequest::setPassword);

                binder.forField(confirmPasswordField)
                                .asRequired("Please confirm your password")
                                .withValidator(
                                                confirm -> confirm.equals(passwordField.getValue()),
                                                "Passwords do not match")
                                .bind(RegisterRequest::getConfirmPassword, RegisterRequest::setConfirmPassword);
        }

        private void register() {
                RegisterRequest request = new RegisterRequest();

                if (binder.writeBeanIfValid(request)) {
                        try {
                                playerService.register(request);

                                Notification.show("Registration successful! Please login.",
                                                3000, Notification.Position.TOP_CENTER)
                                                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);

                                getUI().ifPresent(ui -> ui.navigate("login"));
                        } catch (IllegalArgumentException e) {
                                Notification.show(e.getMessage(),
                                                3000, Notification.Position.TOP_CENTER)
                                                .addThemeVariants(NotificationVariant.LUMO_ERROR);
                        }
                } else {
                        Notification.show("Please fix the errors in the form",
                                        3000, Notification.Position.TOP_CENTER)
                                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                }
        }
}
